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
        val answers: List<String>,
        val crosswordSolverSettings: CrosswordSolverSettings) {
    init {
        require(clues.size > 1) { "Must have at least 2 clues." }
        require(clues.size == answers.size) {
            "Have ${clues.size} clues but ${answers.size} answers"
        }
        require(gridKey.size == answers.size) {
            "Have ${gridKey.size} clue number sets but ${answers.size} answers"
        }
        val allNumbers = gridKey.flatten().sorted()
        require((1..allNumbers.size).toList() == allNumbers) {
            "Grid key must contain exactly one of each number from 1 to the size of the grid"
        }
    }

    fun asJpz(): Jpz {
        val answerColumnSize = (answers.size + 1) / 2
        val answerColumns = answers.chunked(answerColumnSize)
        val answerColumnWidths = getAnswerColumnWidths(answerColumns)
        val width = answerColumnWidths.first + answerColumnWidths.second

        val solutionIndexToClueLetterMap =
                gridKey.mapIndexed { i, nums -> nums.map { it to "${'A' + i}" } }.flatten().toMap()

        val solutionChars = mutableListOf<Char>()
        val grid = mutableListOf<List<Cell>>()
        var row = mutableListOf<Cell>()
        var x = 1
        var y = 1
        var num = 0
        val quoteWord = mutableListOf<Cell>()
        solution.forEach { ch ->
            row.add(when (ch) {
                in 'A'..'Z' -> {
                    num++
                    solutionChars.add(ch)
                    val cell = Cell(
                            x = x,
                            y = y,
                            solution = "$ch",
                            number = "$num",
                            topRightNumber = solutionIndexToClueLetterMap[num] ?: "")
                    quoteWord.add(cell)
                    cell
                }
                ' ' -> Cell(x = x, y = y, cellType = CellType.BLOCK)
                else -> {
                    Cell(x = x, y = y, cellType = CellType.CLUE, solution = "$ch")
                }
            })
            x++
            if (x > width) {
                grid.add(row)
                row = mutableListOf()
                x = 1
                y++
            }
        }
        if (x > 0) {
            row.addAll((x..width).map {
                Cell(x = it, y = y, cellType = CellType.BLOCK, backgroundColor = "#FFFFFF")
            })
        }
        grid.add(row)
        y++
        grid.add((0 until width).map {
            Cell(x = it + 1, y = y, cellType = CellType.BLOCK, backgroundColor = "#FFFFFF")
        }.toList())
        y++
        x = 1
        row = mutableListOf()
        val answerWords = mutableMapOf<Int, MutableList<Cell>>()
        answerColumns[0].forEachIndexed { answerRow, leftAnswer ->
            val word = mutableListOf<Cell>()
            row.add(Cell(x = x++, y = y, cellType = CellType.CLUE, solution = "${'A' + answerRow}"))
            leftAnswer.forEachIndexed { i, ch ->
                word.add(Cell(x = x++, y = y, solution = "$ch", number = "${gridKey[answerRow][i]}"))
            }
            answerWords[answerRow] = word
            row.addAll(word)
            row.addAll((x..answerColumnWidths.first).map {
                Cell(x = it, y = y, cellType = CellType.BLOCK, backgroundColor = "#FFFFFF")
            }.toList())
            x = answerColumnWidths.first + 1
            if (answerColumns[1].size > answerRow) {
                val secondWord = mutableListOf<Cell>()
                row.add(Cell(x = x++, y = y, cellType = CellType.CLUE, solution = "${'A' + answerColumns[0].size + answerRow}"))
                answerColumns[1][answerRow].forEachIndexed { i, ch ->
                    secondWord.add(Cell(x = x++, y = y, solution = "$ch", number = "${gridKey[answerColumns[0].size + answerRow][i]}"))
                }
                answerWords[answerColumns[0].size + answerRow] = secondWord
                row.addAll(secondWord)
            }
            row.addAll((x..width).map {
                Cell(x = it, y = y, cellType = CellType.BLOCK, backgroundColor = "#FFFFFF")
            }.toList())
            grid.add(row)
            row = mutableListOf()
            x = 1
            y++
        }
        val clueList =
                ClueList("Clues", clues.mapIndexed { index, clue ->
                    Clue(Word(index + 1, answerWords[index]!!), "${'A' + index}", clue)
                } + Clue(Word(1000, quoteWord), "", "[QUOTE]"))

        return Jpz(
                title,
                creator,
                copyright,
                description,
                grid,
                listOf(clueList),
                crosswordSolverSettings,
                PuzzleType.ACROSTIC)
    }

    private fun getAnswerColumnWidths(splitAnswers: List<List<String>>): Pair<Int, Int> {
        val widths: List<Int> = splitAnswers.map { it.map(String::length).max()!! + 1 }
        val totalWidth = maxOf(widths[0] + widths[1], 27, suggestedWidth ?: 0)
        val leftWidth = widths[0] + (totalWidth - widths[0] - widths[1]) / 2
        return leftWidth to (totalWidth - leftWidth)
    }
}