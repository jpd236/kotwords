package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.formats.Puzzleable

data class EightTracks(
    val title: String,
    val creator: String,
    val copyright: String,
    val description: String,
    val trackDirections: List<Direction>,
    val trackStartingOffsets: List<Int>,
    val trackAnswers: List<List<String>>,
    val trackClues: List<List<String>>,
    val includeEnumerationsAndDirections: Boolean,
    val lightTrackColor: String,
    val darkTrackColor: String,
) : Puzzleable() {

    enum class Direction {
        CLOCKWISE,
        COUNTERCLOCKWISE
    }

    override suspend fun createPuzzle(): Puzzle {
        val gridMap = mutableMapOf<Pair<Int, Int>, Puzzle.Cell>()
        val gridWidth = trackAnswers.size * 2 + 1
        val clueLists = mutableListOf<Puzzle.ClueList>()
        val otherTracks = mutableListOf<Puzzle.Clue>()
        val words = mutableListOf<Puzzle.Word>()
        trackAnswers.forEachIndexed { trackIndex, answers ->
            val trackWidth = gridWidth - 2 * trackIndex
            val trackCoordinates =
                (trackIndex until trackIndex + trackWidth).map { it to trackIndex } +
                        (trackIndex + 1 until trackIndex + trackWidth).map { trackIndex + trackWidth - 1 to it } +
                        (trackIndex + trackWidth - 2 downTo trackIndex).map { it to trackIndex + trackWidth - 1 } +
                        (trackIndex + trackWidth - 2 downTo trackIndex + 1).map { trackIndex to it }
            val startingOffset = trackStartingOffsets[trackIndex]
            val direction = trackDirections[trackIndex]
            val wordsCells = mutableListOf<List<Puzzle.Coordinate>>()
            answers.foldIndexed(0) { answerId, answerIndex, answer ->
                val word = mutableListOf<Puzzle.Coordinate>()
                answer.forEachIndexed { i, ch ->
                    val mult = if (direction == Direction.CLOCKWISE) 1 else -1
                    val coordinateIndex = (mult * (answerIndex + i) + startingOffset - 1) mod trackCoordinates.size
                    val coordinates = trackCoordinates[coordinateIndex]
                    val cell = Puzzle.Cell(
                        solution = "$ch",
                        number = if (trackIndex == 0 && i == 0) "${answerId + 1}" else "",
                        backgroundColor = if (trackIndex % 2 == 0) darkTrackColor else lightTrackColor,
                        borderDirections = getBorderDirections(coordinateIndex, trackCoordinates.size)
                    )
                    gridMap[coordinates] = cell
                    word += Puzzle.Coordinate(
                        x = coordinates.first,
                        y = coordinates.second,
                    )
                }
                wordsCells += word
                answerIndex + answer.length
            }
            if (trackIndex == 0) {
                val (trackClues, trackWords) = wordsCells.mapIndexed { i, cells ->
                    val clue = enumerateClue(trackClues[0][i], cells.size, includeEnumerationsAndDirections)
                    val word = Puzzle.Word(101 + i, cells)
                    Puzzle.Clue(101 + i, "${i + 1}", clue) to word
                }.unzip()
                clueLists.add(Puzzle.ClueList("Track 1", trackClues))
                words.addAll(trackWords)
            } else {
                var word = wordsCells.flatten()
                if (direction == Direction.COUNTERCLOCKWISE) {
                    word = reverseDirection(word)
                }
                var offsetWord =
                    word.slice((word.size - startingOffset + 1) until word.size) +
                            word.slice(0 until word.size - startingOffset + 1)
                var number = "${trackIndex + 1}"
                if (includeEnumerationsAndDirections) {
                    if (direction == Direction.COUNTERCLOCKWISE) {
                        offsetWord = reverseDirection(offsetWord)
                    }
                    number += "(" + (if (direction == Direction.CLOCKWISE) "+" else "–") + ")"
                }
                val clues = trackClues[trackIndex].mapIndexed { i, clue ->
                    enumerateClue(clue, wordsCells[i].size, includeEnumerationsAndDirections)
                }.joinToString(" / ")
                val trackWord = Puzzle.Word(trackIndex + 1, offsetWord)
                otherTracks.add(Puzzle.Clue(trackIndex + 1, number, clues))
                words.add(trackWord)
            }
        }
        val grid = (0 until gridWidth).map { y ->
            (0 until gridWidth).map { x ->
                gridMap.getOrElse(x to y) { Puzzle.Cell(cellType = Puzzle.CellType.BLOCK) }
            }
        }
        clueLists.add(Puzzle.ClueList("Other tracks", otherTracks))

        return Puzzle(
            title = title,
            creator = creator,
            copyright = copyright,
            description = description,
            grid = grid,
            clues = clueLists,
            words = words,
        )
    }

    private fun getBorderDirections(coordinateIndex: Int, coordinateCount: Int): Set<Puzzle.BorderDirection> {
        return when (8 * coordinateIndex + 8) {
            coordinateCount -> setOf(Puzzle.BorderDirection.RIGHT)
            2 * coordinateCount -> setOf(Puzzle.BorderDirection.RIGHT, Puzzle.BorderDirection.BOTTOM)
            3 * coordinateCount -> setOf(Puzzle.BorderDirection.BOTTOM)
            4 * coordinateCount -> setOf(Puzzle.BorderDirection.BOTTOM, Puzzle.BorderDirection.LEFT)
            5 * coordinateCount -> setOf(Puzzle.BorderDirection.LEFT)
            6 * coordinateCount -> setOf(Puzzle.BorderDirection.LEFT, Puzzle.BorderDirection.TOP)
            7 * coordinateCount -> setOf(Puzzle.BorderDirection.TOP)
            8 * coordinateCount -> setOf(Puzzle.BorderDirection.TOP, Puzzle.BorderDirection.RIGHT)
            else -> setOf()
        }
    }

    private fun enumerateClue(clue: String, length: Int, includeEnumerationsAndDirections: Boolean): String {
        if (!includeEnumerationsAndDirections) {
            return clue
        }
        return "$clue ($length)"
    }

    private fun <T> reverseDirection(list: List<T>): List<T> {
        return listOf(list[0]) + list.slice(1 until list.size).reversed()
    }

    private infix fun Int.mod(b: Int): Int {
        val result = this % b
        return if (result < 0) result + b else result
    }
}