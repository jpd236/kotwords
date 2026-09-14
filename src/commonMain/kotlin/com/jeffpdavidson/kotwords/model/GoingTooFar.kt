package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.formats.Puzzleable

data class GoingTooFar(
    val crossword: Crossword,
    val quote: String,
    val shadedSquareColor: String = "#C0C0C0",
) : Puzzleable() {

    private val cleanQuote = quote.uppercase().filter { it in 'A'..'Z' || it in '0'..'9' }

    init {
        val blackSquareCount = crossword.grid.flatten().count { it.cellType.isBlack() }
        require(cleanQuote.length == blackSquareCount) {
            "Quote length must match the number of black squares ($blackSquareCount), but was ${cleanQuote.length}"
        }
    }

    override suspend fun createPuzzle(): Puzzle {
        val puzzle = crossword.asPuzzle()
        val quoteByCoordinate = puzzle.grid.flatMapIndexed { y, row ->
            row.mapIndexedNotNull { x, cell ->
                if (cell.cellType.isBlack()) x to y else null
            }
        }.zip(cleanQuote.toList()).toMap()

        return puzzle.copy(grid = puzzle.grid.mapIndexed { y, row ->
            row.mapIndexed { x, cell ->
                val solution = quoteByCoordinate[x to y]
                if (solution != null) {
                    Puzzle.Cell(
                        cellType = Puzzle.CellType.REGULAR,
                        solution = "$solution",
                        backgroundColor = shadedSquareColor,
                    )
                } else {
                    cell
                }
            }
        })
    }
}
