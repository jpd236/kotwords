package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.formats.Puzzleable

data class HelterSkelter(
    val title: String,
    val creator: String,
    val copyright: String,
    val description: String,
    val grid: List<List<Char>>,
    val answers: List<String>,
    val clues: List<String>,
    val answerVectors: List<AnswerVector> = listOf(),
    val extendToEdges: Boolean = false,
) : Puzzleable() {

    init {
        require(answers.size == clues.size) {
            "Have ${answers.size} answers but ${clues.size} clues"
        }

        require(answerVectors.isEmpty() || answerVectors.size == answers.size) {
            "Have ${answerVectors.size} answer vectors but ${answers.size} answers"
        }

        require(grid.isNotEmpty() && grid.all { it.size == grid[0].size }) {
            "Grid must be square"
        }
    }

    data class AnswerVector(
        val start: Pair<Int, Int>,
        val direction: Direction,
    ) {
        enum class Direction(internal val dx: Int, internal val dy: Int) {
            NORTH(0, -1),
            NORTHEAST(1, -1),
            EAST(1, 0),
            SOUTHEAST(1, 1),
            SOUTH(0, 1),
            SOUTHWEST(-1, 1),
            WEST(-1, 0),
            NORTHWEST(-1, -1),
        }
    }

    override suspend fun createPuzzle(): Puzzle {
        val vectors = getOrCalculateAnswerVectors()
        val numbers = mutableMapOf<Pair<Int, Int>, String>()
        vectors.forEachIndexed { i, vector ->
            numbers[vector.start] = "${i + 1}"
        }
        return Puzzle(
            title = title,
            creator = creator,
            copyright = copyright,
            description = description,
            grid = grid.mapIndexed { y, row ->
                row.mapIndexed { x, ch ->
                    Puzzle.Cell(
                        solution = "$ch",
                        number = numbers.getOrElse(x to y) { "" }
                    )
                }
            },
            clues = listOf(
                Puzzle.ClueList("Clues", clues.mapIndexed { i, clue ->
                    Puzzle.Clue(wordId = i + 1, number = "${i + 1}", text = clue)
                })
            ),
            words = vectors.mapIndexed { i, vector ->
                val cells = mutableListOf<Puzzle.Coordinate>()
                var x = vector.start.first
                var y = vector.start.second
                do {
                    cells.add(Puzzle.Coordinate(x, y))
                    x += vector.direction.dx
                    y += vector.direction.dy
                } while (y in grid.indices && x in grid[y].indices && (extendToEdges || cells.size < answers[i].length))
                Puzzle.Word(id = i + 1, cells = cells)
            },
        )
    }

    private fun getOrCalculateAnswerVectors(): List<AnswerVector> {
        if (answerVectors.isNotEmpty()) {
            return answerVectors
        }
        val foundVectors = linkedSetOf<AnswerVector>()
        var startingPoints = grid.flatMapIndexed { y, row -> row.indices.map { x -> x to y } }
        answers.forEach { answer ->
            var foundVector: AnswerVector? = null
            startingPoints.forEach { (startX, startY) ->
                AnswerVector.Direction.values().forEach { direction ->
                    val vector = AnswerVector(startX to startY, direction)
                    if (!foundVectors.contains(vector)) {
                        var vectorAnswer = ""
                        var x = startX
                        var y = startY
                        val newStartingPoints = mutableListOf<Pair<Int, Int>>()
                        do {
                            vectorAnswer += grid[y][x]
                            newStartingPoints += x to y
                            x += direction.dx
                            y += direction.dy
                        } while (y in grid.indices && x in grid[y].indices)
                        if (vectorAnswer.startsWith(answer)) {
                            require(foundVector == null) { "Found two possible vectors for answer $answer in grid" }
                            foundVector = vector
                            startingPoints = newStartingPoints
                        }
                    }
                }
            }
            require(foundVector != null) { "Could not find answer $answer in grid" }
            foundVectors.add(foundVector!!)
        }
        return foundVectors.toList()
    }
}