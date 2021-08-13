package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.formats.Puzzleable

data class SnakeCharmer(
    val title: String,
    val creator: String,
    val copyright: String,
    val description: String,
    val answers: List<String>,
    val clues: List<String>,
    val gridCoordinates: List<Pair<Int, Int>>
) : Puzzleable {

    init {
        require(gridCoordinates.isNotEmpty()) {
            "Cannot have an empty grid"
        }
        require(gridCoordinates.size * 2 == answers.sumOf { it.length }) {
            "Grid size must be exactly half the length of all the answers"
        }
    }

    override fun asPuzzle(): Puzzle {
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
                    Puzzle.Cell(cellType = Puzzle.CellType.BLOCK)
                } else {
                    val cellNumbers: List<Int> = cellNumbersMap[x to y] ?: listOf()
                    Puzzle.Cell(
                        solution = "${solutionMap[x to y]!!}",
                        number = if (cellNumbers.isEmpty()) "" else "${cellNumbers[0]}",
                        topRightNumber = if (cellNumbers.size <= 1) "" else "${cellNumbers[1]}"
                    )
                }
            }
        }
        val (puzzleClues, puzzleWords) = clues.mapIndexed { i, clue ->
            val puzzleClue = Puzzle.Clue(i + 1, "${i + 1}", clue)
            val puzzleWord = Puzzle.Word(i + 1, words[i].map { (x, y) -> Puzzle.Coordinate(x = x, y = y) })
            puzzleClue to puzzleWord
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