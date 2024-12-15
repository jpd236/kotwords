package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.json.CrosswordrJson
import com.jeffpdavidson.kotwords.formats.json.JsonSerializer
import com.jeffpdavidson.kotwords.model.Puzzle

/** Container for a puzzle in the Crosswordr JSON format. */
class Crosswordr(val json: String) : DelegatingPuzzleable() {
    override suspend fun getPuzzleable(): Puzzleable {
        val puzzle = JsonSerializer.fromJson<CrosswordrJson.Response>(json).data.puzzleV2.puzzle
        val grid = puzzle.content.cells.chunked(puzzle.width).map { row ->
            row.map { cell ->
                if (cell.isBlack) {
                    Puzzle.Cell(cellType = Puzzle.CellType.BLOCK)
                } else {
                    val backgroundShape =
                        if (cell.circled) Puzzle.BackgroundShape.CIRCLE else Puzzle.BackgroundShape.NONE
                    Puzzle.Cell(
                        solution = cell.solution ?: "",
                        backgroundColor = cell.backgroundColor ?: "",
                        number = "${cell.start ?: ""}",
                        backgroundShape = backgroundShape,
                    )
                }
            }
        }
        val (acrossClues, acrossWords) = toPuzzleClues(puzzle.width, puzzle.content.clues.across, 0)
        val (downClues, downWords) = toPuzzleClues(puzzle.width, puzzle.content.clues.down, 1000)
        return Puzzle(
            title = toHtml(puzzle.title ?: ""),
            creator = toHtml(listOfNotNull(puzzle.byline, puzzle.editedBy?.ifEmpty { null }).joinToString(" / ")),
            copyright = "",
            description = toHtml(puzzle.description ?: ""),
            grid = grid,
            clues = listOf(
                Puzzle.ClueList(title = "<b>Across</b>", clues = acrossClues),
                Puzzle.ClueList(title = "<b>Down</b>", clues = downClues),
            ),
            words = acrossWords + downWords,
            hasHtmlClues = true,
            completionMessage = toHtml(puzzle.postSolveNote ?: ""),
        )
    }

    private fun toPuzzleClues(
        gridWidth: Int,
        data: CrosswordrJson.Response.Data.PuzzleV2.Puzzle.Content.Clues.ClueList,
        wordStartIndex: Int
    ): Pair<List<Puzzle.Clue>, List<Puzzle.Word>> {
        val clues = mutableListOf<Puzzle.Clue>()
        val words = mutableListOf<Puzzle.Word>()
        data.data.forEach { clue ->
            val wordId = wordStartIndex + clue.index
            clues.add(
                Puzzle.Clue(
                    wordId = wordId,
                    number = "${clue.index}",
                    text = toHtml(clue.clue),
                )
            )
            words.add(Puzzle.Word(
                id = wordId,
                cells = clue.cells.map { cell ->
                    Puzzle.Coordinate(x = cell % gridWidth, y = cell / gridWidth)
                }
            ))
        }
        return clues to words
    }

    private fun toHtml(string: String): String {
        return string.replace("&", "&amp;")
    }
}