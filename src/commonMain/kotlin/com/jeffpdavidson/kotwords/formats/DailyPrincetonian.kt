package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.json.DailyPrincetonianJson
import com.jeffpdavidson.kotwords.formats.json.JsonSerializer
import com.jeffpdavidson.kotwords.model.Crossword
import com.jeffpdavidson.kotwords.model.Puzzle

class DailyPrincetonian(val crosswordJson: String, val authorsJson: String, val cluesJson: String) :
    DelegatingPuzzleable() {
    override suspend fun getPuzzleable(): Puzzleable {
        val crossword = JsonSerializer.fromJson<DailyPrincetonianJson.Crossword>(crosswordJson)
        val authors = JsonSerializer.fromJson<List<DailyPrincetonianJson.Author>>(authorsJson)
        val clues = JsonSerializer.fromJson<List<DailyPrincetonianJson.Clue>>(cluesJson)

        val gridMap = mutableMapOf<Pair<Int, Int>, Puzzle.Cell>()
        val acrossClues = mutableMapOf<Pair<Int, Int>, String>()
        val downClues = mutableMapOf<Pair<Int, Int>, String>()
        clues.forEach { clue ->
            val clueMap = if (clue.isAcross) {
                acrossClues
            } else {
                downClues
            }
            var x = clue.x
            var y = clue.y
            clueMap[x to y] = clue.clue
            clue.answer.forEach { ch ->
                val isCircled = if (ch.isLowerCase()) {
                    Puzzle.BackgroundShape.CIRCLE
                } else {
                    Puzzle.BackgroundShape.NONE
                }
                gridMap[x to y] = Puzzle.Cell(
                    solution = "${ch.uppercaseChar()}",
                    backgroundShape = isCircled,
                )
                if (clue.isAcross) {
                    x++
                } else {
                    y++
                }
            }
        }
        val width = gridMap.maxOf { it.key.first } + 1
        val height = gridMap.maxOf { it.key.second } + 1
        val grid = (0 until height).map { y ->
            (0 until width).map { x ->
                gridMap[x to y] ?: Puzzle.Cell(cellType = Puzzle.CellType.BLOCK)
            }
        }

        val acrossClueMap = mutableMapOf<Int, String>()
        val downClueMap = mutableMapOf<Int, String>()
        Crossword.forEachNumberedCell(grid) { x, y, clueNumber, isAcross, isDown ->
            if (isAcross) {
                acrossClueMap[clueNumber] = acrossClues[x to y] ?: ""
            }
            if (isDown) {
                downClueMap[clueNumber] = downClues[x to y] ?: ""
            }
        }

        return Crossword(
            title = crossword.title,
            creator = authors.joinToString(" and ") { "${it.firstName} ${it.lastName}" },
            copyright = "",
            description = "",
            grid = grid,
            acrossClues = acrossClueMap,
            downClues = downClueMap,
            hasHtmlClues = false,
        )
    }
}