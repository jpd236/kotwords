package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.formats.Puzzleable
import kotlin.math.abs

data class Labyrinth(
    val title: String,
    val creator: String,
    val copyright: String,
    val description: String,
    val grid: List<List<Char>>,
    val gridKey: List<List<Int>>,
    val rowClues: List<List<String>>,
    val windingClues: List<String>,
    val alphabetizeWindingClues: Boolean,
) : Puzzleable() {

    init {
        val allNumbers = gridKey.flatten().sorted()
        require((1..allNumbers.size).toList() == allNumbers) {
            "Grid key must contain exactly one of each number from 1 to the size of the grid"
        }
        require(grid.map { it.size }.toList() == gridKey.map { it.size }.toList()) {
            "Grid key and grid must have the same dimensions"
        }
    }

    override suspend fun createPuzzle(): Puzzle {
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
                    solution = "$ch",
                    borderDirections = borderDirections,
                    number = if (x == 0) "${y + 1}" else ""
                )
            }
        }

        val windingPath = gridKey.mapIndexed { y, row ->
            row.mapIndexed { x, i ->
                i to Puzzle.Coordinate(x = x, y = y)
            }
        }.flatten().sortedBy { it.first }.map { it.second }
        val windingClueList = if (alphabetizeWindingClues) windingClues.sorted() else windingClues
        val windingWord = Puzzle.Word(101, windingPath)
        val windingClue = Puzzle.Clue(101, "1", windingClueList.joinToString(" / "))

        val (rowPuzzleClues, rowPuzzleWords) = puzzleGrid.mapIndexed { y, row ->
            val clue = Puzzle.Clue(y + 1, "${y + 1}", rowClues[y].joinToString(" / "))
            val word = Puzzle.Word(y + 1, row.indices.map { x -> Puzzle.Coordinate(x = x, y = y) })
            clue to word
        }.unzip()

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
            words = rowPuzzleWords + windingWord,
        )
    }
}