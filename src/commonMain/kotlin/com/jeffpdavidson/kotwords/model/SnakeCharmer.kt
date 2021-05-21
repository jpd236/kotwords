package com.jeffpdavidson.kotwords.model

data class SnakeCharmer(
    val title: String,
    val creator: String,
    val copyright: String,
    val description: String,
    val answers: List<String>,
    val clues: List<String>,
    val gridCoordinates: List<Pair<Int, Int>>
) {

    init {
        require(gridCoordinates.isNotEmpty()) {
            "Cannot have an empty grid"
        }
        require(gridCoordinates.size * 2 == answers.sumOf { it.length }) {
            "Grid size must be exactly half the length of all the answers"
        }
    }

    fun asPuzzle(crosswordSolverSettings: Puzzle.CrosswordSolverSettings): Puzzle {
        val cellNumbersMap = mutableMapOf<Pair<Int, Int>, MutableList<Int>>()
        val solutionMap = mutableMapOf<Pair<Int, Int>, Char>()
        val words = mutableListOf<MutableList<Pair<Int, Int>>>()
        answers.fold(0 to 1) { (i, clueNumber), answer ->
            cellNumbersMap.getOrPut(gridCoordinates[i % gridCoordinates.size]) { mutableListOf() }.add(clueNumber)
            val word = mutableListOf<Pair<Int, Int>>()
            answer.forEachIndexed { j, ch ->
                val coordinates = gridCoordinates[(i + j) % gridCoordinates.size]
                val cellAnswer = solutionMap.getOrPut(coordinates) { ch }
                require(cellAnswer == ch) {
                    "Conflict at cell (${coordinates.first}, ${coordinates.second}) for answer $clueNumber " +
                            "($answer): $cellAnswer from previous answer does not match $ch from this answer."
                }
                word += coordinates
            }
            words.add(word)
            i + answer.length to clueNumber + 1
        }
        val width = gridCoordinates.maxByOrNull { it.first }!!.first + 1
        val height = gridCoordinates.maxByOrNull { it.second }!!.second + 1
        val grid = (0 until height).map { y ->
            (0 until width).map { x ->
                if (!solutionMap.containsKey(x to y)) {
                    Puzzle.Cell(x = x + 1, y = y + 1, cellType = Puzzle.CellType.BLOCK)
                } else {
                    val cellNumbers: List<Int> = cellNumbersMap[x to y] ?: listOf()
                    Puzzle.Cell(
                        x = x + 1,
                        y = y + 1,
                        solution = "${solutionMap[x to y]!!}",
                        number = if (cellNumbers.isEmpty()) "" else "${cellNumbers[0]}",
                        topRightNumber = if (cellNumbers.size <= 1) "" else "${cellNumbers[1]}"
                    )
                }
            }
        }
        val (jpzClues, jpzWords) = clues.mapIndexed { i, clue ->
            Puzzle.Clue(i + 1, "${i + 1}", clue) to Puzzle.Word(i + 1, words[i].map { (x, y) -> grid[y][x] })
        }.unzip()
        return Puzzle(
            title = title,
            creator = creator,
            copyright = copyright,
            description = description,
            grid = grid,
            clues = listOf(Puzzle.ClueList("Clues", jpzClues)),
            words = jpzWords,
            crosswordSolverSettings = crosswordSolverSettings
        )
    }
}