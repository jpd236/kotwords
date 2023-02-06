package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.model.Puzzle
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.modules.SerializersModule
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.XmlException
import nl.adaptivity.xmlutil.core.XmlVersion
import nl.adaptivity.xmlutil.serialization.UnknownChildHandler
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue
import okio.ByteString.Companion.decodeBase64

private const val CCA_NS = "http://crossword.info/xml/crossword-compiler-applet"
private const val PUZZLE_NS = "http://crossword.info/xml/rectangular-puzzle"

// TODO: Is it possible to restrict the types of the elements of this list at compile time?
// See https://github.com/pdvrieze/xmlutil/issues/30
private typealias Snippet = List<@Polymorphic Any>

private fun getXmlSerializer(prettyPrint: Boolean = false): XML {
    @OptIn(ExperimentalXmlUtilApi::class)
    return XML(Jpz.module()) {
        xmlDeclMode = XmlDeclMode.Charset
        autoPolymorphic = true
        // Ignore unknown elements
        unknownChildHandler = UnknownChildHandler { _, _, _, _, _ -> listOf() }
        indentString = if (prettyPrint) "    " else ""
        xmlVersion = XmlVersion.XML10
    }
}

/**
 * Container for a JPZ file.
 *
 * @param data the file data. Can either be XML or a ZIP file containing one XML file.
 * @param stripFormats if true, the format will be cleared from all clues. Useful when the format is applied
 *                     inconsistently or unnecessarily (e.g. is just the total answer length for each clue).
 */
class JpzFile(private val data: ByteArray, private val stripFormats: Boolean = false) : DelegatingPuzzleable() {
    override suspend fun getPuzzleable(): Jpz {
        val xml = try {
            Zip.unzip(data)
        } catch (e: InvalidZipException) {
            // Assume the file is already unzipped.
            data
        }
        return Jpz.fromXmlString(xml = xml.decodeToString(), stripFormats = stripFormats)
    }

    override suspend fun asJpzFile(solved: Boolean): ByteArray {
        // If we don't need to fill in the solution, we can return the given data directly.
        if (!solved) {
            return data
        }
        return super.asJpzFile(solved)
    }
}

/** Container for JPZ XML data. */
sealed class Jpz : Puzzleable() {
    abstract val rectangularPuzzle: RectangularPuzzle

    abstract fun toXmlString(prettyPrint: Boolean = false): String

    @Serializable
    data class Html(@XmlValue(true) val data: Snippet)

