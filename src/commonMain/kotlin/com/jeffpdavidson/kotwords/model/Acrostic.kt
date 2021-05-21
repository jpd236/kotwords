package com.jeffpdavidson.kotwords.model

data class Acrostic(
    val title: String,
    val creator: String,
    val copyright: String,
    val description: String,
    val suggestedWidth: Int?,
    val solution: String,
    val gridKey: List<List<Int>>,
    val clues: List<String>,
    val answers: List<String> = listOf(),
    val crosswordSolverSettings: Puzzle.CrosswordSolverSettings
) {
    init {
        require(clues.size > 1) { "Must have at least 2 clues." }
        require(clues.size == gridKey.size) {
            "Have ${clues.size} clues but ${gridKey.size} answers"
        }
        val allNumbers = gridKey.flatten().sorted()
        require((1..allNumbers.size).toList() == allNumbers) {
            "Grid key must contain exactly one of each number from 1 to the size of the grid"
        }
        val solutionLetters = solution.filter { it in 'A'..'Z' }
        require(solutionLetters.length == allNumbers.size) {
            "Grid key must have same number of letters as the solution"
        }
        if (answers.isNotEmpty()) {
            // Additional consistency check for the answers.
            require(clues.size == answers.size) {
                "Have ${clues.size} clues but ${answers.size} answers"
            }
            gridKey.forEachIndexed { answerIndex, answerNumbers ->
                answerNumbers.forEachIndexed { i, answerNumber ->
                    val solutionLetter = solutionLetters[answerNumber - 1]
                    val answerLetter = answers[answerIndex][i]
                    require(solutionLetter == answerLetter) {
                        "Grid mismatch at $answerNumber: " +
                                "$solutionLetter from solution, $answerLetter from answers"
                    }
                }
            }
        }
    }

    fun asPuzzle(): Puzzle {
        // Determine the width of the puzzle and both clue columns.
        val answerColumnSize = (gridKey.size + 1) / 2
        val gridKeyColumns = gridKey.chunked(answerColumnSize)
        val answerColumnWidths = getAnswerColumnWidths(
            gridKeyColumns, suggestedWidth
                ?: 0
        )
        val width = answerColumnWidths.first + answerColumnWidths.second + 1

        // Map from number in the grid key to the letter of the clue whose answer has that number.
        val solutionIndexToClueLetterMap =
            gridKey.mapIndexed { i, nums -> nums.map { it to "${'A' + i}" } }.flatten().toMap()

        // Generate the quote portion of the grid.
        val solutionChars = mutableListOf<Char>()
        val grid = mutableListOf<List<Puzzle.Cell>>()
        var row = mutableListOf<Puzzle.Cell>()
        var x = 1
        var y = 1
        val quoteWord = mutableListOf<Puzzle.Cell>()
        fun nextRow() {
            grid.add(row)
            row = mutableListOf()
            x = 1
            y++
        }
        solution.forEach { ch ->
            row.add(
                when (ch) {
                    in 'A'..'Z' -> {
                        solutionChars.add(ch)
                        val cellNumber = solutionChars.size
                        val clueLetter =
                            solutionIndexToClueLetterMap[cellNumber] ?: error("Impossible")
                        val cell = Puzzle.Cell(
                            x, y,
                            solution = "$ch", number = "$cellNumber", topRightNumber = clueLetter
                        )
                        quoteWord.add(cell)
                        cell
                    }
                    ' ' -> Puzzle.Cell(x, y, cellType = Puzzle.CellType.BLOCK)
                    else -> {
                        // Replace hyphen with en-dash for aesthetics.
                        val clue = "$ch".replace('-', '–')
                        Puzzle.Cell(x, y, cellType = Puzzle.CellType.CLUE, solution = clue)
                    }
                }
            )

            // Go to the next square, moving down a row if needed.
            x++
            if (x > width) {
                nextRow()
            }
        }

        // Fill the rest of the current row with white squares.
        if (x > 1) {
            row.addAll(generateWhiteCells(x..width, y))
            nextRow()
        }

        // Add a spacer row of white squares.
        row.addAll(generateWhiteCells(1..width, y))
        nextRow()

        // Generate the clue/answer portion of the grid.
        val answerWords = mutableMapOf<Int, List<Puzzle.Cell>>()
        fun addClue(clueIndex: Int, columnEndIndex: Int, answer: List<Int>) {
            row.add(
                Puzzle.Cell(
                    x++, y,
                    cellType = Puzzle.CellType.CLUE, solution = "${'A' + clueIndex}"
                )
            )
            val word = answer.mapIndexed { i, num ->
                val solutionLetter = "${solutionChars[num - 1]}"
                val solutionNumber = "${gridKey[clueIndex][i]}"
                Puzzle.Cell(x++, y, solution = solutionLetter, number = solutionNumber)
            }
            answerWords[clueIndex] = word
            row.addAll(word)
            row.addAll(generateWhiteCells(x..columnEndIndex, y))
            x = columnEndIndex + 1
        }

        gridKeyColumns[0].forEachIndexed { answerRow, leftAnswer ->
            // Left column answer
            addClue(answerRow, answerColumnWidths.first, leftAnswer)
            // Right column answer
            if (answerRow < gridKeyColumns[1].size) {
                addClue(gridKeyColumns[0].size + answerRow, width, gridKeyColumns[1][answerRow])
            } else {
                row.addAll(generateWhiteCells(x..width, y))
            }
            nextRow()
        }

        val answerClues = clues.mapIndexed { index, clue ->
            val answerWord = answerWords[index] ?: error("Impossible")
            Puzzle.Clue(Puzzle.Word(index + 1, answerWord), "${'A' + index}", clue)
        }
        val clueList = Puzzle.ClueList("Clues", answerClues + Puzzle.Clue(Puzzle.Word(1000, quoteWord), "", "[QUOTE]"))

        return Puzzle(
            title,
            creator,
            copyright,
            description,
            grid,
            listOf(clueList),
            crosswordSolverSettings = crosswordSolverSettings,
            puzzleType = Puzzle.PuzzleType.ACROSTIC
        )
    }

    companion object {
        fun fromRawInput(
            title: String,
            creator: String,
            copyright: String,
            description: String,
            suggestedWidth: String,
            solution: String,
            gridKey: String,
            clues: String,
            answers: String,
            crosswordSolverSettings: Puzzle.CrosswordSolverSettings
        ): Acrostic {
            val suggestedWidthInt = if (suggestedWidth.isEmpty()) null else suggestedWidth.toInt()
            val gridKeyList = gridKey.trim().split("\n").map { row ->
                row.trim().split(" +".toRegex()).map { it.trim().toInt() }
            }
            val answersList =
                if (answers.trim().isEmpty()) {
                    listOf()
                } else {
                    answers.trim().split("\n").map { it.trim() }
                }
            return Acrostic(
                title.trim(),
                creator.trim(),
                copyright.trim(),
                description.trim(),
                suggestedWidthInt,
                solution.trim().toUpperCase(),
                gridKeyList,
                clues.trim().split("\n").map { it.trim() },
                answersList,
                crosswordSolverSettings
            )
        }

        internal fun getAnswerColumnWidths(
            splitAnswers: List<List<List<Any>>>, suggestedWidth: Int = 0
        ): Pair<Int, Int> {
            val widths: List<Int> = splitAnswers.map { it.maxOf(List<Any>::size) + 1 }
            val totalWidth = maxOf(widths[0] + widths[1] + 1, 27, suggestedWidth)
            val leftWidth = widths[0] + (totalWidth - widths[0] - widths[1] - 1) / 2
            return leftWidth to (totalWidth - leftWidth - 1)
        }

        private fun generateWhiteCells(xRange: IntRange, y: Int): List<Puzzle.Cell> {
            return xRange.map {
                Puzzle.Cell(x = it, y = y, cellType = Puzzle.CellType.BLOCK, backgroundColor = "#FFFFFF")
            }
        }
    }
}