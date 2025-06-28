package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.json.JsonSerializer
import com.jeffpdavidson.kotwords.formats.json.WallStreetJournalJson
import com.jeffpdavidson.kotwords.formats.json.WashingtonPostJson
import com.jeffpdavidson.kotwords.model.Crossword
import com.jeffpdavidson.kotwords.model.Puzzle

/** Container for a puzzle in the Washington Post format. */
class WashingtonPost(
    private val json: String,
) : DelegatingPuzzleable() {

    override suspend fun getPuzzleable(): Puzzleable {
        val puzzle = JsonSerializer.fromJson<WashingtonPostJson.Puzzle>(json)
        val grid =
            puzzle.cells.map { cell ->
                val borderDirections =
                    setOfNotNull(
                        if (cell.bars and 1 != 0) Puzzle.BorderDirection.TOP else null,
                        if (cell.bars and 2 != 0) Puzzle.BorderDirection.LEFT else null,
                        if (cell.bars and 4 != 0) Puzzle.BorderDirection.RIGHT else null,
                        if (cell.bars and 8 != 0) Puzzle.BorderDirection.BOTTOM else null,
                    )
                if (cell.type == "locked" || cell.type == "void") {
                    Puzzle.Cell(
                        cellType = if (cell.type == "locked") Puzzle.CellType.BLOCK else Puzzle.CellType.VOID,
                        backgroundColor = cell.background,
                        borderDirections = borderDirections,
                    )
                } else {
                    val backgroundShape =
                        if (cell.circle) Puzzle.BackgroundShape.CIRCLE else Puzzle.BackgroundShape.NONE
                    Puzzle.Cell(
                        solution = cell.answer,
                        number = cell.number,
                        backgroundShape = backgroundShape,
                        backgroundColor = cell.background,
                        borderDirections = borderDirections,
                    )
                }
            }.chunked(puzzle.width)
        val clues = puzzle.words.partition { it.direction == "across" }
        val acrossClues = mutableMapOf<Int, String>()
        val downClues = mutableMapOf<Int, String>()
        var acrossClueIndex = 0
        var downClueIndex = 0
        Crossword.forEachNumberedCell(grid) { _, _, clueNumber, isAcross, isDown ->
            if (isAcross) {
                acrossClues[clueNumber] =
                    if (acrossClueIndex < clues.first.size) clues.first[acrossClueIndex++].clue else "-"
            }
            if (isDown) {
                downClues[clueNumber] =
                    if (downClueIndex < clues.second.size) clues.second[downClueIndex++].clue else "-"
            }
        }
        return Crossword(
            title = puzzle.title,
            creator = puzzle.creator,
            copyright = puzzle.copyright.lines().last(),
            description = puzzle.description,
            grid = grid,
            acrossClues = acrossClues,
            downClues = downClues,
        )
    }
}