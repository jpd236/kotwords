package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.json.GuardianJson
import com.jeffpdavidson.kotwords.formats.json.JsonSerializer
import com.jeffpdavidson.kotwords.model.Crossword
import com.jeffpdavidson.kotwords.model.Puzzle

class Guardian(
    private val json: String,
    private val copyright: String = "",
) : DelegatingPuzzleable() {
    override suspend fun getPuzzleable(): Puzzleable {
        val data = JsonSerializer.fromJson<GuardianJson.Data>(json)
        val cells = mutableMapOf<Pair<Int, Int>, Puzzle.Cell>()
        val acrossClues = mutableMapOf<Int, String>()
        val downClues = mutableMapOf<Int, String>()
        data.entries.forEach { entry ->
            val isAcross = entry.direction == "across"
            val clues = if (isAcross) acrossClues else downClues
            clues[entry.number] = entry.clue
            repeat(entry.length) { i ->
                val x = entry.position.x + if (isAcross) i else 0
                val y = entry.position.y + if (!isAcross) i else 0
                val solution = if (entry.solution.isNotEmpty()) "${entry.solution[i]}" else ""
                val existingCell = cells[x to y]
                require(existingCell == null || existingCell.solution == solution) {
                    "Solution mismatch in cell ($x, $y)"
                }
                cells[x to y] = Puzzle.Cell(
                    number = if (i == 0) "${entry.number}" else "",
                    solution = solution,
                )
            }
        }
        val grid = (0 until data.dimensions.rows).map { y ->
            (0 until data.dimensions.cols).map { x ->
                val cell = cells[x to y]
                if (cell != null) {
                    cell
                } else {
                    Puzzle.Cell(cellType = Puzzle.CellType.BLOCK)
                }
            }
        }
        return Crossword(
            title = data.name,
            creator = data.creator.name,
            copyright = copyright,
            description = data.instructions,
            grid = grid,
            acrossClues = acrossClues,
            downClues = downClues,
            hasHtmlClues = true,
        )
    }
}