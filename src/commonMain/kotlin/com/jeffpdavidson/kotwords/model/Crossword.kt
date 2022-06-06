package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.formats.FONT_FAMILY_TIMES_ROMAN
import com.jeffpdavidson.kotwords.formats.Pdf.asPdf
import com.jeffpdavidson.kotwords.formats.PdfFontFamily
import com.jeffpdavidson.kotwords.formats.Puzzleable

/**
 * A representation of a crossword puzzle with standard numbering.
 *
 * For non-standard words/numbering, use [Puzzle] directly.
 *
 * @constructor Construct a new puzzle.
 * @param title The title of the puzzle.
 * @param creator The author of the puzzle.
 * @param copyright The copyright of the puzzle.
 * @param description Optional notes about the puzzle.
 * @param grid The grid of [Puzzle.Cell]s in the form of a list of rows going from top to bottom, with
 *             each row going from left to right.
 * @param acrossClues Mapping from across clue number to the clue for that number.
 * @param downClues Mapping from down clue number to the clue for that number.
 * @param hasHtmlClues Whether clue contents are in HTML.
 */
data class Crossword(
    val title: String,
    val creator: String,
    val copyright: String,
    val description: String = "",
    val grid: List<List<Puzzle.Cell>>,
    val acrossClues: Map<Int, String>,
    val downClues: Map<Int, String>,
    val hasHtmlClues: Boolean = false,
) : Puzzleable {
    init {
        // Validate that grid is a rectangle.
        val width = grid[0].size
        grid.forEachIndexed { index, row ->
            require(row.size == width) {
                "Invalid grid - row $index has width ${row.size} but should be $width"
            }
        }
        // TODO: Validate standard grid numbering / clues.
    }

    override suspend fun asPuzzle(): Puzzle {
        val acrossPuzzleClues = mutableListOf<Puzzle.Clue>()
        val downPuzzleClues = mutableListOf<Puzzle.Clue>()
        val words = mutableListOf<Puzzle.Word>()

        // Generate numbers and words based on standard crossword numbering.
        val gridNumbers = mutableMapOf<Pair<Int, Int>, Int>()
        forEachCell(grid) { x, y, clueNumber, isAcross, isDown, _ ->
            if (clueNumber != null) {
                gridNumbers[x to y] = clueNumber
            }
            if (isAcross) {
                val word = mutableListOf<Puzzle.Coordinate>()
                var i = x
                do {
                    word.add(Puzzle.Coordinate(x = i, y = y))
                } while (!hasBorder(grid, i++, y, Puzzle.BorderDirection.RIGHT, useBorders = true))
                if (clueNumber != null && acrossClues.containsKey(clueNumber)) {
                    acrossPuzzleClues.add(
                        Puzzle.Clue(clueNumber, "$clueNumber", acrossClues[clueNumber]!!)
                    )
                    words.add(Puzzle.Word(clueNumber, word))
                }
            }
            if (isDown) {
                val word = mutableListOf<Puzzle.Coordinate>()
                var j = y
                do {
                    word.add(Puzzle.Coordinate(x = x, y = j))
                } while (!hasBorder(grid, x, j++, Puzzle.BorderDirection.BOTTOM, useBorders = true))
                if (clueNumber != null && downClues.containsKey(clueNumber)) {
                    downPuzzleClues.add(
                        Puzzle.Clue(1000 + clueNumber, "$clueNumber", downClues[clueNumber]!!)
                    )
                    words.add(Puzzle.Word(1000 + clueNumber, word))
                }
            }
        }

        val acrossTitle = if (hasHtmlClues) "<b>Across</b>" else "Across"
        val downTitle = if (hasHtmlClues) "<b>Down</b>" else "Down"
        return Puzzle(
            title,
            creator,
            copyright,
            description,
            grid.mapIndexed { y, row ->
                row.mapIndexed { x, cell ->
                    val number = gridNumbers[x to y]
                    if (number == null) {
                        cell
                    } else {
                        cell.copy(number = number.toString())
                    }
                }
            },
            listOf(Puzzle.ClueList(acrossTitle, acrossPuzzleClues), Puzzle.ClueList(downTitle, downPuzzleClues)),
            words.sortedBy { it.id },
            hasHtmlClues = hasHtmlClues,
        )
    }

    /**
     * Render this crossword as a PDF document.
     *
     * @param fontFamily Font family to use for the PDF.
     * @param blackSquareLightnessAdjustment Percentage (from 0 to 1) indicating how much to brighten black/colored
     *                                       squares (i.e. to save ink). 0 indicates no adjustment; 1 would be fully
     *                                       white.
     */
    suspend fun asPdf(
        fontFamily: PdfFontFamily = FONT_FAMILY_TIMES_ROMAN,
        blackSquareLightnessAdjustment: Float = 0f
    ): ByteArray = asPuzzle().asPdf(fontFamily, blackSquareLightnessAdjustment)

    companion object {
        /**
         * Execute the given function for each cell in the grid.
         *
         * @param useBorders whether to treat borders between cells as word boundaries when numbering the grid. Defaults
         *                   to true, but may be set to false for formats which do not support borders.
         */
        fun forEachCell(
            grid: List<List<Puzzle.Cell>>,
            useBorders: Boolean = true,
            fn: (
                x: Int,
                y: Int,
                clueNumber: Int?,
                isAcross: Boolean,
                isDown: Boolean,
                cell: Puzzle.Cell,
            ) -> Unit,
        ) {
            var currentClueNumber = 1
            for (y in grid.indices) {
                for (x in grid[y].indices) {
                    if (grid[y][x].cellType.isBlack()) {
                        fn(
                            x, y, /* clueNumber= */ null, /* isAcross= */ false, /* isDown= */ false,
                            grid[y][x]
                        )
                    } else {
                        val isAcross = needsAcrossNumber(grid, x, y, useBorders)
                        val isDown = needsDownNumber(grid, x, y, useBorders)
                        val clueNumber =
                            if (isAcross || isDown) {
                                currentClueNumber++
                            } else {
                                null
                            }
                        fn(x, y, clueNumber, isAcross, isDown, grid[y][x])
                    }
                }
            }
        }

        /** Execute the given function for each numbered cell in the given grid. */
        fun forEachNumberedCell(
            grid: List<List<Puzzle.Cell>>,
            useBorders: Boolean = true,
            fn: (
                x: Int,
                y: Int,
                clueNumber: Int,
                isAcross: Boolean,
                isDown: Boolean,
            ) -> Unit
        ) {
            forEachCell(grid, useBorders) { x, y, clueNumber, isAcross, isDown, _ ->
                if (isAcross || isDown) {
                    fn(x, y, clueNumber!!, isAcross, isDown)
                }
            }
        }

        private fun needsAcrossNumber(grid: List<List<Puzzle.Cell>>, x: Int, y: Int, useBorders: Boolean): Boolean =
            !grid[y][x].cellType.isBlack()
                    && hasBorder(grid, x, y, Puzzle.BorderDirection.LEFT, useBorders)
                    && !hasBorder(grid, x, y, Puzzle.BorderDirection.RIGHT, useBorders)

        private fun needsDownNumber(grid: List<List<Puzzle.Cell>>, x: Int, y: Int, useBorders: Boolean): Boolean =
            !grid[y][x].cellType.isBlack()
                    && hasBorder(grid, x, y, Puzzle.BorderDirection.TOP, useBorders)
                    && !hasBorder(grid, x, y, Puzzle.BorderDirection.BOTTOM, useBorders)

        private fun hasBorder(
            grid: List<List<Puzzle.Cell>>, x: Int, y: Int, direction: Puzzle.BorderDirection, useBorders: Boolean
        ): Boolean {
            if (useBorders && grid[y][x].borderDirections.contains(direction)) return true
            val (borderCellX, borderCellY) = when (direction) {
                Puzzle.BorderDirection.TOP -> x to y - 1
                Puzzle.BorderDirection.BOTTOM -> x to y + 1
                Puzzle.BorderDirection.LEFT -> x - 1 to y
                Puzzle.BorderDirection.RIGHT -> x + 1 to y
            }
            if (borderCellY !in grid.indices || borderCellX !in grid[borderCellY].indices) {
                return true
            }
            val borderCell = grid[borderCellY][borderCellX]
            val oppositeDirection = when (direction) {
                Puzzle.BorderDirection.TOP -> Puzzle.BorderDirection.BOTTOM
                Puzzle.BorderDirection.BOTTOM -> Puzzle.BorderDirection.TOP
                Puzzle.BorderDirection.LEFT -> Puzzle.BorderDirection.RIGHT
                Puzzle.BorderDirection.RIGHT -> Puzzle.BorderDirection.LEFT
            }
            return borderCell.cellType.isBlack() ||
                    (useBorders && borderCell.borderDirections.contains(oppositeDirection))
        }
    }
}
