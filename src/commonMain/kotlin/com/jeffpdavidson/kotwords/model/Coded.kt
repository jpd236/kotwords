package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.formats.Puzzleable

data class Coded(
    val title: String,
    val creator: String,
    val copyright: String,
    val description: String,
    val grid: List<List<Char?>>,
    val assignments: List<Char>,
    val givens: List<Char>,
) : Puzzleable {
    init {
        val gridLetters = grid.flatMap { it.filterNotNull() }.distinct().toSet()
        require(gridLetters == assignments.toSet()) {
            "Set of characters in the grid does not match the set of characters in the assignments"
        }
    }

    override suspend fun asPuzzle(): Puzzle {
        val assignmentMap = assignments.mapIndexed { i, ch -> ch to (i + 1) }.toMap()
        val puzzleGrid = grid.map { row ->
            row.map { ch ->
                if (ch == null) {
                    Puzzle.Cell(cellType = Puzzle.CellType.BLOCK)
                } else {
                    val given = givens.contains(ch)
                    Puzzle.Cell(
                        solution = "$ch",
                        number = "${assignmentMap[ch]}",
                        hint = given,
                        entry = if (given) "$ch" else "",
                    )
                }
            }
        }

        val words = mutableListOf<Puzzle.Word>()
        var acrossWordNumber = 1
        var downWordNumber = 1
        Crossword.forEachCell(puzzleGrid) { x, y, _, isAcross, isDown, _ ->
            if (isAcross) {
                val word = mutableListOf<Puzzle.Coordinate>()
                var i = x
                while (i < puzzleGrid[y].size && !puzzleGrid[y][i].cellType.isBlack()) {
                    word.add(Puzzle.Coordinate(x = i, y = y))
                    i++
                }
                words.add(Puzzle.Word(acrossWordNumber++, word))
            }
            if (isDown) {
                val word = mutableListOf<Puzzle.Coordinate>()
                var j = y
                while (j < puzzleGrid.size && !puzzleGrid[j][x].cellType.isBlack()) {
                    word.add(Puzzle.Coordinate(x = x, y = j))
                    j++
                }
                words.add(Puzzle.Word(1000 + downWordNumber++, word))
            }
        }

        return Puzzle(
            title = title,
            creator = creator,
            copyright = copyright,
            description = description,
            puzzleType = Puzzle.PuzzleType.CODED,
            grid = puzzleGrid,
            clues = listOf(),
            words = words.sortedBy { it.id }
        )
    }

    companion object {
        fun fromRawInput(
            title: String,
            creator: String,
            copyright: String,
            description: String,
            grid: String,
            assignments: String,
            givens: String,
        ): Coded {
            val gridChars = grid.trim().lines().map { line ->
                line.trim().map { ch -> if (ch == '.') null else ch }
            }
            val assignmentList =
                if (assignments.isBlank()) {
                    generateAssignments(gridChars)
                } else {
                    assignments.trim().toList()
                }
            return Coded(
                title = title.trim(),
                creator = creator.trim(),
                copyright = copyright.trim(),
                description = description.trim(),
                grid = gridChars,
                assignments = assignmentList,
                givens = givens.trim().toList(),
            )
        }

        fun generateAssignments(grid: List<List<Char?>>): List<Char> =
            grid.flatMap { it.filterNotNull() }.distinct().shuffled()
    }
}