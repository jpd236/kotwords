package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.json.JsonSerializer
import com.jeffpdavidson.kotwords.formats.json.PuzzleMeJson
import com.jeffpdavidson.kotwords.model.Puzzle
import okio.ByteString.Companion.decodeBase64

private val PUZZLE_DATA_REGEX = """\bwindow\.rawc\s*=\s*'([^']+)'""".toRegex()

/** Container for a puzzle in the PuzzleMe (Amuse Labs) format. */
class PuzzleMe(private val json: String) : Puzzleable {

    override fun asPuzzle(): Puzzle {
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

                if (isBlack || isVoid || isInvisible || (hasNoIntersectingWords && !isPrefilled)) {
                    // Black square, though it may have a custom background color.
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
                        if (data.clueNums.isNotEmpty() && data.clueNums[x][y] != 0) {
                            data.clueNums[x][y].toString()
                        } else {
                            ""
                        }
                    row.add(
                        Puzzle.Cell(
                            solution = box!!,
                            cellType = if (isPrefilled) Puzzle.CellType.CLUE else Puzzle.CellType.REGULAR,
                            backgroundShape = backgroundShape,
                            number = number,
                            foregroundColor = cellInfo?.fgColor ?: "",
                            backgroundColor = cellInfo?.bgColor ?: "",
                            borderDirections =
                            setOfNotNull(
                                if (cellInfo?.topWall == true) Puzzle.BorderDirection.TOP else null,
                                if (cellInfo?.bottomWall == true) Puzzle.BorderDirection.BOTTOM else null,
                                if (cellInfo?.leftWall == true) Puzzle.BorderDirection.LEFT else null,
                                if (cellInfo?.rightWall == true) Puzzle.BorderDirection.RIGHT else null,
                            )
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

        val acrossWords = data.placedWords
            .filter { it.acrossNotDown }
            .filter { it.y >= topRowsToDelete && it.y <= grid.size - bottomRowsToDelete }
            .map { it.copy(x = it.x - leftRowsToDelete, y = it.y - topRowsToDelete) }
        val downWords = data.placedWords
            .filterNot { it.acrossNotDown }
            .filter { it.x >= leftRowsToDelete && it.x <= grid[0].size - rightRowsToDelete }
            .map { it.copy(x = it.x - leftRowsToDelete, y = it.y - topRowsToDelete) }

        return Puzzle(
            title = data.title.trim(),
            creator = data.author.trim(),
            copyright = data.copyright.trim(),
            description = data.description.ifBlank { data.help?.ifBlank { "" } ?: "" }.trim(),
            grid = filteredGrid,
            clues = listOf(
                Puzzle.ClueList("<b>Across</b>", buildClueMap(isAcross = true, clueList = acrossWords)),
                Puzzle.ClueList("<b>Down</b>", buildClueMap(isAcross = false, clueList = downWords))
            ),
            hasHtmlClues = true,
            words = buildWordList(filteredGrid, acrossWords + downWords),
        )
    }

    companion object {
        fun fromHtml(html: String): PuzzleMe = PuzzleMe(extractPuzzleJson(html))

        fun fromRawc(rawc: String): PuzzleMe = PuzzleMe(decodeRawc(rawc))

        internal fun extractPuzzleJson(html: String): String {
            // Look for "window.rawc = '[data]'" inside <script> tags; this is JSON puzzle data
            // encoded as Base64.
            Xml.parse(html, format = DocumentFormat.HTML).select("script").forEach {
                val matchResult = PUZZLE_DATA_REGEX.find(it.data)
                if (matchResult != null) {
                    return decodeRawc(matchResult.groupValues[1])
                }
            }
            throw InvalidFormatException("Could not find puzzle data in PuzzleMe HTML")
        }

        private fun decodeRawc(rawc: String) =
            rawc.decodeBase64()?.utf8() ?: throw InvalidFormatException("Rawc is invalid base64")

        private fun buildClueMap(isAcross: Boolean, clueList: List<PuzzleMeJson.PlacedWord>): List<Puzzle.Clue> =
            clueList.map {
                Puzzle.Clue(
                    wordId = getWordId(isAcross, it.clueNum),
                    number = it.clueNum.toString(),
                    text = toHtml(it.clue.clue)
                )
            }

        private fun buildWordList(
            grid: List<List<Puzzle.Cell>>,
            words: List<PuzzleMeJson.PlacedWord>
        ): List<Puzzle.Word> {
            return words.map { word ->
                var x = word.x
                var y = word.y
                val cells = mutableListOf<Puzzle.Coordinate>()
                repeat(word.nBoxes) {
                    cells.add(Puzzle.Coordinate(x = x, y = y))
                    if (word.acrossNotDown) {
                        x++
                    } else {
                        y++
                    }
                }
                Puzzle.Word(
                    id = getWordId(isAcross = word.acrossNotDown, clueNum = word.clueNum),
                    // Filter out any squares that fall outside the grid (e.g. due to void squares).
                    cells = cells.filter { (x, y) -> y >= 0 && y < grid.size && x >= 0 && x < grid[y].size }
                )
            }
        }

        private fun getWordId(isAcross: Boolean, clueNum: Int): Int = (if (isAcross) 0 else 1000) + clueNum

        /**
         * Convert a PuzzleMe JSON string to HTML.
         *
         * PuzzleMe mixes unescaped special XML characters (&, <) with HTML tags. This method escapes the special
         * characters while leaving supported HTML tags untouched.
         */
        internal fun toHtml(clue: String): String {
            return clue
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace("&lt;(/?(?:b|i|sup|sub|span))>".toRegex(RegexOption.IGNORE_CASE), "<$1>")
        }
    }
}
