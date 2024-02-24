package com.jeffpdavidson.kotwords.web

import com.jeffpdavidson.kotwords.KotwordsInternal
import com.jeffpdavidson.kotwords.model.Patchwork
import com.jeffpdavidson.kotwords.model.Puzzle
import com.jeffpdavidson.kotwords.util.trimmedLines
import com.jeffpdavidson.kotwords.web.html.FormFields
import com.jeffpdavidson.kotwords.web.html.Html
import kotlinx.html.js.onChangeFunction

@JsExport
@KotwordsInternal
class PatchworkForm {
    private val form = PuzzleFileForm("patchwork", ::createPuzzle)
    private val grid: FormFields.TextBoxField = FormFields.TextBoxField("grid")
    private val pieceNumbers: FormFields.TextBoxField = FormFields.TextBoxField("piece-numbers")
    private val rowClues: FormFields.TextBoxField = FormFields.TextBoxField("row-clues")
    private val pieceClues: FormFields.TextBoxField = FormFields.TextBoxField("piece-clues")
    private val labelPieces: FormFields.CheckBoxField = FormFields.CheckBoxField("label-pieces")

    init {
        Html.renderPage {
            form.render(this, bodyBlock = {
                grid.render(this, "Grid") {
                    placeholder = "Letters of the grid, separated into rows."
                    rows = "13"
                }
                pieceNumbers.render(this, "Piece numbers") {
                    placeholder =
                        "The piece number of each cell, in the same as the grid. Separate numbers with spaces."
                    rows = "13"
                }
                rowClues.render(this, "Row clues") {
                    placeholder =
                        "The clues for each row; one line per row. Separate multiple clues for a row with a /."
                    rows = "13"
                }
                pieceClues.render(this, "Piece clues") {
                    placeholder =
                        "The clues for each piece; one line per piece. Assigned to pieces in order of piece number."
                    rows = "13"
                }
            }, advancedOptionsBlock = {
                labelPieces.render(
                    this,
                    "Label pieces (unlabeled requires Ipuz and the Crossword Nexus or squares.io solver)"
                ) {
                    checked = true
                }
            })
        }
    }

    private suspend fun createPuzzle(): Puzzle {
        val patchwork = Patchwork(
            title = form.title,
            creator = form.creator,
            copyright = form.copyright,
            description = form.description,
            grid = grid.value.uppercase().trimmedLines().map { row ->
                row.replace("[^A-Z.]".toRegex(), "").toList()
            },
            rowClues = rowClues.value.trimmedLines().map { clues ->
                clues.split("/").map { it.trim() }
            },
            pieceClues = pieceClues.value.trimmedLines(),
            pieceNumbers = pieceNumbers.value.trimmedLines().map { row ->
                row.split(" ").map { it.toInt() }
            },
            labelPieces = labelPieces.value,
        )
        return patchwork.asPuzzle()
    }
}