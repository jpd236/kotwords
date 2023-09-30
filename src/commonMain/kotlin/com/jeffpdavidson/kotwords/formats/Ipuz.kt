package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.json.JsonSerializer
import com.jeffpdavidson.kotwords.model.Crossword
import com.jeffpdavidson.kotwords.model.Puzzle
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okio.ByteString.Companion.decodeBase64

private val DATA_URL_PATTERN = "data:(.*);base64,(.*)".toRegex()

class Ipuz(private val json: String) : Puzzleable() {
    override suspend fun createPuzzle(): Puzzle {
        val ipuz = JsonSerializer.fromJson<IpuzJson>(json)

        val ipuzKind = ipuz.kind.mapNotNull { kind ->
            val uri = kind.substringBeforeLast('#')
            if (IpuzKind.DIAGRAMLESS.isKind(uri)) {
                IpuzKind.DIAGRAMLESS
            } else if (IpuzKind.CROSSWORD.isKind(uri)) {
                IpuzKind.CROSSWORD
            } else if (IpuzKind.CODED.isKind(uri)) {
                IpuzKind.CODED
            } else {
                null
            }
        }.lastOrNull()
        require(ipuzKind != null) { "Puzzle kinds not recognized: ${ipuz.kind}" }

        val grid = (0 until ipuz.dimensions.height).map { y ->
            (0 until ipuz.dimensions.width).map { x ->
                val solution =
                    (if (ipuz.solution.isNotEmpty()) ipuz.getValueIfValid(ipuz.solution[y][x].value) else "")
                        .ifEmpty { ipuz.puzzle[y][x].value }
                val style = when (val style = ipuz.puzzle[y][x].style) {
                    is IpuzJson.StyleRef -> {
                        ipuz.styles.getOrElse(style.style) { IpuzJson.StyleSpec() }
                    }
                    is IpuzJson.StyleSpec -> style
                }
                val cellType = when {
                    ipuz.puzzle[y][x].cell == null -> Puzzle.CellType.VOID
                    ipuz.puzzle[y][x].cell == ipuz.block -> Puzzle.CellType.BLOCK
                    ipuz.puzzle[y][x].value.isNotEmpty() -> Puzzle.CellType.CLUE
                    else -> Puzzle.CellType.REGULAR
                }
                val backgroundShape = when (style.shapebg) {
                    "circle" -> Puzzle.BackgroundShape.CIRCLE
                    else -> Puzzle.BackgroundShape.NONE
                }
                val borderDirections = style.barred.mapNotNull {
                    when (it) {
                        'T' -> Puzzle.BorderDirection.TOP
                        'R' -> Puzzle.BorderDirection.RIGHT
                        'B' -> Puzzle.BorderDirection.BOTTOM
                        'L' -> Puzzle.BorderDirection.LEFT
                        else -> null
                    }
                }.toSet()
                val backgroundColor = when {
                    style.color.isNotEmpty() -> "#${style.color}"
                    style.highlight -> "#c0c0c0"
                    else -> ""
                }
                Puzzle.Cell(
                    solution = solution,
                    entry = if (ipuz.saved.isEmpty()) "" else ipuz.getValueIfValid(ipuz.saved[y][x].value),
                    foregroundColor = if (style.colorText.isEmpty()) "" else "#${style.colorText}",
                    backgroundColor = backgroundColor,
                    backgroundImage = urlToPuzzleImage(style.imagebg),
                    number = style.mark.tl.ifEmpty { ipuz.getValueIfValid(ipuz.puzzle[y][x].cell) },
                    topRightNumber = style.mark.tr,
                    cellType = cellType,
                    backgroundShape = backgroundShape,
                    borderDirections = borderDirections,
                )
            }
        }

        val clueLists = mutableListOf<Puzzle.ClueList>()
        val words = mutableListOf<Puzzle.Word>()
        if (ipuz.clues.values.any { clueList -> clueList.any { it.cells.isNotEmpty() } }) {
            // Custom words - use exactly as given.
            ipuz.clues.entries.forEachIndexed { clueListIndex, (title, clues) ->
                val puzzleClues = mutableListOf<Puzzle.Clue>()
                clues.forEachIndexed { clueIndex, clue ->
                    val wordId = 1000 * clueListIndex + clueIndex + 1
                    val cells = clue.cells.map { cellList ->
                        Puzzle.Coordinate(x = cellList[0] - 1, y = cellList[1] - 1)
                    }
                    words.add(
                        Puzzle.Word(
                            id = wordId,
                            cells = cells,
                        )
                    )
                    puzzleClues.add(
                        Puzzle.Clue(
                            wordId = wordId,
                            number = clue.number,
                            text = clue.clue,
                            format = if (ipuz.showEnumerations) clue.enumeration else ""
                        )
                    )
                }
                clueLists.add(Puzzle.ClueList(title = title, clues = puzzleClues))
            }
        } else {
            // No words provided - use standard crossword conventions.
            val acrossClues = ipuz.clues.getOrElse("Across") { listOf() }
            val acrossClueMap = acrossClues.associate { it.number.toInt() to it.clue }
            val downClues = ipuz.clues.getOrElse("Down") { listOf() }
            val downClueMap = downClues.associate { it.number.toInt() to it.clue }
            val crossword = Crossword(
                title = ipuz.title,
                creator = ipuz.author,
                copyright = ipuz.copyright,
                grid = grid,
                acrossClues = acrossClueMap,
                downClues = downClueMap,
            ).asPuzzle()
            // Skip empty lists, or lists consisting entirely of empty clues.
            val crosswordClues =
                crossword.clues.filterNot { it.clues.isEmpty() || it.clues.all { clue -> clue.text.isEmpty() } }
            // Copy the enumerations into the generated clue lists, if they're meant to be shown.
            if (ipuz.showEnumerations) {
                crosswordClues.forEach { clueList ->
                    clueLists.add(clueList.copy(clues = clueList.clues.mapIndexed { i, clue ->
                        val originalClues = if (clueList.title.contains("Across")) acrossClues else downClues
                        clue.copy(format = originalClues[i].enumeration)
                    }))
                }
            } else {
                clueLists.addAll(crosswordClues)
            }
            words.addAll(crossword.words)
        }

        return Puzzle(
            title = ipuz.title,
            creator = ipuz.author,
            copyright = ipuz.copyright,
            description = ipuz.notes,
            completionMessage = ipuz.explanation,
            grid = grid,
            clues = clueLists,
            words = words,
            diagramless = ipuzKind == IpuzKind.DIAGRAMLESS,
            puzzleType = if (ipuzKind == IpuzKind.CODED) Puzzle.PuzzleType.CODED else Puzzle.PuzzleType.CROSSWORD,
            hasHtmlClues = true,
        )
    }

