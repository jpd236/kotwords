package com.jeffpdavidson.kotwords.model

import kotlin.math.sqrt

data class SpellWeaving(
    val title: String,
    val creator: String,
    val copyright: String,
    val description: String,
    val answers: List<String>,
    val clues: List<String>
) {

    fun asPuzzle(crosswordSolverSettings: Puzzle.CrosswordSolverSettings): Puzzle {
        val filteredAnswers = answers.map { answer -> answer.toUpperCase().filter { it in 'A'..'Z' } }
        val length = filteredAnswers.sumBy { it.length }
        val middleRowLengthDouble = (1 + sqrt(4 * length - 3.0)) / 2
        val middleRowLength = middleRowLengthDouble.toInt()
        require(middleRowLength > 2 && middleRowLength % 2 == 0 && middleRowLengthDouble - middleRowLength == 0.0) {
            "Answers do not fit into a standard Spell Weaving grid."
        }

        val words = mutableListOf<List<Pair<Int, Int>>>()
        var position = Position(x = 0, y = middleRowLength / 2 - 1, direction = Direction.RIGHT)
        val gridMap = mutableMapOf<Pair<Int, Int>, CellState>()
        filteredAnswers.forEachIndexed { answerIndex, answer ->
            val word = mutableListOf<Pair<Int, Int>>()
            answer.forEachIndexed { i, ch ->
                val cellState = gridMap.getOrPut(position.x to position.y) { CellState(solution = "$ch") }
                require(cellState.solution == "$ch") {
                    "Conflict at cell (${position.x}, ${position.y}) for answer $answerIndex ($answer): " +
                            "${cellState.solution} from previous answer does not match $ch from this answer."
                }
                word.add(position.x to position.y)
                // Add clue number for the first letter of each answer.
                if (i == 0) {
                    cellState.numbers.add("${answerIndex + 1}")
                }
                position = getNextPosition(position, middleRowLength)
                // Add borders after last letter of each answer (except the final answer).
                if (answerIndex < answers.size - 1 && i == answer.length - 1) {
                    cellState.borderDirections.add(
                        when (position.direction) {
                            Direction.UP -> Puzzle.BorderDirection.TOP
                            Direction.DOWN -> Puzzle.BorderDirection.BOTTOM
                            Direction.RIGHT -> Puzzle.BorderDirection.RIGHT
                            Direction.LEFT -> Puzzle.BorderDirection.LEFT
                        }
                    )
                }
            }
            words.add(word)
        }
        val grid = (0 until middleRowLength).map { y ->
            (0 until middleRowLength).map { x ->
                val cell = gridMap[x to y]
                if (cell == null) {
                    Puzzle.Cell(x = x + 1, y = y + 1, cellType = Puzzle.CellType.VOID)
                } else {
                    require(cell.numbers.size <= 2) {
                        "More than 2 entries start at position ($x, $y) which is not supported by JPZ files."
                    }
                    Puzzle.Cell(
                        x = x + 1, y = y + 1,
                        solution = cell.solution,
                        number = if (cell.numbers.isNotEmpty()) cell.numbers[0] else "",
                        topRightNumber = if (cell.numbers.size > 1) cell.numbers[1] else "",
                        borderDirections = cell.borderDirections
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

    private enum class Direction {
        UP,
        DOWN,
        RIGHT,
        LEFT
    }

    private data class Position(val x: Int, val y: Int, val direction: Direction)

    private data class CellState(
        var solution: String,
        var numbers: MutableList<String> = mutableListOf(),
        val borderDirections: MutableSet<Puzzle.BorderDirection> = mutableSetOf()
    )

    companion object {
        private fun getNextPosition(position: Position, middleRowLength: Int): Position {
            when (position.direction) {
                Direction.UP -> {
                    if (isPointInGrid(position.x, position.y - 1, middleRowLength)) {
                        return position.copy(y = position.y - 1)
                    }
                    return position.copy(x = position.x + 1, direction = Direction.RIGHT)
                }
                Direction.DOWN -> {
                    if (isPointInGrid(position.x, position.y + 1, middleRowLength)) {
                        return position.copy(y = position.y + 1)
                    }
                    return position.copy(x = position.x - 1, direction = Direction.LEFT)
                }
                Direction.RIGHT -> {
                    if (isPointInGrid(position.x + 1, position.y, middleRowLength)) {
                        return position.copy(x = position.x + 1)
                    }
                    return position.copy(y = position.y + 1, direction = Direction.DOWN)
                }
                Direction.LEFT -> {
                    if (isPointInGrid(position.x - 1, position.y, middleRowLength)) {
                        return position.copy(x = position.x - 1)
                    }
                    return position.copy(y = position.y - 1, direction = Direction.UP)
                }
            }
        }

        private fun isPointInGrid(x: Int, y: Int, middleRowLength: Int): Boolean {
            return if (y < middleRowLength / 2) {
                // Must be in the center 2 * (y + 1) squares
                val range = 2 * (y + 1)
                x - ((middleRowLength - range) / 2) in 0 until range
            } else {
                // Must be in the center 2 * (middleRowLength - y) - 1 squares, shifted 1 right
                val range = 2 * (middleRowLength - y) - 1
                x - ((middleRowLength - range) / 2) - 1 in 0 until range
            }
        }
    }
}