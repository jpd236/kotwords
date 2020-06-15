package com.jeffpdavidson.kotwords.model

import kotlin.math.abs

data class Labyrinth(
        val title: String,
        val creator: String,
        val copyright: String,
        val description: String,
        val grid: List<List<Char>>,
        val gridKey: List<List<Int>>,
        val rowClues: List<List<String>>,
        val windingClues: List<String>
) {

    init {
        val allNumbers = gridKey.flatten().sorted()
        require((1..allNumbers.size).toList() == allNumbers) {
            "Grid key must contain exactly one of each number from 1 to the size of the grid"
        }
        require(grid.map { it.size }.toList() == gridKey.map { it.size }.toList()) {
            "Grid key and grid must have the same dimensions"
        }
    }

    fun asPuzzle(
            alphabetizeWindingClues: Boolean,
            crosswordSolverSettings: Puzzle.CrosswordSolverSettings
    ): Puzzle {
        val puzzleGrid = grid.mapIndexed { y, row ->
            row.mapIndexed { x, ch ->
                // Calculate the borders. We remove borders from the outer edges of the grid as well as between any two
                // neighboring cells in the winding path.
                val borderDirections = mutableSetOf(
                        Puzzle.BorderDirection.TOP,
                        Puzzle.BorderDirection.BOTTOM,
                        Puzzle.BorderDirection.LEFT,
                        Puzzle.BorderDirection.RIGHT
                )
                if (y == 0 || (y > 0 && abs(gridKey[y - 1][x] - gridKey[y][x]) == 1)) {
                    borderDirections -= Puzzle.BorderDirection.TOP
                }
                if (y == grid.size - 1 || (y < grid.size - 1 && abs(gridKey[y + 1][x] - gridKey[y][x]) == 1)) {
                    borderDirections -= Puzzle.BorderDirection.BOTTOM
                }
                if (x == 0 || (x > 0 && abs(gridKey[y][x - 1] - gridKey[y][x]) == 1)) {
                    borderDirections -= Puzzle.BorderDirection.LEFT
                }
                if (x == grid[y].size - 1 || (x < grid[y].size - 1 && abs(gridKey[y][x + 1] - gridKey[y][x]) == 1)) {
                    borderDirections -= Puzzle.BorderDirection.RIGHT
                }
                Puzzle.Cell(
                        x = x + 1,
                        y = y + 1,
                        solution = "$ch",
                        borderDirections = borderDirections,
                        number = if (x == 0) "${y + 1}" else ""
                )
            }
        }

        val windingPath = gridKey.mapIndexed { y, row ->
            row.mapIndexed { x, i ->
                i to puzzleGrid[y][x]
            }
        }.flatten().sortedBy { it.first }.map { it.second }
        val windingClueList = if (alphabetizeWindingClues) windingClues.sorted() else windingClues
        val windingClue = Puzzle.Clue(Puzzle.Word(101, windingPath), "1", windingClueList.joinToString(" / "))

        val rowPuzzleClues = puzzleGrid.mapIndexed { y, row ->
            Puzzle.Clue(Puzzle.Word(y + 1, row), "${y + 1}", rowClues[y].joinToString(" / "))
        }

        return Puzzle(
                title = title,
                creator = creator,
                copyright = copyright,
                description = description,
                grid = puzzleGrid,
                clues = listOf(
                        Puzzle.ClueList("Rows", rowPuzzleClues),
                        Puzzle.ClueList("Winding", listOf(windingClue))
                ),
                crosswordSolverSettings = crosswordSolverSettings
        )
    }
}