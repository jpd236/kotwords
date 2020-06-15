package com.jeffpdavidson.kotwords.model

data class Spiral(
        val title: String,
        val creator: String,
        val copyright: String,
        val description: String,
        val inwardAnswers: List<String>,
        val inwardClues: List<String>,
        val outwardAnswers: List<String>,
        val outwardClues: List<String>
) {

    init {
        require(inwardAnswers.joinToString("") == outwardAnswers.joinToString("").reversed()) {
            "Inward answer letters must be the outward answer letters in reverse"
        }
    }

    fun asPuzzle(crosswordSolverSettings: Puzzle.CrosswordSolverSettings): Puzzle {
        val sideLength = SpiralGrid.getSideLength(inwardAnswers.sumBy { it.length })
        val squareList = SpiralGrid.createSquareList(sideLength)
        val inwardLetters = inwardAnswers.joinToString("")
        val gridMap = squareList.mapIndexed { i, (x, y) ->
            (x to y) to Puzzle.Cell(
                    x = x + 1,
                    y = y + 1,
                    number = "${i + 1}",
                    solution = "${inwardLetters[i]}",
                    borderDirections = listOfNotNull(squareList[i].borderDirection).toSet()
            )
        }.toMap()
        val grid = (0 until sideLength).map { y ->
            (0 until sideLength).map { x ->
                gridMap[x to y] ?: throw IllegalStateException()
            }
        }

        val inwardJpzClues = mutableListOf<Puzzle.Clue>()
        inwardAnswers.foldIndexed(0) { wordNumber, i, answer ->
            inwardJpzClues += Puzzle.Clue(
                    Puzzle.Word(
                            wordNumber + 1,
                            squareList.slice(i until i + answer.length).map { (x, y) -> grid[y][x] }),
                    "${i + 1}-${i + answer.length}",
                    inwardClues[wordNumber]
            )
            i + answer.length
        }

        val outwardJpzClues = mutableListOf<Puzzle.Clue>()
        outwardAnswers.foldIndexed(squareList.size) { wordNumber, i, answer ->
            outwardJpzClues += Puzzle.Clue(
                    Puzzle.Word(
                            wordNumber + 101,
                            squareList.slice(i - answer.length until i).reversed().map { (x, y) -> grid[y][x] }),
                    "$i-${i - answer.length + 1}",
                    outwardClues[wordNumber]
            )
            i - answer.length
        }

        return Puzzle(
                title = title,
                creator = creator,
                copyright = copyright,
                description = description,
                grid = grid,
                clues = listOf(Puzzle.ClueList("Inward", inwardJpzClues), Puzzle.ClueList("Outward", outwardJpzClues)),
                crosswordSolverSettings = crosswordSolverSettings
        )
    }
}