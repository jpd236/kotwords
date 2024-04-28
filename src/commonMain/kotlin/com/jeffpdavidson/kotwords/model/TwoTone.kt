package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.formats.Puzzleable

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
    val evenSquaresClues: List<String>,
    val oddSquareBackgroundColor: String,
    val evenSquareBackgroundColor: String,
    val dimensions: Pair<Int, Int> = 0 to 0,
) : Puzzleable() {

    init {
        val splitAnswers =
            allSquaresAnswers.joinToString("")
                .mapIndexed { i, ch -> i to ch }
                .partition { it.first % 2 == 0 }
                .toList()
                .map { it.map { (_, ch) -> ch }.joinToString("") }
        require(allSquaresClues.size == allSquaresAnswers.size) {
            "Different number of all square clues (${allSquaresClues.size}) than answers (${allSquaresAnswers.size})"
        }
        require(oddSquaresClues.size == oddSquaresAnswers.size) {
            "Different number of odd square clues (${oddSquaresClues.size}) than answers (${oddSquaresAnswers.size})"
        }
        require(evenSquaresClues.size == evenSquaresAnswers.size) {
            "Different number of even square clues (${evenSquaresClues.size}) than answers (${evenSquaresAnswers.size})"
        }
        require(oddSquaresAnswers.joinToString("") == splitAnswers[0]) {
            "Odd square answers do not match the odd squares of the all squares answers"
        }
        require(evenSquaresAnswers.joinToString("") == splitAnswers[1]) {
            "Even square answers do not match the even squares of the all squares answers"
        }
        if (dimensions.first > 0 || dimensions.second > 0) {
            require(dimensions.first > 0 && dimensions.second > 0) {
                "Either neither or both of width and height must be specified"
            }
            require(dimensions.first * dimensions.second >= allSquaresAnswers.sumOf { it.length }) {
                "Grid size not large enough to fit all cells"
            }
        }
    }

    override suspend fun createPuzzle(): Puzzle {
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

        val (width, height) = if (dimensions.first > 0 || dimensions.second > 0) {
            dimensions
        } else {
            val sideLength = SpiralGrid.getSideLength(allSquaresAnswers.sumOf { it.length })
            sideLength to sideLength
        }
        val squareList = SpiralGrid.createSquareList(width, height)
        val letters = allSquaresAnswers.joinToString("")
        var currentNumber = 1
        val gridMap = squareList.mapIndexed { i, (x, y) ->
            (x to y) to
                    if (i < letters.length) {
                        Puzzle.Cell(
                            number = if (numberedSquares.contains(i)) "${currentNumber++}" else "",
                            solution = "${letters[i]}",
                            backgroundColor = if (i % 2 == 0) oddSquareBackgroundColor else evenSquareBackgroundColor,
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
            val jpzClues = mutableListOf<Puzzle.Clue>()
            val jpzWords = mutableListOf<Puzzle.Word>()
            answers.foldIndexed(0) { wordNumber, i, answer ->
                val firstCell = squareList[i]
                jpzWords += Puzzle.Word(
                    firstWordId + wordNumber,
                    squareList.slice(i until i + answer.length).map { (x, y) -> Puzzle.Coordinate(x = x, y = y) }
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
        )
    }
}