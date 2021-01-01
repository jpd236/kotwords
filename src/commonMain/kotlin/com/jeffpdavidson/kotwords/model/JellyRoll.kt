package com.jeffpdavidson.kotwords.model

// TODO: Can more logic be shared with TwoTone?
data class JellyRoll(
    val title: String,
    val creator: String,
    val copyright: String,
    val description: String,
    val jellyRollAnswers: List<String>,
    val jellyRollClues: List<String>,
    val lightSquaresAnswers: List<String>,
    val lightSquaresClues: List<String>,
    val darkSquaresAnswers: List<String>,
    val darkSquaresClues: List<String>
) {

    init {
        val splitAnswers =
            jellyRollAnswers.joinToString("")
                .mapIndexed { i, ch -> i to ch }
                .partition { LIGHT_SQUARE_MODULOS.contains(it.first % 4) }
                .toList()
                .map { it.map { (_, ch) -> ch }.joinToString("") }
        require(lightSquaresAnswers.joinToString("") == splitAnswers[0]) {
            "Light square answers do not match the jelly roll answers"
        }
        require(darkSquaresAnswers.joinToString("") == splitAnswers[1]) {
            "Dark square answers do not match the jelly roll answers"
        }
    }

    fun asPuzzle(
        lightSquareBackgroundColor: String,
        darkSquareBackgroundColor: String,
        combineJellyRollClues: Boolean,
        crosswordSolverSettings: Puzzle.CrosswordSolverSettings
    ): Puzzle {
        val numberedSquares = mutableSetOf<Int>()
        fun addNumberedSquares(answers: List<String>, startIndex: Int, includedModulos: List<Int>) {
            answers.fold(startIndex) { i, answer ->
                numberedSquares += i
                var remainingSquaresToSkip = answer.length
                var position = i
                while (remainingSquaresToSkip > 0 || !includedModulos.contains(position % 4)) {
                    if (includedModulos.contains(position % 4)) {
                        remainingSquaresToSkip -= 1
                    }
                    position += 1
                }
                position
            }
        }
        if (!combineJellyRollClues) {
            addNumberedSquares(jellyRollAnswers, 0, listOf(0, 1, 2, 3))
        }
        addNumberedSquares(lightSquaresAnswers, 0, LIGHT_SQUARE_MODULOS)
        addNumberedSquares(darkSquaresAnswers, 1, DARK_SQUARE_MODULOS)

        val sideLength = SpiralGrid.getSideLength(jellyRollAnswers.sumBy { it.length })
        val squareList = SpiralGrid.createSquareList(sideLength)
        val letters = jellyRollAnswers.joinToString("")
        var currentNumber = 1
        val gridMap = squareList.mapIndexed { i, (x, y) ->
            (x to y) to
                    if (i < letters.length) {
                        Puzzle.Cell(
                            x = x + 1,
                            y = y + 1,
                            number = if (numberedSquares.contains(i)) "${currentNumber++}" else "",
                            solution = "${letters[i]}",
                            backgroundColor =
                            if (LIGHT_SQUARE_MODULOS.contains(i % 4)) {
                                lightSquareBackgroundColor
                            } else {
                                darkSquareBackgroundColor
                            },
                            borderDirections = listOfNotNull(squareList[i].borderDirection).toSet()
                        )
                    } else {
                        Puzzle.Cell(
                            x = x + 1,
                            y = y + 1,
                            cellType = Puzzle.CellType.BLOCK
                        )
                    }
        }.toMap()
        val grid = (0 until sideLength).map { y ->
            (0 until sideLength).map { x ->
                gridMap[x to y] ?: throw IllegalStateException()
            }
        }

        fun createClues(
            answers: List<String>,
            clues: List<String>,
            squareList: List<SpiralGrid.Square>,
            firstWordId: Int
        ): List<Puzzle.Clue> {
            val jpzClues = mutableListOf<Puzzle.Clue>()
            answers.foldIndexed(0) { wordNumber, i, answer ->
                val firstCell = squareList[i]
                jpzClues += Puzzle.Clue(
                    Puzzle.Word(
                        firstWordId + wordNumber,
                        squareList.slice(i until i + answer.length).map { (x, y) -> grid[y][x] }),
                    grid[firstCell.y][firstCell.x].number,
                    clues[wordNumber]
                )
                i + answer.length
            }
            return jpzClues
        }

        val allSquaresJpzClues =
            if (combineJellyRollClues) {
                createClues(
                    listOf(jellyRollAnswers.joinToString("")),
                    listOf(jellyRollClues.joinToString(" / ")),
                    squareList,
                    1
                )
            } else {
                createClues(jellyRollAnswers, jellyRollClues, squareList, 1)
            }
        val partitionedSquares = squareList
            .mapIndexed { i, square -> i to square }
            .partition { LIGHT_SQUARE_MODULOS.contains(it.first % 4) }
            .toList()
            .map { it.map { (_, square) -> square } }
        val everyOtherJpzClues =
            createClues(lightSquaresAnswers, lightSquaresClues, partitionedSquares[0], 101) +
                    createClues(darkSquaresAnswers, darkSquaresClues, partitionedSquares[1], 201)

        return Puzzle(
            title = title,
            creator = creator,
            copyright = copyright,
            description = description,
            grid = grid,
            clues = listOf(
                Puzzle.ClueList("Jelly Rolls", allSquaresJpzClues),
                Puzzle.ClueList("Colored Paths", everyOtherJpzClues)
            ),
            crosswordSolverSettings = crosswordSolverSettings
        )
    }

    private companion object {
        val LIGHT_SQUARE_MODULOS = listOf(0, 3)
        val DARK_SQUARE_MODULOS = listOf(1, 2)
    }
}