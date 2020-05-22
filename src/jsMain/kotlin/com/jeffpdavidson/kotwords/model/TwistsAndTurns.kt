package com.jeffpdavidson.kotwords.model

data class TwistsAndTurns(
        val title: String,
        val creator: String,
        val copyright: String,
        val description: String,
        val width: Int,
        val height: Int,
        val twistBoxSize: Int,
        val turnsAnswers: List<String>,
        val turnsClues: List<String>,
        val twistsClues: List<String>,
        val lightTwistsColor: String,
        val darkTwistsColor: String,
        val crosswordSolverSettings: Puzzle.CrosswordSolverSettings) {
    init {
        require(width % twistBoxSize == 0 && height % twistBoxSize == 0) {
            "Width $width and height $height must evenly divide twist box size $twistBoxSize"
        }
        val neededTwistClues = (width / twistBoxSize) * (height / twistBoxSize)
        require(twistsClues.size == neededTwistClues) {
            "Grid size requires $neededTwistClues twist clues but have ${twistsClues.size}"
        }
        require(turnsAnswers.size == turnsClues.size) {
            "Have ${turnsAnswers.size} turns answers but ${turnsClues.size} turns clues"
        }
        val cellCount = turnsAnswers.fold(0) { acc, str -> acc + str.length }
        require(cellCount == width * height) {
            "Have $cellCount letters in turns answers but need ${width * height}"
        }
    }

    fun asPuzzle(): Puzzle {
        var x = 1
        var y = 1
        val turnsCluesList = mutableListOf<Puzzle.Clue>()
        val cellMap = mutableMapOf<Pair<Int, Int>, Puzzle.Cell>()
        turnsAnswers.forEachIndexed { answerIndex, answer ->
            val word = mutableListOf<Puzzle.Cell>()
            val clueNumber = answerIndex + 1
            answer.forEachIndexed { chIndex, ch ->
                val number = if (chIndex == 0) {
                    "$clueNumber"
                } else {
                    ""
                }
                val backgroundColor =
                        if ((((x - 1) / twistBoxSize) % 2) == (((y - 1) / twistBoxSize) % 2)) {
                            "#FFFFFF"
                        } else {
                            "#999999"
                        }
                val cell = Puzzle.Cell(x, y, "$ch", backgroundColor, number)
                cellMap[x to y] = cell
                word.add(cell)

                // Move to the next cell
                if ((y - 1) % 2 == 0) {
                    if (x == width) {
                        y++
                    } else {
                        x++
                    }
                } else {
                    if (x == 1) {
                        y++
                    } else {
                        x--
                    }
                }
            }
            turnsCluesList.add(Puzzle.Clue(Puzzle.Word(clueNumber, word), "$clueNumber", turnsClues[answerIndex]))
        }

        val grid = generateGrid(cellMap)

        return Puzzle(
                title,
                creator,
                copyright,
                description,
                grid,
                listOf(
                        Puzzle.ClueList("Turns", turnsCluesList),
                        Puzzle.ClueList("Twists", generateTwistsCluesList(grid))),
                crosswordSolverSettings = crosswordSolverSettings)
    }

    private fun generateGrid(cellMap: Map<Pair<Int, Int>, Puzzle.Cell>): List<List<Puzzle.Cell>> {
        val grid = mutableListOf<MutableList<Puzzle.Cell>>()
        (0 until height).forEach { y ->
            val row = mutableListOf<Puzzle.Cell>()
            (0 until width).forEach { x ->
                row.add(cellMap[x + 1 to y + 1] ?: throw IllegalStateException())
            }
            grid.add(row)
        }
        return grid
    }

    private fun generateTwistsCluesList(grid: List<List<Puzzle.Cell>>): List<Puzzle.Clue> {
        val twistsCluesList = mutableListOf<Puzzle.Clue>()
        var twistNumber = 0
        for (j in 0 until (height / twistBoxSize)) {
            for (i in 0 until (width / twistBoxSize)) {
                val cells = mutableListOf<Puzzle.Cell>()
                for (y in (j * twistBoxSize + 1)..((j + 1) * twistBoxSize)) {
                    for (x in (i * twistBoxSize + 1)..((i + 1) * twistBoxSize)) {
                        cells.add(grid[y - 1][x - 1])
                    }
                }
                val wordId = (1001 + (j * (width / twistBoxSize)) + i)
                twistsCluesList.add(Puzzle.Clue(Puzzle.Word(wordId, cells), "${twistNumber + 1}", twistsClues[twistNumber]))
                twistNumber++
            }
        }
        return twistsCluesList
    }

    companion object {
        @JsName("fromRawInput")
        fun fromRawInput(
                title: String,
                creator: String,
                copyright: String,
                description: String,
                width: String,
                height: String,
                twistBoxSize: String,
                turnsAnswers: String,
                turnsClues: String,
                twistsClues: String,
                lightTwistsColor: String,
                darkTwistsColor: String,
                crosswordSolverSettings: Puzzle.CrosswordSolverSettings): TwistsAndTurns {
            return TwistsAndTurns(
                    title.trim(),
                    creator.trim(),
                    copyright.trim(),
                    description.trim(),
                    width.toInt(),
                    height.toInt(),
                    twistBoxSize.toInt(),
                    turnsAnswers.trim().toUpperCase().replace("[^A-Z ]", "").split(" +".toRegex()),
                    turnsClues.trim().split("\n").map { it.trim() },
                    twistsClues.trim().split("\n").map { it.trim() },
                    lightTwistsColor,
                    darkTwistsColor,
                    crosswordSolverSettings)
        }
    }
}