    @Serializable
    @XmlSerialName("rectangular-puzzle", PUZZLE_NS, "")
    data class RectangularPuzzle(
        val metadata: Metadata = Metadata(),
        val alphabet: String? = null,
        @XmlSerialName("crossword", PUZZLE_NS, "") val crossword: Crossword? = null,
        @XmlSerialName("acrostic", PUZZLE_NS, "") val acrostic: Crossword? = null,
        @XmlSerialName("coded", PUZZLE_NS, "") val coded: Crossword? = null,
    ) {

        @Serializable
        @SerialName("metadata")
        data class Metadata(
            @XmlSerialName("title", PUZZLE_NS, "") val title: Html? = null,
            @XmlSerialName("creator", PUZZLE_NS, "") val creator: Html? = null,
            @XmlSerialName("copyright", PUZZLE_NS, "") val copyright: Html? = null,
            @XmlSerialName("description", PUZZLE_NS, "") val description: Html? = null,
        )

        @Serializable
        data class Crossword(val grid: Grid, val words: List<Word>, val clues: List<Clues>) {

            @Serializable
            @SerialName("grid")
            data class Grid(
                @SerialName("width") val width: Int,
                @SerialName("height") val height: Int,
                val gridLook: GridLook = GridLook(),
                val cell: List<Cell>
            ) {

                @Serializable
                @SerialName("grid-look")
                data class GridLook(@SerialName("numbering-scheme") val numberingScheme: String = "normal")

                @Serializable
                @SerialName("background-picture")
                data class BackgroundPicture(
                    @SerialName("encoded-image") @XmlElement(true) val encodedImage: String,
                    @SerialName("format") val format: String,
                )

                @Serializable
                @SerialName("cell")
                data class Cell(
                    @SerialName("x") val x: Int,
                    @SerialName("y") val y: Int,
                    @SerialName("solution") val solution: String? = null,
                    @SerialName("foreground-color") val foregroundColor: String? = null,
                    @SerialName("background-color") val backgroundColor: String? = null,
                    val backgroundPicture: BackgroundPicture? = null,
                    @SerialName("number") val number: String? = null,
                    @SerialName("type") val type: String? = null,
                    @SerialName("solve-state") val solveState: String? = null,
                    @SerialName("top-right-number") val topRightNumber: String? = null,
                    @SerialName("background-shape") val backgroundShape: String? = null,
                    @SerialName("top-bar") val topBar: Boolean? = null,
                    @SerialName("left-bar") val leftBar: Boolean? = null,
                    @SerialName("right-bar") val rightBar: Boolean? = null,
                    @SerialName("bottom-bar") val bottomBar: Boolean? = null,
                    @SerialName("hint") val hint: Boolean? = null,
                )
            }

            @Serializable
            @SerialName("word")
            data class Word(
                @SerialName("id") val id: Int,
                val cells: List<Cells>,
                val x: String? = null,
                val y: String? = null,
            ) {

                @Serializable
                @SerialName("cells")
                data class Cells(val x: String, val y: String)
            }

            @Serializable
            @SerialName("clues")
            data class Clues(
                @XmlSerialName("title", PUZZLE_NS, "") val title: Html,
                val clues: List<Clue>,
            ) {
                @Serializable
                @SerialName("clue")
                data class Clue(
                    @SerialName("word") val word: Int,
                    @SerialName("number") val number: String,
                    @XmlValue(true) val text: Snippet,
                    @SerialName("format") val format: String? = null,
                )
            }
        }
    }

    @Serializable
    @SerialName("b")
    data class B(@XmlValue(true) val data: Snippet)

    @Serializable
    @SerialName("i")
    data class I(@XmlValue(true) val data: Snippet)

    @Serializable
    @SerialName("sub")
    data class Sub(@XmlValue(true) val data: Snippet)

    @Serializable
    @SerialName("sup")
    data class Sup(@XmlValue(true) val data: Snippet)

    @Serializable
    @SerialName("span")
    data class Span(@XmlValue(true) val data: Snippet)

    suspend fun toCompressedFile(filename: String = "puzzle.jpz", prettyPrint: Boolean = false): ByteArray {
        return Zip.zip(filename, toXmlString(prettyPrint).encodeToByteArray())
    }

