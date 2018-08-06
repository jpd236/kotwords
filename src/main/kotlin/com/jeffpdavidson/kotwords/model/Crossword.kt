package com.jeffpdavidson.kotwords.model

import java.nio.charset.Charset
import java.nio.charset.CharsetEncoder

private val WINDOWS_1252 = Charset.forName("windows-1252")

/**
 * A representation of a crossword puzzle.
 *
 * While this is a generic container, it only includes features that are supported by Across Lite.
 * All string data must be encodable in the Windows-1252 charset.
 *
 * @constructor Construct a new puzzle.
 * @param title The title of the puzzle.
 * @param author The author of the puzzle.
 * @param copyright The copyright of the puzzle.
 * @param notes Optional notes about the puzzle.
 * @param grid The grid of {@link Square}s in the form of a list of rows going from top to bottom,
 *             with each row going from left to right.
 * @param acrossClues Mapping from across clue number to the clue for that number.
 * @param downClues Mapping from down clue number to the clue for that number.
 */
data class Crossword(
        val title: String,
        val author: String,
        val copyright: String,
        val notes: String = "",
        val grid: List<List<Square>>,
        val acrossClues: Map<Int, String>,
        val downClues: Map<Int, String>) {
    init {
        // Validate that grid is a rectangle.
        val width = grid[0].size
        grid.forEachIndexed { index, row ->
            require(row.size == width) {
                "Invalid grid - row $index has width ${row.size} but should be $width"
            }
        }

        // Validate that all strings are encodable.
        val encoder = WINDOWS_1252.newEncoder()
        requireEncodableString(encoder, "title", title)
        requireEncodableString(encoder, "author", author)
        requireEncodableString(encoder, "copyright", copyright)
        requireEncodableString(encoder, "notes", notes)
        acrossClues.forEach { clueNum, clue ->
            requireEncodableString(encoder, "Across clue $clueNum", clue)
        }
        downClues.forEach { clueNum, clue ->
            requireEncodableString(encoder, "Down clue $clueNum", clue)
        }

        // TODO: Validate standard grid numbering / clues.
    }

    private fun requireEncodableString(encoder: CharsetEncoder, name: String, value: String) {
        require(encoder.canEncode(value)) {
            "Provided string for $name cannot be encoded to Windows-1252: $value"
        }
    }

    companion object {
        /** Execute the given function for each square in the grid. */
        fun forEachSquare(grid: List<List<Square>>,
                          fn: (x: Int,
                               y: Int,
                               clueNumber: Int?,
                               isAcross: Boolean,
                               isDown: Boolean,
                               square: Square) -> Unit) {
            var currentClueNumber = 1
            for (y in 0 until grid.size) {
                for (x in 0 until grid[y].size) {
                    if (grid[y][x].isBlack) {
                        fn(x, y, /* clueNumber= */ null, /* isAcross= */ false, /* isDown= */ false,
                                grid[y][x])
                    } else {
                        val isAcross = needsAcrossNumber(grid, x, y)
                        val isDown = needsDownNumber(grid, x, y)
                        val clueNumber =
                                if (isAcross || isDown) { currentClueNumber++ } else { null }
                        fn(x, y, clueNumber, isAcross, isDown, grid[y][x])
                    }
                }
            }
        }

        /** Execute the given function for each numbered square in the given grid. */
        fun forEachNumberedSquare(grid: List<List<Square>>,
                                  fn: (x: Int,
                                       y: Int,
                                       clueNumber: Int,
                                       isAcross: Boolean,
                                       isDown: Boolean) -> Unit) {
            forEachSquare(grid) { x, y, clueNumber, isAcross, isDown, _ ->
                if (isAcross || isDown) {
                    fn(x, y, clueNumber!!, isAcross, isDown)
                }
            }
        }

        private fun needsAcrossNumber(grid: List<List<Square>>, x: Int, y: Int): Boolean {
            return !grid[y][x].isBlack && (x == 0 || grid[y][x - 1].isBlack)
                    && (x + 1 < grid[0].size && !grid[y][x + 1].isBlack)
        }

        private fun needsDownNumber(grid: List<List<Square>>, x: Int, y: Int): Boolean {
            return !grid[y][x].isBlack && (y == 0 || grid[y - 1][x].isBlack)
                    && (y + 1 < grid.size && !grid[y + 1][x].isBlack)
        }
    }
}