    override suspend fun asIpuzFile(solved: Boolean): ByteArray {
        // If we don't need to fill in the solution, we can return the given data directly.
        if (!solved) {
            return json.encodeToByteArray()
        }
        return super.asIpuzFile(solved)
    }

    // TODO: Pass in an optional ImageGetter interface to support fetching of HTTP image URLs; cache by URL.
    private fun urlToPuzzleImage(url: String): Puzzle.Image {
        val matchResult = DATA_URL_PATTERN.matchEntire(url) ?: return Puzzle.Image.None
        val format = when (matchResult.groupValues[1]) {
            "image/jpeg" -> Puzzle.ImageFormat.JPG
            "image/png" -> Puzzle.ImageFormat.PNG
            "image/gif" -> Puzzle.ImageFormat.GIF
            else -> return Puzzle.Image.None
        }
        return Puzzle.Image.Data(
            format = format,
            bytes = matchResult.groupValues[2].decodeBase64() ?: return Puzzle.Image.None
        )
    }

    companion object {
        internal fun asIpuzJson(puzzle: Puzzle, solved: Boolean = false): IpuzJson {
            // TODO: Add Acrostic support
            require(puzzle.puzzleType in listOf(Puzzle.PuzzleType.CROSSWORD, Puzzle.PuzzleType.CODED)) {
                "Unsupported puzzle type"
            }
            val kinds =
                if (puzzle.diagramless) {
                    listOf("${IpuzKind.DIAGRAMLESS.uri}#1")
                } else if (puzzle.puzzleType == Puzzle.PuzzleType.CODED) {
                    listOf("${IpuzKind.CROSSWORD.uri}#1", "${IpuzKind.CODED.uri}#1")
                } else {
                    listOf("${IpuzKind.CROSSWORD.uri}#1")
                }
            // To find suitable block/empty characters, pick the first character starting from the default choice which
            // doesn't appear in the solution.
            val solutionChars =
                puzzle.grid.flatten().filterNot { it.cellType.isBlack() }.mapNotNull { it.solution.firstOrNull() }
            val block = generateSequence('#') { it + 1 }.first { !solutionChars.contains(it) }
            val empty = generateSequence('0') { it + 1 }.first { !solutionChars.contains(it) }
            val wordsByWordId = puzzle.words.associate { it.id to it.cells }
            return IpuzJson(
                kind = kinds,
                title = puzzle.title,
                copyright = puzzle.copyright,
                author = puzzle.creator,
                notes = puzzle.description,
                explanation = puzzle.completionMessage,
                block = "$block",
                empty = "$empty",
                dimensions = IpuzJson.Dimensions(
                    height = puzzle.grid.size,
                    width = puzzle.grid[0].size,
                ),
                puzzle = puzzle.grid.map { row ->
                    row.map { cell ->
                        val cellValue = when (cell.cellType) {
                            Puzzle.CellType.VOID -> null
                            Puzzle.CellType.BLOCK -> "$block"
                            else -> cell.number.ifEmpty { "$empty" }
                        }
                        val backgroundImage = if (cell.backgroundImage is Puzzle.Image.Data) {
                            val mimeType = when (cell.backgroundImage.format) {
                                Puzzle.ImageFormat.GIF -> "image/gif"
                                Puzzle.ImageFormat.PNG -> "image/png"
                                Puzzle.ImageFormat.JPG -> "image/jpeg"
                            }
                            "data:$mimeType;base64,${cell.backgroundImage.bytes.base64()}"
                        } else {
                            ""
                        }
                        val barred = cell.borderDirections.joinToString {
                            when (it) {
                                Puzzle.BorderDirection.TOP -> "T"
                                Puzzle.BorderDirection.LEFT -> "L"
                                Puzzle.BorderDirection.BOTTOM -> "B"
                                Puzzle.BorderDirection.RIGHT -> "R"
                            }
                        }
                        IpuzJson.LabeledCell(
                            cell = cellValue,
                            style = IpuzJson.StyleSpec(
                                shapebg = if (cell.backgroundShape == Puzzle.BackgroundShape.CIRCLE) "circle" else "",
                                mark = IpuzJson.StyleSpec.Mark(tr = cell.topRightNumber),
                                imagebg = backgroundImage,
                                barred = barred,
                                color = cell.backgroundColor.substringAfter('#'),
                                colorText = cell.foregroundColor.substringAfter('#'),
                            ),
                            value = if (cell.cellType == Puzzle.CellType.CLUE) cell.solution else "",
                        )
                    }
                },
                saved =
                if (puzzle.grid.flatten().any { it.entry.isNotEmpty() } || solved) {
                    puzzle.grid.map { row ->
                        row.map { cell ->
                            IpuzJson.CrosswordValue(value = if (cell.cellType.isBlack()) {
                                null
                            } else if (solved) {
                                cell.solution
                            } else {
                                cell.entry.ifEmpty { null }
                            })
                        }
                    }
                } else {
                    listOf()
                },
                solution = if (puzzle.hasSolution()) {
                    puzzle.grid.map { row ->
                        row.map { cell ->
                            IpuzJson.CrosswordValue(value = if (cell.cellType.isBlack()) null else cell.solution)
                        }
                    }
                } else {
                    listOf()
                },
                clues =
                puzzle.clues.associate { clueList ->
                    // Strip any HTML tags from the title.
                    val title = Xml.parse(clueList.title, format = DocumentFormat.HTML).text ?: clueList.title
                    title to clueList.clues.map { clue ->
                        val cells =
                            wordsByWordId.getOrElse(clue.wordId) { listOf() }.map { listOf(it.x + 1, it.y + 1) }
                        IpuzJson.Clue(
                            number = clue.number,
                            cells = cells,
                            clue = clue.text,
                            enumeration = clue.format,
                        )
                    }
                },
                showEnumerations = puzzle.clues.any { clueList -> clueList.clues.any { it.format.isNotEmpty() } },
                // Set fakeClues = true if there are any unclued words.
                fakeClues = puzzle.hasUncluedWords()
            )
        }
    }
}

