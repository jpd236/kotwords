package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.json.JsonSerializer
import com.jeffpdavidson.kotwords.formats.json.PuzzleMeJson
import com.jeffpdavidson.kotwords.model.MarchingBands
import com.jeffpdavidson.kotwords.model.Puzzle
import com.jeffpdavidson.kotwords.model.RowsGarden
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okio.ByteString.Companion.decodeBase64
import okio.ByteString.Companion.toByteString
import kotlin.math.min
import kotlin.math.roundToInt

private val PUZZLE_DATA_REGEX = """\bwindow\.(?:puzzleEnv\.)?rawc\s*=\s*'([^']+)'""".toRegex()

private val ROWS_REGEX = """Row \d+: """.toRegex(RegexOption.IGNORE_CASE)
private val BANDS_REGEX = """[^:]*band: """.toRegex(RegexOption.IGNORE_CASE)

private val KEY_DIGIT_REGEX = """<[a-z]+.length\?(\d+)""".toRegex()

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
            PuzzleMe(decodeRawc(rawc, crosswordJs = crosswordJs))

        internal fun extractPuzzleJson(html: String, crosswordJs: String = ""): String {
            return decodeRawc(extractRawc(html), crosswordJs = crosswordJs)
        }

        private fun extractRawc(html: String): String {
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

        private fun deobfuscateRawc(rawc: String, crosswordJs: String = ""): String {
            // If JS was provided, attempt to extract the key digits from it. This is a small optimization, but more
            // importantly, it can help as a bridge solution in case the domain of possible key digits changes and the
            // raw brute force solution stops working.
            val candidateKeyDigits: Set<Int> =
                if (crosswordJs.isNotEmpty()) {
                    val keyDigitMatches = KEY_DIGIT_REGEX.findAll(crosswordJs).toList()
                    if (keyDigitMatches.size == 7) {
                        keyDigitMatches.map { it.groupValues[1].toInt() }.toSet()
                    } else {
                        (2..19).toSet()
                    }
                } else {
                    (2..19).toSet()
                }
            val minKeyDigit = candidateKeyDigits.min()
            val maxKeyDigit = candidateKeyDigits.max()

            val candidateKeyPrefixes = mutableListOf<List<Int>>(listOf())

            // As an optimization, we expect the first two characters of the JSON to be {" or {\n, so we can use this to
            // guess the first digit of the key. If this heuristic fails, we just start with an empty key.
            val firstKeyDigit = min(
                rawc.indexOf("ye").let { if (it == -1) rawc.length else it },
                rawc.indexOf("we").let { if (it == -1) rawc.length else it },
            ) + 2
            if (firstKeyDigit in candidateKeyDigits) {
                candidateKeyPrefixes.add(listOf(firstKeyDigit))
            }

            while (!candidateKeyPrefixes.isEmpty()) {
                val candidateKeyPrefix = candidateKeyPrefixes.removeAt(candidateKeyPrefixes.lastIndex)
                if (candidateKeyPrefix.size == 7) {
                    // This is a full key candidate. Test if the decoding result is valid JSON, as it's possible that
                    // a slight variant of the valid key doesn't fail our heuristics.
                    val deobfuscatedRawc = deobfuscateRawc(rawc, candidateKeyPrefix)
                    try {
                        Json.decodeFromString<JsonElement>(deobfuscatedRawc.decodeBase64()?.utf8() ?: continue)
                    } catch (e: Exception) {
                        continue
                    }
                    return deobfuscatedRawc
                }
                candidateKeyPrefixes.addAll(candidateKeyDigits.map {
                    candidateKeyPrefix + it
                }.filter { newCandidateKeyPrefix ->
                    // While we don't know the rest of the key, we can put bounds on the sum of the digits of the key
                    // given its length. We can thus try every possible sum, decoding chunks of the rawc with the
                    // digits we have while leaving gaps for the remaining key digits between each chunk.
                    val remainingDigits = 7 - candidateKeyPrefix.size - 1
                    ((minKeyDigit * remainingDigits)..(maxKeyDigit * remainingDigits)).any { spacing ->
                        isValidKeyPrefix(rawc, newCandidateKeyPrefix, spacing)
                    }
                })
            }
            return ""
        }

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

        /**
         * Determine if the given key prefix could be valid, assuming the remainder of the key sums to [spacing].
         *
         * We decode all of the chunks of the input with the given key prefix, leaving spacing gaps to account for the
         * unknown digits of the key, and aligning to 4-byte boundaries as required by Base64. The key prefix is
         * considered valid as long as each chunk is valid Base64 and all of the characters are valid, visible unicode.
         */
        private fun isValidKeyPrefix(rawc: String, keyPrefix: List<Int>, spacing: Int): Boolean {
            var pos = 0
            val chunk = StringBuilder(keyPrefix.sum())
            while (pos < rawc.length) {
                // Assemble a chunk of decoded data starting from pos using the key prefix.
                val startPos = pos
                var keyIndex = 0
                while (keyIndex < keyPrefix.size && pos < rawc.length) {
                    val chunkLength = min(keyPrefix[keyIndex++], rawc.length - pos)
                    chunk.append(rawc.substring(pos, pos + chunkLength).reversed())
                    pos += chunkLength
                }

                // Align the chunk to 4-byte boundaries, since Base64 comes in 4-byte sections. If Base64 decoding fails
                // altogether, we know this prefix/spacing is invalid.
                val base64Start = ((startPos + 3) / 4) * 4 - startPos
                val base64End = (pos / 4) * 4 - startPos
                val deobfuscatedChunk = chunk.substring(base64Start, base64End).decodeBase64() ?: return false

                // Reject the decoding if we get any invalid UTF-8 byte. We could be stricter here by validating that
                // the sequence as a whole could be a valid UTF-8 subsequence, considering the bytes that could come
                // before/after, but this seems like a good enough heuristic.
                if (
                    deobfuscatedChunk.toByteArray().any { ch ->
                        val byte = ch.toInt() and 0xFF
                        (byte < 32 && byte != 0x09 && byte != 0x0A && byte != 0x0D) ||
                                byte == 0xC0 || byte == 0xC1 || byte >= 0xF5
                    }) {
                    return false
                }

                // Skip over spacing to reach the next decodable chunk.
                pos += spacing
                chunk.clear()
            }

            // Reached the end without any invalid chunks - this may be a valid key prefix.
            return true
        }

        private fun decodeRawc(rawc: String, key: List<Int> = listOf(), crosswordJs: String = ""): String {
            val deobfuscatedRawc = if (key.isNotEmpty()) {
                deobfuscateRawc(rawc, key)
            } else {
                deobfuscateRawc(rawc, crosswordJs)
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
