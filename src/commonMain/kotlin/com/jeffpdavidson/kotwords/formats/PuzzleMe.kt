package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.json.JsonSerializer
import com.jeffpdavidson.kotwords.formats.json.PuzzleMeJson
import com.jeffpdavidson.kotwords.model.MarchingBands
import com.jeffpdavidson.kotwords.model.Puzzle
import com.jeffpdavidson.kotwords.model.RowsGarden
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import okio.ByteString.Companion.decodeBase64
import okio.ByteString.Companion.toByteString
import kotlin.math.min
import kotlin.math.roundToInt

private val PUZZLE_DATA_REGEX = """\bwindow\.(?:puzzleEnv\.)?rawc\s*=\s*'([^']+)'""".toRegex()
private val KEY_REGEX = """var [a-zA-Z]+\s*=\s*"([0-9a-f]{7,})"""".toRegex()
private val KEY_DIGIT_REGEX = """.push\((\d+)\)""".toRegex()

private val KEY_2_ORDER_REGEX = """[a-z]+=(\d+);[a-z]+<[a-z]+.length;[a-z]+\+=""".toRegex()
private val KEY_2_DIGIT_REGEX = """<[a-z]+.length\?(\d+)""".toRegex()

private val ROWS_REGEX = """Row \d+: """.toRegex(RegexOption.IGNORE_CASE)
private val BANDS_REGEX = """[^:]*band: """.toRegex(RegexOption.IGNORE_CASE)

/** Container for a puzzle in the PuzzleMe (Amuse Labs) format. */
class PuzzleMe(val json: String) : DelegatingPuzzleable() {