private enum class IpuzKind(val uri: String) {
    CROSSWORD("http://ipuz.org/crossword"),
    DIAGRAMLESS("http://ipuz.org/crossword/diagramless"),

    // Proprietary extension unless/until this is officially supported.
    CODED("http://crosswordnexus.com/ipuz/coded");

    fun isKind(puzzleUri: String): Boolean {
        return uri == puzzleUri || puzzleUri.startsWith("$uri/")
    }
}

@Serializable
internal data class IpuzJson @OptIn(ExperimentalSerializationApi::class) constructor(
    @EncodeDefault val version: String = "http://ipuz.org/v2",
    val kind: List<String>,
    val title: String = "",
    val copyright: String = "",
    val author: String = "",
    val notes: String = "",
    val explanation: String = "",
    @Serializable(with = NumericStringSerializer::class) val block: String = "#",
    @Serializable(with = NumericStringSerializer::class) val empty: String = "0",
    val styles: Map<String, StyleSpec> = mapOf(),
    val dimensions: Dimensions = Dimensions(),
    @Serializable(with = LabeledCellGridSerializer::class)
    val puzzle: List<List<LabeledCell>> = listOf(),
    @Serializable(with = CrosswordValueGridSerializer::class)
    val saved: List<List<CrosswordValue>> = listOf(),
    @Serializable(with = CrosswordValueGridSerializer::class)
    val solution: List<List<CrosswordValue>> = listOf(),
    @Serializable(with = ClueMapSerializer::class)
    @EncodeDefault val clues: Map<String, List<Clue>> = mapOf(),
    @SerialName("showenumerations") val showEnumerations: Boolean = false,
    // Proprietary extension introduced by squares.io for when some clue lists are unassociated with answers.
    @SerialName("fakeclues") val fakeClues: Boolean = false,
) {
    @Serializable
    sealed class Style

    @Serializable
    data class StyleSpec(
        val shapebg: String = "",
        val highlight: Boolean = false,
        val mark: Mark = Mark(),
        val imagebg: String = "",
        val barred: String = "",
        /** Background color in hex format (without #). Numerical colors are normalized to a hex color. */
        @Serializable(with = ColorSerializer::class)
        val color: String = "",
        /** Foreground color in hex format (without #). Numerical colors are normalized to a hex color. */
        @Serializable(with = ColorSerializer::class)
        @SerialName("colortext") val colorText: String = "",
    ) : Style() {
        @Serializable
        data class Mark(
            @SerialName("TL") val tl: String = "",
            @SerialName("TR") val tr: String = "",
        )

        object ColorSerializer : JsonTransformingSerializer<String>(String.serializer()) {
            override fun transformDeserialize(element: JsonElement): JsonElement {
                val colorInt = element.jsonPrimitive.intOrNull ?: return element
                // Integer indicating an arbitrary app-defined color, with 0 as black and others as non-black.
                // For simplicity, just support 0 and treat all other colors as gray/shaded.
                return JsonPrimitive(
                    if (colorInt == 0) {
                        "000000"
                    } else {
                        "c0c0c0"
                    }
                )
            }
        }
    }

    @Serializable
    data class StyleRef(val style: String) : Style()

    object StyleSerializer : JsonContentPolymorphicSerializer<Style>(Style::class) {
        override fun selectDeserializer(element: JsonElement) = when (element) {
            is JsonPrimitive -> StyleRef.serializer()
            else -> StyleSpec.serializer()
        }
    }

    @Serializable
    data class Dimensions(val width: Int = 0, val height: Int = 0)

    @Serializable
    data class LabeledCell(
        val cell: String? = null,
        @Serializable(with = StyleSerializer::class)
        val style: Style = StyleSpec(),
        val value: String = "",
    ) {
        /** Serializer for LabeledCells which converts to and from simplified representations. */
        object LabeledCellSerializer : JsonTransformingSerializer<LabeledCell>(serializer()) {
            override fun transformDeserialize(element: JsonElement): JsonElement {
                return when (element) {
                    is JsonNull -> buildJsonObject { }
                    is JsonPrimitive -> buildJsonObject { put("cell", JsonPrimitive(element.jsonPrimitive.content)) }
                    else -> JsonObject(element.jsonObject.mapValues { (key, value) ->
                        if (key == "cell") {
                            JsonPrimitive(value.jsonPrimitive.content)
                        } else {
                            value
                        }
                    })
                }
            }

            override fun transformSerialize(element: JsonElement): JsonElement {
                require(element is JsonObject)
                return when {
                    element.isEmpty() -> JsonNull
                    element.keys.all { it == "cell" } -> element["cell"]!!
                    else -> element
                }
            }
        }
    }

    object LabeledCellGridSerializer : JsonTransformingSerializer<List<List<LabeledCell>>>(
        ListSerializer(ListSerializer(LabeledCell.LabeledCellSerializer))
    )

    @Serializable
    data class CrosswordValue(
        val value: String? = null,
    ) {
        /** Serializer for LabeledCells which converts to and from simplified representations. */
        object CrosswordValueSerializer : JsonTransformingSerializer<CrosswordValue>(serializer()) {
            override fun transformDeserialize(element: JsonElement): JsonElement {
                return when (element) {
                    is JsonNull -> buildJsonObject { }
                    is JsonPrimitive -> buildJsonObject { put("value", element) }
                    else -> element
                }
            }

            override fun transformSerialize(element: JsonElement): JsonElement {
                require(element is JsonObject)
                return when {
                    element.isEmpty() -> JsonNull
                    element.keys.all { it == "value" } -> element["value"]!!
                    else -> element
                }
            }
        }
    }

    object CrosswordValueGridSerializer : JsonTransformingSerializer<List<List<CrosswordValue>>>(
        ListSerializer(ListSerializer(CrosswordValue.CrosswordValueSerializer))
    )

    @Serializable
    data class Clue(
        @Serializable(with = NumericStringSerializer::class) val number: String = "",
        val cells: List<List<Int>> = listOf(),
        val clue: String = "",
        val enumeration: String = "",
    ) {
        object ClueSerializer : JsonTransformingSerializer<Clue>(serializer()) {
            override fun transformDeserialize(element: JsonElement): JsonElement {
                return when (element) {
                    is JsonPrimitive -> buildJsonObject { put("clue", element) }
                    is JsonArray -> buildJsonObject {
                        put("number", element[0].jsonPrimitive)
                        put("clue", element[1].jsonPrimitive)
                    }
                    is JsonObject -> element
                }
            }

            override fun transformSerialize(element: JsonElement): JsonElement {
                require(element is JsonObject)
                return when {
                    element.isEmpty() -> JsonPrimitive("")
                    element.keys == setOf("clue") -> element["clue"]!!
                    element.keys == setOf("number", "clue") -> JsonArray(listOf(element["number"]!!, element["clue"]!!))
                    else -> element
                }
            }
        }
    }

    object ClueMapSerializer : JsonTransformingSerializer<Map<String, List<Clue>>>(
        MapSerializer(String.serializer(), ListSerializer(Clue.ClueSerializer))
    )

    object NumericStringSerializer : JsonTransformingSerializer<String>(String.serializer()) {
        override fun transformDeserialize(element: JsonElement): JsonElement {
            // Normalize numeric values to strings.
            val intValue = element.jsonPrimitive.intOrNull ?: return element
            return JsonPrimitive(intValue.toString())
        }
    }

    fun toJsonString(prettyPrint: Boolean = false): String {
        val json = if (prettyPrint) {
            Json {
                this.prettyPrint = true
            }
        } else {
            Json
        }
        return json.encodeToString(serializer(), this)
    }

    /**
     * Returns the value of the provided string only if it is "valid".
     *
     * Values for omitted, empty, or block cells are all converted to the empty string.
     */
    fun getValueIfValid(value: String?): String = when (value) {
        null, block, empty -> ""
        else -> value
    }
}