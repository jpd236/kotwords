package com.jeffpdavidson.kotwords.model

data class AroundTheBend(
        val title: String,
        val creator: String,
        val copyright: String,
        val description: String,
        var rows: List<String>,
        val clues: List<String>
) {
    fun asPuzzle(crosswordSolverSettings: Puzzle.CrosswordSolverSettings): Puzzle {
        val maxWidth = rows.maxByOrNull { it.length }!!.length
        val grid = rows.mapIndexed { y, row ->
            val padding = maxWidth - row.length
            (0 until padding).map { x ->
                Puzzle.Cell(x = x + 1, y = y + 1, cellType = Puzzle.CellType.BLOCK)
            } + row.mapIndexed { i, ch ->
                Puzzle.Cell(
                        x = padding + i + 1,
                        y = y + 1,
                        solution = "$ch",
                        number = if (i == 0) "${y + 1}" else "")
            }
        }
        val puzzleClues = clues.mapIndexed { y, clue ->
            Puzzle.Clue(
                    Puzzle.Word(y,
                            grid[y].filterNot { it.cellType == Puzzle.CellType.BLOCK } +
                                    grid[(y + 1) % grid.size].filterNot { it.cellType == Puzzle.CellType.BLOCK }.reversed()),
                    "${y + 1}",
                    clue)
        }
        return Puzzle(
                title = title,
                creator = creator,
                copyright = copyright,
                description = description,
                grid = grid,
                clues = listOf(Puzzle.ClueList("Clues", puzzleClues)),
                crosswordSolverSettings = crosswordSolverSettings)
    }
}