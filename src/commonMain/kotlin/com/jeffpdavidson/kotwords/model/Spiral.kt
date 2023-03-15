package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.formats.Puzzleable

data class Spiral(
    val title: String,
    val creator: String,
    val copyright: String,
    val description: String,
    val inwardAnswers: List<String>,
    val inwardClues: List<String>,
    val outwardAnswers: List<String>,
    val outwardClues: List<String>,
    val inwardCellsInput: List<String> = listOf(),
    val dimensions: Pair<Int, Int> = 0 to 0,
) : Puzzleable() {

    private val inwardCells = inwardCellsInput.ifEmpty { inwardAnswers.joinToString("").chunked(1) }
    private val outwardCells = inwardCells.reversed()

    init {
        require(inwardAnswers.joinToString("") == inwardCells.joinToString("")) {
            "Inward cells do not match the inward answers"
        }
        require(outwardAnswers.joinToString("") == outwardCells.joinToString("")) {
            "Inward and outward answers/cells do not match"
        }
        require(inwardClues.size == inwardAnswers.size) {
            "Different number of inward clues (${inwardClues.size}) than answers (${inwardAnswers.size})"
        }
        require(outwardClues.size == outwardAnswers.size) {
            "Different number of outward clues (${outwardClues.size}) than answers (${outwardAnswers.size})"
        }
        if (dimensions.first > 0 || dimensions.second > 0) {
            require(dimensions.first > 0 && dimensions.second > 0) {
                "Either neither or both of width and height must be specified"
            }
            require(dimensions.first * dimensions.second >= inwardCells.size) {
                "Grid size not large enough to fit all cells"
            }
        }
    }

    override suspend fun createPuzzle(): Puzzle {
        val (width, height) = if (dimensions.first > 0 || dimensions.second > 0) {
            dimensions
        } else {
            val sideLength = SpiralGrid.getSideLength(inwardCells.size)
            sideLength to sideLength
        }
        val squareList = SpiralGrid.createSquareList(width, height)
        val gridMap = squareList.mapIndexed { i, (x, y) ->
            (x to y) to
                    if (i < inwardCells.size) {
                        Puzzle.Cell(
                            number = "${i + 1}",
                            solution = inwardCells[i],
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

        val words = mutableListOf<Puzzle.Word>()

        val inwardJpzClues = mutableListOf<Puzzle.Clue>()
        inwardAnswers.foldIndexed(0) { wordNumber, i, answer ->
            var cells = 0
            var partialAnswer = ""
            while (partialAnswer.length < answer.length) {
                partialAnswer += inwardCells[i + cells++]
            }
            require(partialAnswer.length == answer.length) {
                "Answers must be split cleanly across answer chunks"
            }
            val endCell = i + cells
            words += Puzzle.Word(
                wordNumber + 1,
                squareList.slice(i until endCell).map { (x, y) -> Puzzle.Coordinate(x = x, y = y) }
            )
            inwardJpzClues += Puzzle.Clue(
                wordNumber + 1,
                "${i + 1}-${endCell}",
                inwardClues[wordNumber]
            )
            endCell
        }

        val outwardJpzClues = mutableListOf<Puzzle.Clue>()
        outwardAnswers.foldIndexed(outwardCells.size) { wordNumber, i, answer ->
            var cells = 0
            var partialAnswer = ""
            while (partialAnswer.length < answer.length) {
                partialAnswer += inwardCells[i - cells++ - 1]
            }
            require(partialAnswer.length == answer.length) {
                "Answers must be split cleanly across answer chunks"
            }
            val endCell = i - cells
            words += Puzzle.Word(
                wordNumber + 101,
                squareList.slice(endCell until i).reversed().map { (x, y) -> Puzzle.Coordinate(x = x, y = y) }
            )
            outwardJpzClues += Puzzle.Clue(
                wordNumber + 101,
                "$i-${endCell + 1}",
                outwardClues[wordNumber]
            )
            endCell
        }

        return Puzzle(
            title = title,
            creator = creator,
            copyright = copyright,
            description = description,
            grid = grid,
            clues = listOf(Puzzle.ClueList("Inward", inwardJpzClues), Puzzle.ClueList("Outward", outwardJpzClues)),
            words = words,
        )
    }
}