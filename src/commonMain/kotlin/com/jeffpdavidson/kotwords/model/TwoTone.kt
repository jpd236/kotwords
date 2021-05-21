package com.jeffpdavidson.kotwords.model

data class TwoTone(
    val title: String,
    val creator: String,
    val copyright: String,
    val description: String,
    val allSquaresAnswers: List<String>,
    val allSquaresClues: List<String>,
    val oddSquaresAnswers: List<String>,
    val oddSquaresClues: List<String>,
    val evenSquaresAnswers: List<String>,
    val evenSquaresClues: List<String>
) {

    init {
        val splitAnswers =
            allSquaresAnswers.joinToString("")
                .mapIndexed { i, ch -> i to ch }
                .partition { it.first % 2 == 0 }
                .toList()
                .map { it.map { (_, ch) -> ch }.joinToString("") }
        require(oddSquaresAnswers.joinToString("") == splitAnswers[0]) {
            "Odd square answers do not match the odd squares of the all squares answers"
        }
        require(evenSquaresAnswers.joinToString("") == splitAnswers[1]) {
            "Even square answers do not match the even squares of the all squares answers"
        }
    }

    fun asPuzzle(
        oddSquareBackgroundColor: String,
        evenSquareBackgroundColor: String,
        crosswordSolverSettings: Puzzle.CrosswordSolverSettings
    ): Puzzle {
        val numberedSquares = mutableSetOf<Int>()
        fun addNumberedSquares(answers: List<String>, startIndex: Int, isEveryOther: Boolean) {
            answers.fold(startIndex) { i, answer ->
                numberedSquares += i
                i + answer.length * if (isEveryOther) 2 else 1
            }
        }
        addNumberedSquares(allSquaresAnswers, 0, false)
        addNumberedSquares(oddSquaresAnswers, 0, true)
        addNumberedSquares(evenSquaresAnswers, 1, true)

        val sideLength = SpiralGrid.getSideLength(allSquaresAnswers.sumOf { it.length })
        val squareList = SpiralGrid.createSquareList(sideLength)
        val letters = allSquaresAnswers.joinToString("")
        var currentNumber = 1
        val gridMap = squareList.mapIndexed { i, (x, y) ->
            (x to y) to
                    if (i < letters.length) {
                        Puzzle.Cell(
                            x = x + 1,
                            y = y + 1,
                            number = if (numberedSquares.contains(i)) "${currentNumber++}" else "",
                            solution = "${letters[i]}",
                            backgroundColor = if (i % 2 == 0) oddSquareBackgroundColor else evenSquareBackgroundColor,
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
        ): Pair<List<Puzzle.Clue>, List<Puzzle.Word>> {
            val jpzClues = mutableListOf<Puzzle.Clue>()
            val jpzWords = mutableListOf<Puzzle.Word>()
            answers.foldIndexed(0) { wordNumber, i, answer ->
                val firstCell = squareList[i]
                jpzWords += Puzzle.Word(
                    firstWordId + wordNumber,
                    squareList.slice(i until i + answer.length).map { (x, y) -> grid[y][x] }
                )
                jpzClues += Puzzle.Clue(
                    firstWordId + wordNumber,
                    grid[firstCell.y][firstCell.x].number,
                    clues[wordNumber]
                )
                i + answer.length
            }
            return jpzClues to jpzWords
        }

        val (allSquaresJpzClues, allSquaresJpzWords) = createClues(allSquaresAnswers, allSquaresClues, squareList, 1)
        val partitionedSquares = squareList
            .mapIndexed { i, square -> i to square }
            .partition { it.first % 2 == 0 }
            .toList()
            .map { it.map { (_, square) -> square } }
        val (oddSquaresJpzClues, oddSquaresJpzWords) =
            createClues(oddSquaresAnswers, oddSquaresClues, partitionedSquares[0], 101)
        val (evenSquaresJpzClues, evenSquaresJpzWords) =
            createClues(evenSquaresAnswers, evenSquaresClues, partitionedSquares[1], 201)

        return Puzzle(
            title = title,
            creator = creator,
            copyright = copyright,
            description = description,
            grid = grid,
            clues = listOf(
                Puzzle.ClueList("All Squares", allSquaresJpzClues),
                Puzzle.ClueList("Every Other", oddSquaresJpzClues + evenSquaresJpzClues)
            ),
            words = allSquaresJpzWords + oddSquaresJpzWords + evenSquaresJpzWords,
            crosswordSolverSettings = crosswordSolverSettings
        )
    }
}