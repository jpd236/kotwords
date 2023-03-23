package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.formats.Puzzleable

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
    val darkSquaresClues: List<String>,
    val lightSquareBackgroundColor: String,
    val darkSquareBackgroundColor: String,
    val combineJellyRollClues: Boolean,
    val dimensions: Pair<Int, Int> = 0 to 0,
) : Puzzleable() {

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
        if (dimensions.first > 0 || dimensions.second > 0) {
            require (dimensions.first > 0 && dimensions.second > 0) {
                "Either neither or both of width and height must be specified"
            }
            require (dimensions.first * dimensions.second >= jellyRollAnswers.sumOf { it.length }) {
                "Grid size not large enough to fit all cells"
            }
        }
    }

    override suspend fun createPuzzle(): Puzzle {
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

        val (width, height) = if (dimensions.first > 0 || dimensions.second > 0) {
            dimensions
        } else {
            val sideLength = SpiralGrid.getSideLength(jellyRollAnswers.sumOf { it.length })
            sideLength to sideLength
        }
        val squareList = SpiralGrid.createSquareList(width, height)
        val letters = jellyRollAnswers.joinToString("")
        var currentNumber = 1
        val gridMap = squareList.mapIndexed { i, (x, y) ->
            (x to y) to
                    if (i < letters.length) {
                        Puzzle.Cell(
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
                        Puzzle.Cell(cellType = Puzzle.CellType.BLOCK)
                    }
        }.toMap()
        val grid = (0 until height).map { y ->
            (0 until width).map { x ->
                gridMap[x to y] ?: throw IllegalStateException()
            }
        }

        fun createClues(
            answers: List<String>,
            clues: List<String>,
            squareList: List<SpiralGrid.Square>,
            firstWordId: Int
        ): Pair<List<Puzzle.Clue>, List<Puzzle.Word>> {
            val puzzleClues = mutableListOf<Puzzle.Clue>()
            val puzzleWords = mutableListOf<Puzzle.Word>()
            answers.foldIndexed(0) { wordNumber, i, answer ->
                val firstCell = squareList[i]
                puzzleWords += Puzzle.Word(
                    firstWordId + wordNumber,
                    squareList.slice(i until i + answer.length).map { (x, y) -> Puzzle.Coordinate(x = x, y = y) }
                )
                puzzleClues += Puzzle.Clue(
                    firstWordId + wordNumber,
                    grid[firstCell.y][firstCell.x].number,
                    clues[wordNumber]
                )
                i + answer.length
            }
            return puzzleClues to puzzleWords
        }

        val (allSquaresPuzzleClues, allSquaresPuzzleWords) =
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

        val (lightSquaresPuzzleClues, lightSquaresPuzzleWords) =
            createClues(lightSquaresAnswers, lightSquaresClues, partitionedSquares[0], 101)
        val (darkSquaresPuzzleClues, darkSquaresPuzzleWords) =
            createClues(darkSquaresAnswers, darkSquaresClues, partitionedSquares[1], 201)

        return Puzzle(
            title = title,
            creator = creator,
            copyright = copyright,
            description = description,
            grid = grid,
            clues = listOf(
                Puzzle.ClueList("Jelly Rolls", allSquaresPuzzleClues),
                Puzzle.ClueList("Colored Paths", lightSquaresPuzzleClues + darkSquaresPuzzleClues)
            ),
            words = allSquaresPuzzleWords + lightSquaresPuzzleWords + darkSquaresPuzzleWords,
        )
    }

    private companion object {
        val LIGHT_SQUARE_MODULOS = listOf(0, 3)
        val DARK_SQUARE_MODULOS = listOf(1, 2)
    }
}