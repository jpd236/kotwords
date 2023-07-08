package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.formats.Puzzleable

data class Patchwork(
    val title: String,
    val creator: String,
    val copyright: String,
    val description: String,
    val grid: List<List<Char>>,
    val rowClues: List<List<String>>,
    val pieceClues: List<String>,
    val pieceNumbers: List<List<Int>>,
    val labelPieces: Boolean,
) : Puzzleable() {
    init {
        require(rowClues.size == grid.size) {
            "Grid has height ${grid.size} but has ${rowClues.size} row clue sets"
        }

        require((1..pieceClues.size).toSet() == pieceNumbers.flatten().toSet()) {
            "Piece numbers do not cover range from 1 to ${pieceClues.size} piece clues"
        }

        require(grid.size == pieceNumbers.size) {
            "Have ${grid.size} rows in grid but ${pieceNumbers.size} rows of piece numbers"
        }

        require(grid.zip(pieceNumbers).all { (gridRow, pieceNumbersRow) -> gridRow.size == pieceNumbersRow.size }) {
            "Width of grid and piece numbers do not match"
        }
    }

    override suspend fun createPuzzle(): Puzzle {
        val words = mutableMapOf<Int, MutableList<Puzzle.Coordinate>>()
        val puzzleGrid = mutableListOf<List<Puzzle.Cell>>()
        grid.forEachIndexed { y, row ->
            val puzzleRow = mutableListOf<Puzzle.Cell>()
            row.forEachIndexed { x, ch ->
                val pieceNumber = pieceNumbers[y][x]
                val word = words.getOrPut(pieceNumber) { mutableListOf<Puzzle.Coordinate>() }
                word.add(Puzzle.Coordinate(x = x, y = y))
                puzzleRow.add(
                    Puzzle.Cell(
                        solution = ch.toString(),
                        number = if (x == 0) ('A' + y).toString() else "",
                        topRightNumber = if (word.size == 1) pieceNumber.toString() else "",
                        // Draw a border between any two cells belonging to different pieces.
                        borderDirections = listOf(
                            (x to y - 1) to Puzzle.BorderDirection.TOP,
                            (x + 1 to y) to Puzzle.BorderDirection.RIGHT,
                            (x to y + 1) to Puzzle.BorderDirection.BOTTOM,
                            (x - 1 to y) to Puzzle.BorderDirection.LEFT,
                        ).mapNotNull { (point, borderDirection) ->
                            var (i, j) = point
                            if (
                                j in pieceNumbers.indices && i in pieceNumbers[j].indices &&
                                pieceNumber != pieceNumbers[j][i]
                            ) {
                                borderDirection
                            } else {
                                null
                            }
                        }.toSet()
                    )
                )
            }
            puzzleGrid.add(puzzleRow)
        }
        val (rowClueList, rowWordList) = rowClues.mapIndexed { y, clues ->
            val cells = puzzleGrid[y].indices.map { x -> Puzzle.Coordinate(x = x, y = y) }
            Puzzle.Clue(y + 1, "${'A' + y}", clues.joinToString(" / ")) to Puzzle.Word(y + 1, cells)
        }.unzip()
        val (pieceClueList, pieceWordList) = pieceClues.mapIndexed { i, clue ->
            val puzzleClue = Puzzle.Clue(
                wordId = 1000 + i + 1,
                number = if (labelPieces) (i + 1).toString() else "",
                text = clue,
            )
            puzzleClue to Puzzle.Word(1000 + i + 1, words[i + 1]!!)
        }.unzip()
        return Puzzle(
            title = title,
            creator = creator,
            copyright = copyright,
            description = description,
            grid = puzzleGrid,
            clues = listOf(
                Puzzle.ClueList("Rows", rowClueList),
                Puzzle.ClueList("Pieces", if (labelPieces) pieceClueList else pieceClueList.sortedBy { it.text })
            ),
            words = rowWordList + if (labelPieces) pieceWordList else listOf(),
        )
    }
}