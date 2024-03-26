package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.formats.Puzzleable
import com.jeffpdavidson.kotwords.util.trimmedLines

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
    val completionMessage: String = "",
    val includeAttribution: Boolean = true,
) : Puzzleable() {
    init {
        require(clues.size > 1) { "Must have at least 2 clues." }
        require(clues.size == gridKey.size) {
            "Have ${clues.size} clues but ${gridKey.size} answers"
        }
        val allNumbers = gridKey.flatten().sorted()
        require((1..allNumbers.size).toList() == allNumbers) {
            "Grid key must contain exactly one of each number from 1 to the size of the grid"
        }
        val solutionLetters = solution.filter { it in 'A'..'Z' || it in '0'..'9' }
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

    override suspend fun createPuzzle(): Puzzle {
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
            gridKey.mapIndexed { i, nums -> nums.map { it to getClueLetters(i) } }.flatten().toMap()

        // Generate the quote portion of the grid.
        val solutionChars = mutableListOf<Char>()
        val grid = mutableListOf<List<Puzzle.Cell>>()
        var row = mutableListOf<Puzzle.Cell>()
        var x = 0
        var y = 0
        val quoteWord = mutableListOf<Puzzle.Coordinate>()
        fun nextRow() {
            grid.add(row)
            row = mutableListOf()
            x = 0
            y++
        }
        solution.forEach { ch ->
            row.add(
                when (ch) {
                    in 'A'..'Z', in '0'..'9' -> {
                        solutionChars.add(ch)
                        val cellNumber = solutionChars.size
                        val clueLetter =
                            solutionIndexToClueLetterMap[cellNumber] ?: error("Impossible")
                        val cell = Puzzle.Cell(
                            solution = "$ch", number = "$cellNumber", topRightNumber = clueLetter
                        )
                        quoteWord.add(Puzzle.Coordinate(x = x, y = y))
                        cell
                    }

                    ' ' -> Puzzle.Cell(cellType = Puzzle.CellType.BLOCK)
                    else -> {
                        // Replace hyphen with en-dash for aesthetics.
                        val clue = "$ch".replace('-', '–')
                        Puzzle.Cell(cellType = Puzzle.CellType.CLUE, solution = clue)
                    }
                }
            )

            // Go to the next square, moving down a row if needed.
            x++
            if (x >= width) {
                nextRow()
            }
        }

        fun endSection() {
            // Fill the rest of the current row with white squares.
            if (x > 0) {
                row.addAll(generateBlankCells(width - x))
                nextRow()
            }

            // Add a spacer row of white squares.
            row.addAll(generateBlankCells(width))
            nextRow()
        }
        endSection()

        // Add the attribution (first letters of each clue).
        val attributionWord: MutableList<Puzzle.Coordinate>?
        if (includeAttribution) {
            attributionWord = mutableListOf()
            gridKey.forEachIndexed { answerIndex, answer ->
                val cell = Puzzle.Cell(
                    solution = "${solutionChars[answer[0] - 1]}",
                    number = "${answer[0]}",
                    topRightNumber = getClueLetters(answerIndex),
                )
                attributionWord.add(Puzzle.Coordinate(x = x, y = y))
                row.add(cell)
                x++
                if (x >= width) {
                    nextRow()
                }
            }
            endSection()
        } else {
            attributionWord = null
        }

        // Generate the clue/answer portion of the grid.
        val answerWordMap = mutableMapOf<Int, List<Puzzle.Coordinate>>()
        fun addClue(clueIndex: Int, columnEndIndex: Int, answer: List<Int>) {
            x++
            row.add(
                Puzzle.Cell(
                    cellType = Puzzle.CellType.CLUE, solution = getClueLetters(clueIndex)
                )
            )
            val cells = mutableListOf<Puzzle.Cell>()
            val word = mutableListOf<Puzzle.Coordinate>()
            answer.forEachIndexed { i, num ->
                val solutionLetter = "${solutionChars[num - 1]}"
                val solutionNumber = "${gridKey[clueIndex][i]}"
                cells += Puzzle.Cell(solution = solutionLetter, number = solutionNumber)
                word += Puzzle.Coordinate(x++, y)
            }
            answerWordMap[clueIndex] = word
            row.addAll(cells)
            row.addAll(generateBlankCells(columnEndIndex - x))
            x = columnEndIndex
        }

        gridKeyColumns[0].forEachIndexed { answerRow, leftAnswer ->
            // Left column answer
            addClue(answerRow, answerColumnWidths.first, leftAnswer)
            // Right column answer
            if (answerRow < gridKeyColumns[1].size) {
                addClue(gridKeyColumns[0].size + answerRow, width, gridKeyColumns[1][answerRow])
            } else {
                row.addAll(generateBlankCells(width - x))
            }
            nextRow()
        }


        val (answerClues, answerWords) = clues.mapIndexed { index, clue ->
            val answerWord = answerWordMap[index] ?: error("Impossible")
            val word = Puzzle.Word(index + 1, answerWord)
            Puzzle.Clue(index + 1, getClueLetters(index), clue) to word
        }.unzip()
        val words =
            answerWords +
                    (attributionWord?.let { listOf(Puzzle.Word(900, it)) } ?: listOf()) +
                    listOf(Puzzle.Word(1000, quoteWord))
        val clueList = Puzzle.ClueList(
            "Clues",
            answerClues +
                    (if (attributionWord != null) listOf(Puzzle.Clue(900, "", "[ATTRIBUTION]")) else listOf()) +
                    Puzzle.Clue(1000, "", "[QUOTE]")
        )

        return Puzzle(
            title = title,
            creator = creator,
            copyright = copyright,
            description = description,
            grid = grid,
            clues = listOf(clueList),
            words = words,
            completionMessage = completionMessage,
            puzzleType = Puzzle.PuzzleType.ACROSTIC,
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
            completionMessage: String,
            includeAttribution: Boolean,
        ): Acrostic {
            val suggestedWidthInt = if (suggestedWidth.isEmpty()) null else suggestedWidth.toInt()
            val answersList = answers.uppercase().trimmedLines()
            val normalizedSolution = solution.uppercase()
            val gridKeyList = if (gridKey.isBlank()) {
                generateGridKey(normalizedSolution, answersList)
            } else {
                gridKey.trimmedLines().map { row -> row.split(" +".toRegex()).map { it.trim().toInt() } }
            }
            return Acrostic(
                title = title,
                creator = creator,
                copyright = copyright,
                description = description,
                suggestedWidth = suggestedWidthInt,
                solution = normalizedSolution,
                gridKey = gridKeyList,
                clues = clues.trimmedLines(),
                answers = answersList,
                completionMessage = completionMessage,
                includeAttribution = includeAttribution,
            )
        }

        fun generateGridKey(solution: String, answers: List<String>): List<List<Int>> {
            var wordIndex = 0
            // Ordered list of solution characters paired with their word index.
            val solutionWords = mutableListOf<Pair<Char, Int>>()
            solution.forEach { ch ->
                when (ch) {
                    in 'A'..'Z' -> {
                        solutionWords.add(ch to wordIndex)
                    }

                    ' ' -> wordIndex++
                }
            }

            val solutionLetterFrequencies = solutionWords.groupingBy { it.first }.eachCount()
            val answersLetterFrequencies = answers.flatMap { it.toList() }.groupingBy { it }.eachCount()
            if (solutionLetterFrequencies != answersLetterFrequencies) {
                throw IllegalArgumentException("Solution letters do not exactly match letters from clue answers")
            }

            val remainingSolutionCharacters = solutionWords.withIndex().toMutableSet()
            return answers.withIndex().shuffled().map { (answerIndex, answer) ->
                val solutionWordsUsedInAnswer = mutableSetOf<Int>()
                answerIndex to answer.withIndex().shuffled().map { (answerCharIndex, ch) ->
                    val matchingSolutionCharacters = remainingSolutionCharacters.filter { (_, charAndIndex) ->
                        ch == charAndIndex.first
                    }
                    val solutionCharactersInUnusedWords = matchingSolutionCharacters.filterNot { (_, charAndIndex) ->
                        solutionWordsUsedInAnswer.contains(charAndIndex.second)
                    }
                    // Prefer to use words in the solution that we haven't used yet, but accept any word if there's no
                    // unused words left.
                    val candidateSolutionCharacters = solutionCharactersInUnusedWords.ifEmpty {
                        matchingSolutionCharacters
                    }
                    val selectedSolutionCharacter = candidateSolutionCharacters.random()
                    remainingSolutionCharacters.remove(selectedSolutionCharacter)
                    solutionWordsUsedInAnswer.add(selectedSolutionCharacter.value.second)
                    answerCharIndex to (selectedSolutionCharacter.index + 1)
                }.sortedBy { it.first }.map { it.second }
            }.sortedBy { it.first }.map { it.second }
        }

        internal fun getAnswerColumnWidths(
            splitAnswers: List<List<List<Any>>>, suggestedWidth: Int = 0
        ): Pair<Int, Int> {
            val widths: List<Int> = splitAnswers.map { it.maxOf(List<Any>::size) + 1 }
            val totalWidth = maxOf(widths[0] + widths[1] + 1, 27, suggestedWidth)
            val leftWidth = widths[0] + (totalWidth - widths[0] - widths[1] - 1) / 2
            return leftWidth to (totalWidth - leftWidth - 1)
        }

        internal fun getClueLetters(clueIndex: Int): String = "${'A' + (clueIndex % 26)}".repeat(clueIndex / 26 + 1)

        private fun generateBlankCells(count: Int): List<Puzzle.Cell> {
            return (0 until count).map {
                Puzzle.Cell(cellType = Puzzle.CellType.VOID)
            }
        }
    }
}