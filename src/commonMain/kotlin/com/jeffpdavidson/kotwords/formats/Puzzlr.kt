package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.json.JsonSerializer
import com.jeffpdavidson.kotwords.formats.json.PuzzlrJson
import com.jeffpdavidson.kotwords.model.Crossword
import com.jeffpdavidson.kotwords.model.Puzzle

/** Container for a puzzle in the Puzzlr JSON format. */
class Puzzlr(private val json: String) : DelegatingPuzzleable() {
    override suspend fun getPuzzleable(): Puzzleable {
        val responses = JsonSerializer.fromJson<List<PuzzlrJson.Response>>(json)
        val response = responses.firstOrNull() ?: throw InvalidFormatException("No response data found")
        val puzzleData = response.result.data.data

        val grid = puzzleData.grid.map { row ->
            row.map { cell ->
                if (cell.isBlack) {
                    Puzzle.Cell(cellType = Puzzle.CellType.BLOCK)
                } else {
                    val backgroundShape = if (cell.isCircled) {
                        Puzzle.BackgroundShape.CIRCLE
                    } else {
                        Puzzle.BackgroundShape.NONE
                    }
                    Puzzle.Cell(
                        solution = cell.answer,
                        backgroundShape = backgroundShape,
                    )
                }
            }
        }

        return Crossword(
            title = puzzleData.title,
            creator = puzzleData.author,
            copyright = puzzleData.description ?: "",
            description = puzzleData.contestClueText ?: "",
            grid = grid,
            acrossClues = puzzleData.clues.across.associate { it.number to it.text },
            downClues = puzzleData.clues.down.associate { it.number to it.text },
        )
    }
}