    override suspend fun createPuzzle(): Puzzle {
        val crossword: RectangularPuzzle.Crossword
        val puzzleType: Puzzle.PuzzleType
        if (rectangularPuzzle.acrostic != null) {
            crossword = rectangularPuzzle.acrostic!!
            puzzleType = Puzzle.PuzzleType.ACROSTIC
        } else if (rectangularPuzzle.coded != null) {
            crossword = rectangularPuzzle.coded!!
            puzzleType = Puzzle.PuzzleType.CODED
        } else {
            crossword = rectangularPuzzle.crossword!!
            puzzleType = Puzzle.PuzzleType.CROSSWORD
        }
        val width = crossword.grid.width
        val height = crossword.grid.height
        val gridMap: MutableMap<Pair<Int, Int>, Puzzle.Cell> = mutableMapOf()
        crossword.grid.cell.forEach {
            val position = Pair(it.x - 1, it.y - 1)
            val cellType = when (it.type) {
                "block" -> Puzzle.CellType.BLOCK
                "clue" -> Puzzle.CellType.CLUE
                "void" -> Puzzle.CellType.VOID
                else -> Puzzle.CellType.REGULAR
            }
            val backgroundShape =
                if (it.backgroundShape == "circle") {
                    Puzzle.BackgroundShape.CIRCLE
                } else {
                    Puzzle.BackgroundShape.NONE
                }
            val backgroundImage =
                if (it.backgroundPicture != null) {
                    val format = when (it.backgroundPicture.format) {
                        "GIF" -> Puzzle.ImageFormat.GIF
                        "JPG" -> Puzzle.ImageFormat.JPG
                        "PNG" -> Puzzle.ImageFormat.PNG
                        else -> throw UnsupportedOperationException("Unknown image type ${it.backgroundPicture.format}")
                    }
                    Puzzle.Image.Data(format, it.backgroundPicture.encodedImage.decodeBase64()!!)
                } else {
                    Puzzle.Image.None
                }
            gridMap[position] = Puzzle.Cell(
                solution = it.solution ?: "",
                foregroundColor = it.foregroundColor ?: "",
                backgroundColor = it.backgroundColor ?: "",
                number = it.number ?: "",
                topRightNumber = it.topRightNumber ?: "",
                cellType = cellType,
                backgroundShape = backgroundShape,
                borderDirections = setOfNotNull(
                    if (it.topBar == true) Puzzle.BorderDirection.TOP else null,
                    if (it.bottomBar == true) Puzzle.BorderDirection.BOTTOM else null,
                    if (it.leftBar == true) Puzzle.BorderDirection.LEFT else null,
                    if (it.rightBar == true) Puzzle.BorderDirection.RIGHT else null,
                ),
                hint = it.hint ?: false,
                backgroundImage = backgroundImage,
            )
        }
        val grid: MutableList<MutableList<Puzzle.Cell>> = mutableListOf()
        for (y in 0 until height) {
            val row = mutableListOf<Puzzle.Cell>()
            for (x in 0 until width) {
                row.add(gridMap[Pair(x, y)]!!)
            }
            grid.add(row)
        }
        var completionMessage = ""
        var title = rectangularPuzzle.metadata.title
        var copyright = rectangularPuzzle.metadata.copyright
        if (this is CrosswordCompilerApplet) {
            completionMessage = appletSettings.completion.message
            if (appletSettings.title != null) {
                title = appletSettings.title
            }
            if (appletSettings.copyright != null) {
                copyright = appletSettings.copyright
            }
        }
        return Puzzle(
            title = title?.data?.toHtml() ?: "",
            creator = rectangularPuzzle.metadata.creator?.data?.toHtml() ?: "",
            copyright = copyright?.data?.toHtml() ?: "",
            description = rectangularPuzzle.metadata.description?.data?.toHtml() ?: "",
            grid = grid,
            clues = crossword.clues.map { clues ->
                Puzzle.ClueList(title = clues.title.data.toHtml(), clues = clues.clues.map { clue ->
                    Puzzle.Clue(
                        wordId = clue.word, number = clue.number, text = clue.text.toHtml(), format = clue.format ?: ""
                    )
                })
            },
            words = crossword.words.map { word ->
                Puzzle.Word(id = word.id, cells = word.getCoordinates())
            },
            hasHtmlClues = true,
            completionMessage = completionMessage,
            puzzleType = puzzleType,
        )
    }

    private fun RectangularPuzzle.Crossword.Word.getCoordinates(): List<Puzzle.Coordinate> {
        val innerCells = cells.flatMap { cell -> getCoordinates(cell.x, cell.y) }
        if (x == null || x.isEmpty() || y == null || y.isEmpty()) {
            return innerCells
        }
        return getCoordinates(x, y) + innerCells
    }

    private fun getCoordinates(x: String, y: String): List<Puzzle.Coordinate> {
        val xParts = x.split("-")
        val yParts = y.split("-")
        val xRange = xParts.first().toInt()..xParts.last().toInt()
        val yRange = yParts.first().toInt()..yParts.last().toInt()
        return xRange.flatMap { i -> yRange.map { j -> Puzzle.Coordinate(x = i - 1, y = j - 1) } }
    }

