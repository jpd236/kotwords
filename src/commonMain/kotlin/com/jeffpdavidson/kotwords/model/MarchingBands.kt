package com.jeffpdavidson.kotwords.model

data class MarchingBands(
        val title: String,
        val creator: String,
        val copyright: String,
        val description: String,
        val grid: List<List<Char?>>,
        val bandClues: List<List<String>>,
        val rowClues: List<List<String>>
) {
    init {
        val height = grid.size
        require(grid.count { it.size == height } == height) {
            "Only square grids are supported, but at least one grid row has a width != $height"
        }
        require(rowClues.size == height) {
            "Grid has height $height but has ${rowClues.size} row clue sets"
        }
        val bandCount = height / 2
        require(bandClues.size == bandCount) {
            "Grid should have $bandCount bands but has ${bandClues.size} band clue sets"
        }
    }

    fun asPuzzle(
            includeRowNumbers: Boolean,
            lightBandColor: String,
            darkBandColor: String,
            crosswordSolverSettings: Puzzle.CrosswordSolverSettings
    ): Puzzle {
        val puzzleGrid = grid.mapIndexed { y, row ->
            row.mapIndexed { x, ch ->
                if (ch == null) {
                    Puzzle.Cell(x = x + 1, y = y + 1, cellType = Puzzle.CellType.BLOCK)
                } else {
                    val rowNumber = if (x == 0) "${y + 1}" else ""
                    val bandLetter = if (x == y && x < grid.size / 2) "${'A' + x}" else ""
                    val backgroundColor =
                            if (listOf(x, y, grid.size - y - 1, grid[y].size - x - 1).minOrNull()!! % 2 == 0) {
                                lightBandColor
                            } else {
                                darkBandColor
                            }
                    Puzzle.Cell(
                            x = x + 1, y = y + 1,
                            solution = "$ch",
                            number = if (includeRowNumbers) rowNumber else bandLetter,
                            topRightNumber = if (includeRowNumbers) bandLetter else "",
                            backgroundColor = backgroundColor
                    )
                }
            }
        }
        val rowClueList = rowClues.mapIndexed { y, clues ->
            val cells = puzzleGrid[y].filterNot { it.cellType == Puzzle.CellType.BLOCK }
            Puzzle.Clue(Puzzle.Word(y + 1, cells), "${y + 1}", clues.joinToString(" / "))
        }
        val bandClueList = bandClues.mapIndexed { i, clues ->
            val cells = mutableListOf<Puzzle.Cell>()
            cells.addAll(puzzleGrid[i].subList(i, puzzleGrid[i].size - i))
            (i + 1 until puzzleGrid.size - i).forEach { y ->
                cells.add(puzzleGrid[y][puzzleGrid[y].size - i - 1])
            }
            cells.addAll(puzzleGrid[puzzleGrid.size - i - 1].subList(i, puzzleGrid[i].size - i).reversed())
            (puzzleGrid.size - i - 1 downTo i + 1).forEach { y ->
                cells.add(puzzleGrid[y][i])
            }
            Puzzle.Clue(Puzzle.Word(1000 + i + 1, cells), "${'A' + i}", clues.joinToString(" / "))
        }
        return Puzzle(
                title = title,
                creator = creator,
                copyright = copyright,
                description = description,
                grid = puzzleGrid,
                clues = listOf(Puzzle.ClueList("Bands", bandClueList), Puzzle.ClueList("Rows", rowClueList)),
                crosswordSolverSettings = crosswordSolverSettings
        )
    }
}