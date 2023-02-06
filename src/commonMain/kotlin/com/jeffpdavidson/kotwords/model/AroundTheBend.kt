package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.formats.Puzzleable

data class AroundTheBend(
    val title: String,
    val creator: String,
    val copyright: String,
    val description: String,
    var rows: List<String>,
    val clues: List<String>
) : Puzzleable() {
    override suspend fun createPuzzle(): Puzzle {
        val maxWidth = rows.maxByOrNull { it.length }!!.length
        val grid = rows.mapIndexed { y, row ->
            val padding = maxWidth - row.length
            (0 until padding).map {
                Puzzle.Cell(cellType = Puzzle.CellType.BLOCK)
            } + row.mapIndexed { i, ch ->
                Puzzle.Cell(
                    solution = "$ch",
                    number = if (i == 0) "${y + 1}" else ""
                )
            }
        }
        val (puzzleClues, puzzleWords) = clues.mapIndexed { y, clue ->
            val nextY = (y + 1) % grid.size
            Puzzle.Clue(
                y,
                "${y + 1}",
                clue
            ) to Puzzle.Word(
                y,
                grid[y].mapIndexedNotNull { x, cell ->
                    if (cell.cellType == Puzzle.CellType.BLOCK) null else Puzzle.Coordinate(x = x, y = y)
                } + grid[nextY].mapIndexedNotNull { x, cell ->
                    if (cell.cellType == Puzzle.CellType.BLOCK) null else Puzzle.Coordinate(x = x, y = nextY)
                }.reversed()
            )
        }.unzip()
        return Puzzle(
            title = title,
            creator = creator,
            copyright = copyright,
            description = description,
            grid = grid,
            clues = listOf(Puzzle.ClueList("Clues", puzzleClues)),
            words = puzzleWords,
        )
    }
}