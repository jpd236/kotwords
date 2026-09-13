package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.formats.Puzzleable

data class Shapeshifter(
    val title: String,
    val creator: String,
    val copyright: String,
    val description: String,
    val shortsAnswers: List<String>,
    val shortsClues: List<String>,
    val longsClues: List<String>,
    val longsAnswers: List<String> = listOf(),
    val labelClues: Boolean = true,
) : Puzzleable() {

    init {
        val size = shortsAnswers.size
        require(size >= 3) {
            "Shorts count must be at least 3, but was $size"
        }
        val shortLength = size - 1
        require(shortsAnswers.all { it.length == shortLength }) {
            "All short answers must have length $shortLength, but at least one differed"
        }
        require(shortsClues.size == size) {
            "Grid has $size shorts but has ${shortsClues.size} shorts clues"
        }
        val longCount = size - 1
        require(longsClues.size == longCount) {
            "Grid has $longCount longs but has ${longsClues.size} longs clues"
        }
        if (longsAnswers.isNotEmpty()) {
            require(longsAnswers.size == longCount) {
                "Grid has $longCount longs but has ${longsAnswers.size} longs answers"
            }
            require(longsAnswers.all { it.length == size }) {
                "All long answers must have length $size, but at least one differed"
            }
            for (r in 0 until longCount) {
                val expectedLong = (0..r).map { x -> shortsAnswers[r + 1][x] }.joinToString("") +
                        (r + 1 until size).map { x -> shortsAnswers[r][x - 1] }.joinToString("")
                require(longsAnswers[r].uppercase() == expectedLong.uppercase()) {
                    "Long answer ${r + 1} does not match the grid derived from shorts"
                }
            }
        }
    }

    override suspend fun createPuzzle(): Puzzle {
        val n = shortsAnswers.size

        val puzzleGrid = (0 until n).map { y ->
            (0 until n).map { x ->
                when {
                    (x == 0 && y == 0) || (x == n - 1 && y == n - 1) ->
                        Puzzle.Cell(cellType = Puzzle.CellType.VOID)

                    x == y ->
                        Puzzle.Cell(cellType = Puzzle.CellType.BLOCK)

                    else -> {
                        val shortX = if (x < y) x else x - 1
                        val number = when {
                            labelClues && y == 0 && x == 1 -> "S1"
                            labelClues && y > 0 && x == 0 -> "S${y + 1}"
                            else -> ""
                        }
                        val topRightNumber = if (labelClues && y > 0 && x == 0) "L$y" else ""
                        Puzzle.Cell(
                            solution = "${shortsAnswers[y][shortX]}".uppercase(),
                            number = number,
                            topRightNumber = topRightNumber,
                        )
                    }
                }
            }
        }

        val (shortsClueList, shortsWordList) = shortsClues.mapIndexed { y, clue ->
            val cells = (0 until n).filter { it != y }.map { x -> Puzzle.Coordinate(x, y) }
            val number = if (labelClues) "S${y + 1}" else ""
            Puzzle.Clue(y + 1, number, clue) to Puzzle.Word(y + 1, cells)
        }.unzip()

        val (longsClueList, longsWordList) = longsClues.mapIndexed { r, clue ->
            val wordId = 1001 + r
            val cells = (0..r).map { x -> Puzzle.Coordinate(x, r + 1) } +
                    (r + 1 until n).map { x -> Puzzle.Coordinate(x, r) }
            val number = if (labelClues) "L${r + 1}" else ""
            Puzzle.Clue(wordId, number, clue) to Puzzle.Word(wordId, cells)
        }.unzip()

        return Puzzle(
            title = title,
            creator = creator,
            copyright = copyright,
            description = description,
            grid = puzzleGrid,
            clues = listOf(
                Puzzle.ClueList("Shorts", if (labelClues) shortsClueList else shortsClueList.sortedBy { it.text }),
                Puzzle.ClueList("Longs", if (labelClues) longsClueList else longsClueList.sortedBy { it.text }),
            ),
            words = if (labelClues) shortsWordList + longsWordList else listOf(),
        )
    }
}