    private fun Snippet.toHtml(trim: Boolean = true): String {
        val result = joinToString("") {
            when (it) {
                is String -> it.replace("&", "&amp;").replace("<", "&lt;")
                is B -> "<b>${it.data.toHtml(trim = false)}</b>"
                is I -> "<i>${it.data.toHtml(trim = false)}</i>"
                is Sub -> "<sub>${it.data.toHtml(trim = false)}</sub>"
                is Sup -> "<sup>${it.data.toHtml(trim = false)}</sup>"
                is Span -> "<span>${it.data.toHtml(trim = false)}</span>"
                else -> throw IllegalStateException("Unknown data type: $it")
            }
        }
        return if (trim) result.trim() else result
    }

    companion object {
        /**
         * Parse the given JPZ XML.
         *
         * @param stripFormats if true, the format will be cleared from all clues. Useful when the format is applied
         *                     inconsistently or unnecessarily (e.g. is just the total answer length for each clue).
         */
        fun fromXmlString(xml: String, stripFormats: Boolean = false): Jpz {
            // Clean up invalid xmlns as saved by Xword.
            val cleanedXml = if (
                xml.contains("<crossword-compiler-applet") &&
                xml.contains("xmlns=\"http://crossword.info/xml/crossword-compiler\"")
            ) {
                xml.replace(
                    "xmlns=\"http://crossword.info/xml/crossword-compiler\"",
                    "xmlns=\"http://crossword.info/xml/crossword-compiler-applet\""
                )
            } else {
                xml
            }

            // Try to parse as a <crossword-compiler-applet>; if it fails, fall back to <crossword-compiler>.
            val jpz = try {
                getXmlSerializer().decodeFromString(CrosswordCompilerApplet.serializer(), cleanedXml)
            } catch (e: XmlException) {
                getXmlSerializer().decodeFromString(CrosswordCompiler.serializer(), cleanedXml)
            }
            if (!stripFormats || jpz.rectangularPuzzle.crossword == null) {
                return jpz
            }
            val rectangularPuzzle = jpz.rectangularPuzzle.copy(
                crossword = jpz.rectangularPuzzle.crossword!!.copy(
                    clues = jpz.rectangularPuzzle.crossword!!.clues.map { clues ->
                        clues.copy(clues = clues.clues.map { clue ->
                            clue.copy(format = null)
                        })
                    }
                )
            )
            return when (jpz) {
                is CrosswordCompilerApplet -> jpz.copy(rectangularPuzzle = rectangularPuzzle)
                is CrosswordCompiler -> jpz.copy(rectangularPuzzle = rectangularPuzzle)
            }
        }

        /** Returns this puzzle as a JPZ file. */
        internal fun asJpz(
            puzzle: Puzzle,
            solved: Boolean,
            appletSettings: CrosswordCompilerApplet.AppletSettings?,
        ): Jpz = with(puzzle) {
            val jpzGrid = RectangularPuzzle.Crossword.Grid(
                width = grid[0].size,
                height = grid.size,
                cell = grid.mapIndexed { y, row ->
                    row.mapIndexed { x, cell ->
                        val type = when (cell.cellType) {
                            Puzzle.CellType.BLOCK -> "block"
                            Puzzle.CellType.CLUE -> "clue"
                            Puzzle.CellType.VOID -> "void"
                            else -> null
                        }
                        val backgroundShape =
                            if (cell.backgroundShape == Puzzle.BackgroundShape.CIRCLE) "circle" else null
                        // Crossword Solver only renders top and left borders, so if we have a right border, apply it
                        // as a left border on the square to the right (if we're not at the right edge), and if we have
                        // a bottom border, apply it as a top border on the square to the bottom (if we're not at the
                        // bottom edge).
                        val topBorder = cell.borderDirections.contains(Puzzle.BorderDirection.TOP)
                                || (y > 0 && grid[y - 1][x].borderDirections.contains(Puzzle.BorderDirection.BOTTOM))
                        val bottomBorder =
                            cell.borderDirections.contains(Puzzle.BorderDirection.BOTTOM) && y == grid.size - 1
                        val leftBorder = cell.borderDirections.contains(Puzzle.BorderDirection.LEFT)
                                || (x > 0 && grid[y][x - 1].borderDirections.contains(Puzzle.BorderDirection.RIGHT))
                        val rightBorder =
                            cell.borderDirections.contains(Puzzle.BorderDirection.RIGHT) && x == grid[y].size - 1
                        val backgroundPicture = when (cell.backgroundImage) {
                            is Puzzle.Image.None -> null
                            is Puzzle.Image.Data -> RectangularPuzzle.Crossword.Grid.BackgroundPicture(
                                encodedImage = cell.backgroundImage.bytes.base64(),
                                format = when (cell.backgroundImage.format) {
                                    Puzzle.ImageFormat.GIF -> "GIF"
                                    Puzzle.ImageFormat.JPG -> "JPG"
                                    Puzzle.ImageFormat.PNG -> "PNG"
                                }
                            )
                        }
                        RectangularPuzzle.Crossword.Grid.Cell(
                            x = x + 1,
                            y = y + 1,
                            solution = cell.solution.ifEmpty { null },
                            backgroundColor = cell.backgroundColor.ifEmpty { null },
                            backgroundPicture = backgroundPicture,
                            number = cell.number.ifEmpty { null },
                            type = type,
                            solveState = if (cell.cellType == Puzzle.CellType.CLUE || solved || cell.hint) {
                                cell.solution
                            } else {
                                cell.entry.ifEmpty { null }
                            },
                            topRightNumber = cell.topRightNumber.ifEmpty { null },
                            backgroundShape = backgroundShape,
                            topBar = if (topBorder) true else null,
                            bottomBar = if (bottomBorder) true else null,
                            leftBar = if (leftBorder) true else null,
                            rightBar = if (rightBorder) true else null,
                            hint = if (cell.hint) true else null,
                        )
                    }
                }.flatten(),
                gridLook = RectangularPuzzle.Crossword.Grid.GridLook(
                    numberingScheme = if (puzzleType == Puzzle.PuzzleType.CODED) "coded" else "normal"
                )
            )

            val jpzWords = words.map { word ->
                RectangularPuzzle.Crossword.Word(
                    id = word.id,
                    cells = word.cells.map { cell ->
                        RectangularPuzzle.Crossword.Word.Cells("${cell.x + 1}", "${cell.y + 1}")
                    }
                )
            }

            val jpzClues = clues.map { clueList ->
                val htmlTitle = clueList.title.toSnippet(hasHtmlClues)
                val title = if (hasHtmlClues) htmlTitle else listOf(B(htmlTitle))
                RectangularPuzzle.Crossword.Clues(
                    title = Html(title),
                    clues = clueList.clues.map { clue ->
                        RectangularPuzzle.Crossword.Clues.Clue(
                            word = clue.wordId,
                            number = clue.number,
                            text = clue.text.toSnippet(hasHtmlClues),
                            format = clue.format.ifEmpty { null },
                        )
                    })
            }

            val crossword = RectangularPuzzle.Crossword(jpzGrid, jpzWords, jpzClues)

            val combinedDescription = listOfNotNull(
                description.ifBlank { null },
                if (hasUnsupportedFeatures) UNSUPPORTED_FEATURES_WARNING else null
            ).joinToString("\n\n")

            val rectangularPuzzle = RectangularPuzzle(
                metadata = RectangularPuzzle.Metadata(
                    title = if (title.isBlank()) null else Html(title.toSnippet(hasHtmlClues)),
                    creator = if (creator.isBlank()) null else Html(creator.toSnippet(hasHtmlClues)),
                    copyright = if (copyright.isBlank()) null else Html(copyright.toSnippet(hasHtmlClues)),
                    description = if (combinedDescription.isBlank()) null else Html(
                        combinedDescription.toSnippet(hasHtmlClues)
                    ),
                ),
                crossword = if (puzzleType == Puzzle.PuzzleType.CROSSWORD) crossword else null,
                acrostic = if (puzzleType == Puzzle.PuzzleType.ACROSTIC) crossword else null,
                coded = if (puzzleType == Puzzle.PuzzleType.CODED) crossword else null,
                alphabet = if (puzzleType == Puzzle.PuzzleType.CODED) getAlphabet(grid) else null,
            )

            return if (
                completionMessage.isNotEmpty() ||
                appletSettings != null ||
                puzzleType == Puzzle.PuzzleType.CODED
            ) {
                val settings = appletSettings ?: CrosswordCompilerApplet.AppletSettings()
                val completion =
                    if (completionMessage.isEmpty()) {
                        settings.completion
                    } else {
                        settings.completion.copy(message = completionMessage)
                    }
                CrosswordCompilerApplet(
                    appletSettings = settings.copy(
                        completion = completion,
                        showAlphabet = if (puzzleType == Puzzle.PuzzleType.CODED) true else null
                    ),
                    rectangularPuzzle = rectangularPuzzle
                )
            } else {
                CrosswordCompiler(rectangularPuzzle = rectangularPuzzle)
            }
        }

        private fun getAlphabet(grid: List<List<Puzzle.Cell>>): String =
            grid.flatMap { row ->
                row.flatMap { cell -> cell.solution.toList() }
            }.toSet().sorted().joinToString("")

        private fun String.toSnippet(hasHtmlClues: Boolean): Snippet =
            htmlToSnippet(if (hasHtmlClues) this else formatPlainText(this))

        /**
         * Format plain text as valid inner HTML for a JPZ file.
         *
         * <p>Invalid XML characters are escaped, and text surrounded by asterisks is italicized.
         */
        private fun formatPlainText(plainText: String): String {
            val escapedClue = plainText.replace("&", "&amp;").replace("<", "&lt;")
            // Only italicize text if there are an even number of asterisks to try to avoid false positives on text like
            // "M*A*S*H". If this proves to trigger in other unintended circumstances, it may need to be removed from
            // here and applied instead at a higher level where the intent is clearer.
            val asteriskCount = escapedClue.count { it == '*' }
            return if (asteriskCount > 0 && asteriskCount % 2 == 0) {
                escapedClue.replace("\\*([^*]+)\\*".toRegex(), "<i>$1</i>")
            } else {
                escapedClue
            }
        }

        internal fun module(): SerializersModule {
            return SerializersModule {
                // Snippet tags
                polymorphic(Any::class, String::class, String.serializer())
                polymorphic(Any::class, B::class, B.serializer())
                polymorphic(Any::class, I::class, I.serializer())
                polymorphic(Any::class, Sub::class, Sub.serializer())
                polymorphic(Any::class, Sup::class, Sup.serializer())
                polymorphic(Any::class, Span::class, Span.serializer())
            }
        }

        @Serializable
        @SerialName("dummy")
        private data class Dummy(@XmlValue(true) val data: Snippet)

        /** Parse the given HTML string as a [Snippet] (i.e. for use in clues). */
        internal fun htmlToSnippet(html: String): Snippet {
            val dummyXml = "<dummy>$html</dummy>"
            var snippet = XML(module()) {
                autoPolymorphic = true
            }.decodeFromString(Dummy.serializer(), dummyXml).data
            snippet = trimWhitespace(snippet)
            // For better compatibility with Crossword Solver, we need to avoid mixed content. Unwrap any outer spans,
            // and then wrap plain text in spans whenever a snippet has HTML children.
            while (snippet.size == 1 && snippet[0] is Span) {
                snippet = (snippet[0] as Span).data
            }
            if (snippet.filterNot { it is String }.isEmpty()) {
                return snippet
            }
            return snippet.map {
                if (it is String) {
                    Span(listOf(it))
                } else {
                    it
                }
            }
        }

        /** Trim whitespace from the beginning and end of the given snippet. */
        private fun trimWhitespace(snippet: Snippet, start: Boolean = true, end: Boolean = true): Snippet {
            if (snippet.isEmpty()) {
                return snippet
            }
            val trimmed = snippet.toMutableList()
            if (start) {
                val first = trimmed[0]
                trimmed[0] = when (first) {
                    is String -> first.trimStart()
                    is B -> B(trimWhitespace(first.data, start = true, end = false))
                    is I -> I(trimWhitespace(first.data, start = true, end = false))
                    is Sub -> Sub(trimWhitespace(first.data, start = true, end = false))
                    is Sup -> Sup(trimWhitespace(first.data, start = true, end = false))
                    is Span -> Span(trimWhitespace(first.data, start = true, end = false))
                    else -> throw IllegalStateException("Unknown data type: $first")
                }
            }
            if (end) {
                val last = trimmed[trimmed.lastIndex]
                trimmed[trimmed.lastIndex] = when (last) {
                    is String -> last.trimEnd()
                    is B -> B(trimWhitespace(last.data, start = false, end = true))
                    is I -> I(trimWhitespace(last.data, start = false, end = true))
                    is Sub -> Sub(trimWhitespace(last.data, start = false, end = true))
                    is Sup -> Sup(trimWhitespace(last.data, start = false, end = true))
                    is Span -> Span(trimWhitespace(last.data, start = false, end = true))
                    else -> throw IllegalStateException("Unknown data type: $last")
                }
            }
            return trimmed
        }
    }
}

