package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.Encodings.decodeHtmlEntities
import com.jeffpdavidson.kotwords.formats.json.JsonSerializer
import com.jeffpdavidson.kotwords.formats.json.xwordinfo.XWordInfoJson
import com.jeffpdavidson.kotwords.model.Crossword
import com.jeffpdavidson.kotwords.model.Puzzle

/** Container for a puzzle in the XWord Info JSON format. */
class XWordInfo(private val json: String) : DelegatingPuzzleable() {

    override suspend fun getPuzzleable(): Puzzleable {
        val response = JsonSerializer.fromJson<XWordInfoJson.Response>(json)
        val grid = (0 until response.size.rows).map { y ->
            (0 until response.size.cols).map { x ->
                val index = y * response.size.cols + x
                val solution = response.grid[index]
                if (solution == ".") {
                    Puzzle.Cell(cellType = Puzzle.CellType.BLOCK)
                } else {
                    val backgroundShape =
                        if ((response.circles?.get(index) ?: 0) == 1) {
                            Puzzle.BackgroundShape.CIRCLE
                        } else {
                            Puzzle.BackgroundShape.NONE
                        }
                    Puzzle.Cell(
                        solution = solution,
                        backgroundShape = backgroundShape,
                    )
                }
            }
        }

        val author = decodeHtmlEntities(response.author)
        val creator =
            if (response.editor?.isNotEmpty() == true) {
                "$author / Edited by ${decodeHtmlEntities(response.editor)}"
            } else {
                author
            }

        return Crossword(
            title = decodeHtmlEntities(response.title),
            creator = creator,
            copyright = "\u00a9 ${decodeHtmlEntities(response.copyright)}",
            description = response.notepad?.let { decodeHtmlEntities(it) } ?: "",
            grid = grid,
            acrossClues = response.clues.across.associate {
                it.substringBefore(". ").toInt() to decodeHtmlEntities(it.substringAfter(". "))
            },
            downClues = response.clues.down.associate {
                it.substringBefore(". ").toInt() to decodeHtmlEntities(it.substringAfter(". "))
            },
            diagramless = response.type == "diagramless",
            hasHtmlClues = true,
        )
    }
}