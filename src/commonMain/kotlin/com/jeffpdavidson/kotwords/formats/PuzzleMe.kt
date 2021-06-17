package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.json.JsonSerializer
import com.jeffpdavidson.kotwords.formats.json.PuzzleMeJson
import com.jeffpdavidson.kotwords.model.BLACK_SQUARE
import com.jeffpdavidson.kotwords.model.Crossword
import com.jeffpdavidson.kotwords.model.Square
import io.ktor.utils.io.core.String

private val PUZZLE_DATA_REGEX = """\bwindow\.rawc\s*=\s*'([^']+)'""".toRegex()

/** Container for a puzzle in the PuzzleMe (Amuse Labs) format. */
class PuzzleMe(private val json: String) : Crosswordable {

    override fun asCrossword(): Crossword {
        val data = JsonSerializer.fromJson<PuzzleMeJson.Data>(json)
        val grid: MutableList<MutableList<Square>> = mutableListOf()

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

        // If bgColor == fgColor, assume the square is meant to be hidden/black and revealed after solving.
        val voidCells = data.cellInfos
            .filter { it.isVoid || (it.bgColor.isNotEmpty() && it.bgColor == it.fgColor) }.map { it.x to it.y }

        for (y in 0 until data.box[0].size) {
            val row: MutableList<Square> = mutableListOf()
            for (x in 0 until data.box.size) {
                // Treat black squares, void squares, and squares with no intersecting words that aren't pre-filled
                // (which likely means they're meant to be revealed after solving) as black squares.
                val box = data.box[x][y]
                if (box == null ||
                    box == "\u0000" ||
                    voidCells.contains(x to y) ||
                    (data.boxToPlacedWordsIdxs.isNotEmpty() && data.boxToPlacedWordsIdxs[x][y] == null &&
                            (data.preRevealIdxs.isEmpty() || !data.preRevealIdxs[x][y]))
                ) {
                    // Black square, though it may have a custom background color.
                    val backgroundColor =
                        if (box == "\u0000") {
                            cellInfoMap[x to y]?.bgColor?.ifEmpty { null }
                        } else {
                            null
                        }
                    row.add(BLACK_SQUARE.copy(backgroundColor = backgroundColor))
                } else {
                    val solutionRebus = if (box.length > 1) box else ""
                    val isCircled = circledCells.contains(x to y)
                    val isPrefilled = data.preRevealIdxs.isNotEmpty() && data.preRevealIdxs[x][y]
                    val number =
                        if (data.clueNums.isNotEmpty() && data.clueNums[x][y] != 0) {
                            data.clueNums[x][y]
                        } else {
                            null
                        }
                    row.add(
                        Square(
                            solution = box[0],
                            solutionRebus = solutionRebus,
                            isCircled = isCircled,
                            entry = if (isPrefilled) box[0] else null,
                            isGiven = isPrefilled,
                            number = number,
                            foregroundColor = cellInfoMap[x to y]?.fgColor?.ifEmpty { null },
                            backgroundColor = cellInfoMap[x to y]?.bgColor?.ifEmpty { null },
                            borderDirections =
                            setOfNotNull(
                                if (cellInfoMap[x to y]?.topWall == true) Square.BorderDirection.TOP else null,
                                if (cellInfoMap[x to y]?.bottomWall == true) {
                                    Square.BorderDirection.BOTTOM
                                } else {
                                    null
                                },
                                if (cellInfoMap[x to y]?.leftWall == true) Square.BorderDirection.LEFT else null,
                                if (cellInfoMap[x to y]?.rightWall == true) Square.BorderDirection.RIGHT else null,
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

        // Void cells can lead to entirely black rows/columns on the outer edges. Delete these.
        val anyNonBlackSquare = { row: List<Square> -> row.any { !it.isBlack } }
        val topRowsToDelete = grid.indexOfFirst(anyNonBlackSquare)
        val bottomRowsToDelete = grid.size - grid.indexOfLast(anyNonBlackSquare) - 1
        val leftRowsToDelete =
            grid.filter(anyNonBlackSquare).minOf { row -> row.indexOfFirst { !it.isBlack } }
        val rightRowsToDelete = grid[0].size -
                grid.filter(anyNonBlackSquare).maxOf { row -> row.indexOfLast { !it.isBlack } } - 1
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

        return Crossword(
            title = data.title.trim(),
            author = data.author.trim(),
            copyright = data.copyright.trim(),
            notes = data.description.ifBlank { data.help?.ifBlank { "" } ?: "" }.trim(),
            grid = filteredGrid,
            acrossClues = buildClueMap(acrossWords),
            downClues = buildClueMap(downWords),
            hasHtmlClues = true,
            acrossWords = buildWordList(acrossWords),
            downWords = buildWordList(downWords),
        )
    }

    companion object {
        fun fromHtml(html: String): PuzzleMe = PuzzleMe(extractPuzzleJson(html))

        fun fromRawc(rawc: String): PuzzleMe = PuzzleMe(String(Encodings.decodeBase64(rawc)))

        internal fun extractPuzzleJson(html: String): String {
            // Look for "window.rawc = '[data]'" inside <script> tags; this is JSON puzzle data
            // encoded as Base64.
            Xml.parse(html, format = DocumentFormat.HTML).select("script").forEach {
                val matchResult = PUZZLE_DATA_REGEX.find(it.data)
                if (matchResult != null) {
                    return String(Encodings.decodeBase64(matchResult.groupValues[1]))
                }
            }
            throw InvalidFormatException("Could not find puzzle data in PuzzleMe HTML")
        }

        private fun buildClueMap(clueList: List<PuzzleMeJson.PlacedWord>): Map<Int, String> =
            clueList.associate { it.clueNum to toHtml(it.clue.clue) }

        private fun buildWordList(words: List<PuzzleMeJson.PlacedWord>): List<Crossword.Word> {
            return words.map { word ->
                var x = word.x
                var y = word.y
                val squares = mutableListOf<Pair<Int, Int>>()
                repeat(word.nBoxes) {
                    squares.add(x to y)
                    if (word.acrossNotDown) {
                        x++
                    } else {
                        y++
                    }
                }
                Crossword.Word(id = (if (word.acrossNotDown) 0 else 1000) + word.clueNum, squares = squares)
            }
        }

        /**
         * Convert a PuzzleMe JSON string to HTML.
         *
         * PuzzleMe mixes unescaped special XML characters (&, <) with HTML tags. This method escapes the special
         * characters while leaving the HTML tags untouched.
         */
        private fun toHtml(clue: String): String {
            return clue
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace("&lt;(/?[^>]+)>".toRegex(), "<$1>")
        }
    }
}