    override suspend fun getPuzzleable(): Puzzleable {
        val data = JsonSerializer.fromJson<PuzzleMeJson.Data>(json)
        val grid: MutableList<MutableList<Puzzle.Cell>> = mutableListOf()

        val cellInfoMap = data.cellInfos.associateBy { it.x to it.y }

        // PuzzleMe supports circled cells and cells with special background shapes. We pick one
        // mechanism to map to circles (preferring "isCircled" which is a direct match) and the
        // other if present.
        val circledCells =
            when {
                data.cellInfos.find { it.isCircled } != null -> {
                    cellInfoMap.filterValues { it.isCircled }.keys
                }

                else -> {
                    data.backgroundShapeBoxes.filter { it.size == 2 }.map { it[0] to it[1] }
                }
            }

        val images = data.imagesInGrid.flatMap { image ->
            val format = ParsedImageFormat.fromExtension(image.imageFormat)
            val parsedImage = ParsedImage.parse(format, image.image.decodeBase64()!!.toByteArray())
            val widthBoxes = image.endX - image.startX + 1
            val heightBoxes = image.endY - image.startY + 1
            val imageWidth = (parsedImage.width / widthBoxes.toDouble())
            val imageHeight = (parsedImage.height / heightBoxes.toDouble())
            (image.startX..image.endX).flatMap { x ->
                (image.startY..image.endY).flatMap { y ->
                    val croppedImage = parsedImage.crop(
                        width = imageWidth.roundToInt(),
                        height = imageHeight.roundToInt(),
                        x = ((x - image.startX) * imageWidth).roundToInt(),
                        y = ((y - image.startY) * imageHeight).roundToInt(),
                    )
                    if (croppedImage.containsVisiblePixels()) {
                        listOf((x to y) to croppedImage.toPngBytes())
                    } else {
                        listOf()
                    }
                }
            }
        }.toMap()

        for (y in 0 until data.box[0].size) {
            val row: MutableList<Puzzle.Cell> = mutableListOf()
            for (x in 0 until data.box.size) {
                // Treat black squares, void squares, and squares with no intersecting words that aren't pre-filled
                // (which likely means they're meant to be revealed after solving) as black squares.
                val box = data.box[x][y]
                val cellInfo = cellInfoMap[x to y]
                val isBlack = box == null || box == "\u0000"
                val isVoid = cellInfo?.isVoid == true
                // If bgColor == fgColor, assume the square is meant to be hidden/black and revealed after solving.
                val isInvisible = cellInfo?.bgColor?.isNotEmpty() == true && cellInfo.bgColor == cellInfo.fgColor
                // If the square has no intersecting words that aren't pre-filled, assume the square is likely meant to
                // be revealed after solving.
                val hasNoIntersectingWords =
                    data.boxToPlacedWordsIdxs.isNotEmpty() &&
                            (!data.boxToPlacedWordsIdxs.indices.contains(x) ||
                                    !data.boxToPlacedWordsIdxs[x].indices.contains(y) ||
                                    data.boxToPlacedWordsIdxs[x][y] == null)
                val isPrefilled = data.preRevealIdxs.isNotEmpty() && data.preRevealIdxs[x][y]
                val backgroundImage =
                    if (images.containsKey(x to y)) {
                        Puzzle.Image.Data(
                            format = Puzzle.ImageFormat.PNG,
                            bytes = images[x to y]!!.toByteString()
                        )
                    } else {
                        Puzzle.Image.None
                    }
                val borderDirections =
                    setOfNotNull(
                        if (cellInfo?.topWall == true) Puzzle.BorderDirection.TOP else null,
                        if (cellInfo?.bottomWall == true) Puzzle.BorderDirection.BOTTOM else null,
                        if (cellInfo?.leftWall == true) Puzzle.BorderDirection.LEFT else null,
                        if (cellInfo?.rightWall == true) Puzzle.BorderDirection.RIGHT else null,
                    )

                if (isBlack || isVoid || isInvisible || (hasNoIntersectingWords && !isPrefilled)) {
                    // Black square, though it may have a custom background and/or borders.
                    val backgroundColor =
                        if (isBlack) {
                            cellInfoMap[x to y]?.bgColor ?: ""
                        } else {
                            ""
                        }
                    row.add(
                        Puzzle.Cell(
                            cellType = if (isVoid) Puzzle.CellType.VOID else Puzzle.CellType.BLOCK,
                            backgroundColor = backgroundColor,
                            backgroundImage = backgroundImage,
                            borderDirections = borderDirections,
                        )
                    )
                } else {
                    val backgroundShape =
                        if (circledCells.contains(x to y)) {
                            Puzzle.BackgroundShape.CIRCLE
                        } else {
                            Puzzle.BackgroundShape.NONE
                        }
                    val number =
                        if (data.clueNums.isNotEmpty() && (data.clueNums[x][y] ?: 0) != 0) {
                            data.clueNums[x][y].toString()
                        } else {
                            ""
                        }
                    row.add(
                        Puzzle.Cell(
                            solution = if (isPrefilled && box!! == "*") {
                                ""
                            } else {
                                box!!
                            },
                            cellType = if (isPrefilled) Puzzle.CellType.CLUE else Puzzle.CellType.REGULAR,
                            backgroundShape = backgroundShape,
                            number = number,
                            foregroundColor = cellInfo?.fgColor ?: "",
                            backgroundColor = cellInfo?.bgColor ?: "",
                            borderDirections = borderDirections,
                            backgroundImage = backgroundImage,
                        )
                    )
                }
            }
            if (grid.size > 0 && grid[0].size != row.size) {
                throw InvalidFormatException("Grid is not rectangular")
            }
            grid.add(row)
        }

        // Post-solve revealed squares can lead to entirely black rows/columns on the outer edges. Delete these.
        val anyNonBlackSquare = { row: List<Puzzle.Cell> -> row.any { !it.cellType.isBlack() } }
        val topRowsToDelete = grid.indexOfFirst(anyNonBlackSquare)
        val bottomRowsToDelete = grid.size - grid.indexOfLast(anyNonBlackSquare) - 1
        val leftRowsToDelete =
            grid.filter(anyNonBlackSquare).minOf { row -> row.indexOfFirst { !it.cellType.isBlack() } }
        val rightRowsToDelete = grid[0].size -
                grid.filter(anyNonBlackSquare).maxOf { row -> row.indexOfLast { !it.cellType.isBlack() } } - 1
        val filteredGrid = grid.drop(topRowsToDelete).dropLast(bottomRowsToDelete)
            .map { row -> row.drop(leftRowsToDelete).dropLast(rightRowsToDelete) }

        // Ignore words that don't have a proper clue associated.
        val words = data.placedWords.filterNot { it.clueNum == "0" }
        val acrossWords = words
            .filter { it.acrossNotDown }
            .filter { it.y >= topRowsToDelete && it.y <= grid.size - bottomRowsToDelete }
            .map { it.copy(x = it.x - leftRowsToDelete, y = it.y - topRowsToDelete) }
        val downWords = words
            .filterNot { it.acrossNotDown }
            .filter { it.x >= leftRowsToDelete && it.x <= grid[0].size - rightRowsToDelete }
            .map { it.copy(x = it.x - leftRowsToDelete, y = it.y - topRowsToDelete) }
        return createPuzzleable(data, filteredGrid, acrossWords, downWords)
    }