@Serializable
@XmlSerialName("crossword-compiler", "http://crossword.info/xml/crossword-compiler", "")
data class CrosswordCompiler(override val rectangularPuzzle: RectangularPuzzle) : Jpz() {
    override fun toXmlString(prettyPrint: Boolean): String {
        return getXmlSerializer(prettyPrint).encodeToString(serializer(), this)
    }
}

@Serializable
@XmlSerialName("crossword-compiler-applet", CCA_NS, "")
data class CrosswordCompilerApplet(
    val appletSettings: AppletSettings = AppletSettings(),
    override val rectangularPuzzle: RectangularPuzzle
) : Jpz() {

    @Serializable
    @SerialName("applet-settings")
    data class AppletSettings(
        @SerialName("cursor-color") val cursorColor: String = "#00B100",
        @SerialName("selected-cells-color") val selectedCellsColor: String = "#80FF80",
        @SerialName("show-alphabet") val showAlphabet: Boolean? = null,
        val completion: Completion = Completion(),
        val actions: Actions = Actions(),
        @XmlSerialName("title", CCA_NS, "") val title: Html? = null,
        @XmlSerialName("copyright", CCA_NS, "") val copyright: Html? = null,
    ) {

        @Serializable
        @SerialName("completion")
        data class Completion(
            @XmlValue(true) val message: String = "Congratulations! The puzzle is solved correctly.",
            @SerialName("only-if-correct") val onlyIfCorrect: Boolean = true
        )

        @Serializable
        @SerialName("actions")
        data class Actions(
            @SerialName("buttons-layout") val buttonsLayout: String = "left",
            @XmlSerialName("reveal-word", CCA_NS, "") val revealWord: Action? = Action("Reveal Word"),
            @XmlSerialName("reveal-letter", CCA_NS, "") val revealLetter: Action? = Action("Reveal Letter"),
            @XmlSerialName("check", CCA_NS, "") val check: Action? = Action("Check"),
            @XmlSerialName("solution", CCA_NS, "") val solution: Action? = Action("Solution"),
            @XmlSerialName("pencil", CCA_NS, "") val pencil: Action? = Action("Pencil")
        ) {

            @Serializable
            data class Action(@SerialName("label") val label: String)
        }
    }

    override fun toXmlString(prettyPrint: Boolean): String {
        return getXmlSerializer(prettyPrint).encodeToString(serializer(), this)
    }
}