package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.formats.Puzzleable

data class Crosswordle(
    val title: String,
    val creator: String,
    val copyright: String,
    val description: String,
    val grid: List<List<Char>>,
    val answer: String,
    val acrossClues: List<String>,
    val downClues: List<String>,
) : Puzzleable {

    private enum class CellStatus(val bgColor: String) {
        WHITE(""),
        YELLOW("#c9b458"),
        GREEN("#6aaa64"),
    }

    init {
        require(grid.isNotEmpty() && grid.all { it.size == grid[0].size }) {
            "All rows of the grid must have the same length"
        }
        require(grid[0].size == answer.length) {
            "Answer must have the same length as the grids"
        }
        require(grid.size == acrossClues.size) {
            "Have ${grid.size} rows but ${acrossClues.size} across clues"
        }
        require(grid[0].size == downClues.size) {
            "Have ${grid[0].size} columns but ${downClues.size} down clues"
        }
    }

    override suspend fun asPuzzle(): Puzzle {
        val cellStatuses = grid.map { row ->
            val isUsed = row.mapIndexed { x, ch -> answer[x] == ch }.toMutableList()
            row.mapIndexed { x, ch ->
                if (answer[x] == ch) {
                    CellStatus.GREEN
                } else {
                    val firstMatch = isUsed.withIndex().firstOrNull { (i, used) ->
                        answer[i] == ch && !used
                    }?.index ?: -1
                    if (firstMatch != -1) {
                        isUsed[firstMatch] = true
                        CellStatus.YELLOW
                    } else {
                        CellStatus.WHITE
                    }
                }
            }
        }

        val puzzleGrid = grid.mapIndexed { y, row ->
            row.mapIndexed { x, ch ->
                Puzzle.Cell(solution = "$ch", backgroundColor = cellStatuses[y][x].bgColor)
            }.toMutableList()
        } + listOf(
            answer.map { ch ->
                Puzzle.Cell(
                    solution = "$ch",
                    backgroundColor = CellStatus.GREEN.bgColor,
                    borderDirections = setOf(Puzzle.BorderDirection.TOP),
                )
            }.toMutableList()
        )

        Crossword.forEachCell(puzzleGrid) { x, y, clueNumber, _, _, cell ->
            if (clueNumber != null) {
                puzzleGrid[y][x] = cell.copy(number = clueNumber.toString())
            }
        }

        val clues = listOf(
            Puzzle.ClueList(
                title = "Across",
                clues = puzzleGrid.indices.map { y ->
                    Puzzle.Clue(
                        wordId = y + 1,
                        number = (if (y == 0) 1 else y + grid[0].size).toString(),
                        text = if (y in acrossClues.indices) acrossClues[y] else "-"
                    )
                }
            ),
            Puzzle.ClueList(
                title = "Down",
                clues = puzzleGrid[0].indices.map { x ->
                    Puzzle.Clue(
                        wordId = 1000 + x + 1,
                        number = (x + 1).toString(),
                        text = downClues[x]
                    )
                }
            ),
        )

        val words = puzzleGrid.mapIndexed { y, row ->
            Puzzle.Word(id = y + 1, cells = row.indices.map { x -> Puzzle.Coordinate(x, y) })
        } + puzzleGrid[0].indices.map { x ->
            Puzzle.Word(
                id = 1000 + x + 1,
                cells = puzzleGrid.dropLast(1).indices.map { y -> Puzzle.Coordinate(x, y) },
            )
        }

        return Puzzle(
            title = title,
            creator = creator,
            copyright = copyright,
            description = description,
            grid = puzzleGrid,
            clues = clues,
            words = words,
        )
    }
}