    companion object {
        /**
         * Returns the URL containing the crosswordJs to be passed to [fromHtml], or null if it couldn't be determined.
         */
        fun getCrosswordJsUrl(html: String, baseUri: String): String? {
            return Xml.parse(html, baseUri, format = DocumentFormat.HTML)
                .selectFirst("script[src*='c-min.js']")?.attr("abs:src")
        }

        fun fromHtml(html: String, crosswordJs: String = ""): PuzzleMe = PuzzleMe(extractPuzzleJson(html, crosswordJs))

        fun fromRawc(rawc: String, crosswordJs: String = ""): PuzzleMe =
            PuzzleMe(decodeRawc(rawc, crosswordJs))

        internal fun extractPuzzleJson(html: String, crosswordJs: String = ""): String {
            return decodeRawc(extractRawc(html), crosswordJs)
        }

        internal fun extractRawc(html: String): String {
            val document = Xml.parse(html, format = DocumentFormat.HTML)

            // Newer mechanism: parameters are embedded in <script id="params"> JSON.
            val params = document.selectFirst("script#params")
            if (params != null) {
                val puzzleParams = JsonSerializer.fromJson<PuzzleMeJson.PuzzleParams>(params.data)
                if (puzzleParams.rawc != null) {
                    return puzzleParams.rawc
                }
            }

            // Older mechanism: look for "window.rawc = '[data]'" inside <script> tags; this is JSON puzzle data
            // encoded as Base64.
            document.select("script").forEach {
                val matchResult = PUZZLE_DATA_REGEX.find(it.data)
                if (matchResult != null) {
                    return matchResult.groupValues[1]
                }
            }
            throw InvalidFormatException("Could not find puzzle data in PuzzleMe HTML")
        }

        private fun deobfuscateRawc(rawc: String): String {
            val rawcParts = rawc.split(".")
            return deobfuscateRawc(rawcParts[0], rawcParts[1].reversed())
        }

        private fun deobfuscateRawc(rawc: String, keyStr: String): String {
            return deobfuscateRawc(rawc, convertKeyStrToKey(keyStr))
        }

        private fun convertKeyStrToKey(keyStr: String): List<Int> = keyStr.map { it.digitToInt(16) + 2 }

        private fun deobfuscateRawc(rawc: String, key: List<Int>): String {
            val buffer = rawc.toCharArray()
            var i = 0
            var segmentCount = 0
            while (i < buffer.size - 1) {
                // Reverse sections of the buffer, using the key digits as lengths of each section.
                val segmentLength = min(key[segmentCount++ % key.size], buffer.size - i)
                (0 until segmentLength / 2).forEach { j ->
                    val temp = buffer[i + j]
                    buffer[i + j] = buffer[i + segmentLength - j - 1]
                    buffer[i + segmentLength - j - 1] = temp
                }
                i += segmentLength
            }
            return buffer.joinToString("")
        }

        internal fun decodeRawc(rawc: String, crosswordJs: String): String {
            if (!rawc.contains('.') && crosswordJs.isNotEmpty()) {
                // Try to find the individual numbers of the key and their order in the Javascript.
                // The order regex is overly permissive, but the digits should appear as a consecutive subsequence, so
                // we can try all subsequences until we find one that decodes successfully.
                val keyDigitMatches = KEY_2_DIGIT_REGEX.findAll(crosswordJs).toList()
                val keyOrderMatches = KEY_2_ORDER_REGEX.findAll(crosswordJs).toList()
                if (keyDigitMatches.isNotEmpty() && keyDigitMatches.size <= keyOrderMatches.size) {
                    val keyDigits = keyDigitMatches.map { it.groupValues[1].toInt() }
                    val keyOrders = keyOrderMatches.map { it.groupValues[1].toInt() }
                    (0..keyOrderMatches.size - keyDigitMatches.size).forEach { startIndex ->
                        val key = keyDigits.zip(keyOrders.subList(startIndex, startIndex + keyDigitMatches.size))
                            .sortedBy { it.second }.map { it.first }
                        try {
                            val decoded = decodeRawc(rawc, key)
                            // Ensure the result is valid JSON.
                            JsonSerializer.fromJson<JsonObject>(decoded)
                            return decoded
                        } catch (e: Exception) {
                            // Assume this is an invalid key; try the next technique.
                        }
                    }
                }

                // Try another way to find the individual numbers of the key in the Javascript.
                val keyDigits = KEY_DIGIT_REGEX.findAll(crosswordJs).toList()
                if (keyDigits.isNotEmpty()) {
                    val key = keyDigits.map { matchResult -> matchResult.groupValues[1].toInt() }
                    try {
                        return decodeRawc(rawc, key)
                    } catch (e: InvalidFormatException) {
                        // Assume this is an invalid key; try the next technique.
                    }
                }

                // Try to find the key variable in the Javascript.
                KEY_REGEX.findAll(crosswordJs).forEach { matchResult ->
                    val decodedRawc = try {
                        decodeRawc(rawc, convertKeyStrToKey(matchResult.groupValues[1]))
                    } catch (e: InvalidFormatException) {
                        // Assume this is an invalid key; try the next match.
                        return@forEach
                    }
                    return decodedRawc
                }
            }
            return decodeRawc(rawc)
        }

        private fun decodeRawc(rawc: String, key: List<Int> = listOf()): String {
            val deobfuscatedRawc = if (rawc.contains(".")) {
                deobfuscateRawc(rawc)
            } else if (key.isNotEmpty()) {
                deobfuscateRawc(rawc, key)
            } else {
                rawc
            }
            return deobfuscatedRawc.decodeBase64()?.utf8() ?: throw InvalidFormatException("Rawc is invalid base64")
        }

        private fun buildClueMap(
            isAcross: Boolean, clueList: List<PuzzleMeJson.PlacedWord>, wordLengthsEnabled: Boolean
        ): List<Puzzle.Clue> =
            clueList.mapIndexed { i, word ->
                val format = if (wordLengthsEnabled && word.wordLens.isNotEmpty()) {
                    word.wordLens.joinToString(", ")
                } else {
                    ""
                }
                Puzzle.Clue(
                    wordId = getWordId(isAcross, "${i + 1}"),
                    number = word.clueNum,
                    text = toHtml(word.clue.clue),
                    format = format,
                )
            }

        private fun buildWordList(
            grid: List<List<Puzzle.Cell>>,
            words: List<PuzzleMeJson.PlacedWord>
        ): List<Puzzle.Word> {
            return words.mapIndexed { i, word ->
                var x = word.x
                var y = word.y
                val cells = mutableListOf<Puzzle.Coordinate>()
                if (word.boxesForWord.isNotEmpty()) {
                    cells.addAll(word.boxesForWord.map { box -> Puzzle.Coordinate(x = box.x, y = box.y) })
                } else {
                    repeat(word.nBoxes) {
                        cells.add(Puzzle.Coordinate(x = x, y = y))
                        if (word.acrossNotDown) {
                            x++
                        } else {
                            y++
                        }
                    }
                }
                Puzzle.Word(
                    id = getWordId(isAcross = word.acrossNotDown, clueNum = "${i + 1}"),
                    // Filter out any squares that fall outside the grid (e.g. due to void squares) or which cannot
                    // have letters entered in them.
                    cells = cells.filter { (x, y) ->
                        y >= 0 && y < grid.size && x >= 0 && x < grid[y].size
                                && grid[y][x].cellType == Puzzle.CellType.REGULAR
                    }
                )
            }
        }

        private fun getWordId(isAcross: Boolean, clueNum: String): Int = (if (isAcross) 0 else 1000) + clueNum.toInt()

        /**
         * Convert a PuzzleMe JSON string to HTML.
         *
         * PuzzleMe mixes unescaped special XML characters (&, <) with HTML tags. Sometimes there are real HTML escape
         * sequences as well. This method escapes the special characters while leaving supported HTML tags untouched.
         * <br> tags are replaced with newlines. Attributes are stripped out.
         */
        internal fun toHtml(clue: String): String {
            return Encodings.decodeHtmlEntities(clue)
                .replace("&", "&amp;")
                .replace("\\s*<br/?>\\s*".toRegex(RegexOption.IGNORE_CASE), "\n")
                // Strip other unsupported tags.
                .replace("</?(?:div|img)(?: [^>]*)?/?>".toRegex(RegexOption.IGNORE_CASE), "")
                // Strip <a> tags but leave their contents.
                .replace("<a(?: [^>]*)?>(.*?)</a>".toRegex(RegexOption.IGNORE_CASE), "$1")
                .replace("<", "&lt;")
                // Workaround for New Yorker titles. These use a span with a custom style to add a margin between the
                // date and the title; since we don't support styles, add a space to separate them.
                .replace("&lt;span[^>]*class=\"[^\"]*subtitle[^\"]*\">".toRegex(RegexOption.IGNORE_CASE), " <span>")
                .replace("&lt;(/?(?:b|i|sup|sub|span))(?: [^>]*)?>".toRegex(RegexOption.IGNORE_CASE), "<$1>")
        }

        private fun createPuzzleable(
            data: PuzzleMeJson.Data,
            filteredGrid: List<List<Puzzle.Cell>>,
            acrossWords: List<PuzzleMeJson.PlacedWord>,
            downWords: List<PuzzleMeJson.PlacedWord>,
        ): Puzzleable {
            val title = toHtml(data.title.trim())
            val creator = toHtml(data.author.trim())
            val copyright = toHtml(data.copyright.trim())
            val description = toHtml(data.description.ifBlank { data.help?.ifBlank { "" } ?: "" }.trim())

            return if (acrossWords.all { it.clueSection == "Rows" } && downWords.all { it.clueSection == "Blooms" }) {
                // Assume this is a Rows Garden puzzle.
                RowsGarden(
                    title = title,
                    creator = creator,
                    copyright = copyright,
                    description = description,
                    rows = acrossWords.map { word ->
                        val answers = word.originalTerm.split(" / ")
                        val clues = toHtml(word.clue.clue).split(" / ")
                        if (answers.size != clues.size) {
                            throw InvalidFormatException("Row clue has mismatched clue and answer counts")
                        }
                        clues.zip(answers).map { (clue, answer) ->
                            RowsGarden.Entry(clue = clue, answer = answer)
                        }
                    },
                    light = downWords.filter { it.clueNum.toInt() == acrossWords.size + 1 }.map { word ->
                        RowsGarden.Entry(clue = toHtml(word.clue.clue), answer = word.originalTerm)
                    },
                    medium = downWords.filter { it.clueNum.toInt() == acrossWords.size + 3 }.map { word ->
                        RowsGarden.Entry(clue = toHtml(word.clue.clue), answer = word.originalTerm)
                    },
                    dark = downWords.filter { it.clueNum.toInt() == acrossWords.size + 2 }.map { word ->
                        RowsGarden.Entry(clue = toHtml(word.clue.clue), answer = word.originalTerm)
                    },
                    addWordCount = false,
                    addHyphenated = false,
                    hasHtmlClues = true,
                )
            } else {
                val grid: List<List<Puzzle.Cell>>
                val clues: List<Puzzle.ClueList>
                val words: List<Puzzle.Word>

                if (acrossWords.all { it.clue.clue.isEmpty() || it.clue.clue.contains(ROWS_REGEX) } &&
                    downWords.all { it.clue.clue.isEmpty() || it.clue.clue.contains(BANDS_REGEX) }) {
                    // Assume this is a legacy Marching Bands puzzle. There's no other indication apparent in the data
                    // itself; the only proper way to know is to look at the full HTML to see if the extra "Sparkling
                    // bands" Javascript code which post-processes the raw puzzle data is included. Newer puzzles should
                    // have proper boxesForWords and clueSections and can be processed as standard puzzles.

                    // Regenerate words since the provided ones are invalid.
                    val rowWords = acrossWords
                        .filterNot { it.clue.clue.isEmpty() }.mapIndexed { i, word ->
                            word.copy(
                                clueNum = "${i + 1}",
                                // Clear the "Row [n]: " prefix
                                clue = word.clue.copy(clue = word.clue.clue.replace(ROWS_REGEX, "")),
                                x = 0,
                                y = word.y,
                                nBoxes = filteredGrid[0].size,
                                boxesForWord = listOf(),
                            )
                        }
                    val rowWordList = buildWordList(filteredGrid, rowWords)
                    val bandWords = downWords.filterNot { it.clue.clue.isEmpty() }.map { word ->
                        // Clear the "[Color] band: " prefix
                        word.copy(
                            clue = word.clue.copy(clue = word.clue.clue.replace(BANDS_REGEX, "")),
                            boxesForWord = listOf()
                        )
                    }
                    val bandWordList = List(bandWords.size) { i ->
                        Puzzle.Word(
                            id = getWordId(isAcross = false, clueNum = "${i + 1}"),
                            cells = MarchingBands.getBandCells(
                                width = filteredGrid[0].size,
                                height = filteredGrid.size,
                                bandIndex = i,
                            )
                        )
                    }
                    val bandClues = bandWords.mapIndexed { i, word ->
                        Puzzle.Clue(
                            wordId = getWordId(isAcross = false, clueNum = "${i + 1}"),
                            number = ('A' + i).toString(),
                            text = toHtml(word.clue.clue)
                        )
                    }

                    // Clear extraneous numbers and add band letters.
                    grid = filteredGrid.mapIndexed { y, row ->
                        row.mapIndexed { x, cell ->
                            val topRightNumber =
                                if (x == y && x < bandClues.size && cell.cellType == Puzzle.CellType.REGULAR) {
                                    ('A' + y).toString()
                                } else {
                                    ""
                                }
                            cell.copy(
                                number = if (x == 0) "${y + 1}" else "",
                                topRightNumber = topRightNumber,
                            )
                        }
                    }
                    clues = listOf(
                        Puzzle.ClueList(
                            "<b>Rows</b>",
                            buildClueMap(isAcross = true, clueList = rowWords, wordLengthsEnabled = false),
                        ),
                        Puzzle.ClueList("<b>Bands</b>", bandClues),
                    )
                    words = bandWordList + rowWordList
                } else {
                    val clueTitles = if (data.clueSections.size == 2) data.clueSections else listOf("Across", "Down")
                    grid = filteredGrid
                    clues = listOf(
                        Puzzle.ClueList(
                            "<b>${clueTitles[0]}</b>",
                            buildClueMap(
                                isAcross = true, clueList = acrossWords, wordLengthsEnabled = data.wordLengthsEnabled
                            )
                        ),
                        Puzzle.ClueList(
                            "<b>${clueTitles[1]}</b>",
                            buildClueMap(
                                isAcross = false, clueList = downWords, wordLengthsEnabled = data.wordLengthsEnabled
                            )
                        )
                    )
                    words = buildWordList(filteredGrid, acrossWords) + buildWordList(filteredGrid, downWords)
                }

                return Puzzle(
                    title = title,
                    creator = creator,
                    copyright = copyright,
                    description = description,
                    grid = grid,
                    clues = clues,
                    hasHtmlClues = true,
                    words = words,
                )
            }
        }
    }
}
