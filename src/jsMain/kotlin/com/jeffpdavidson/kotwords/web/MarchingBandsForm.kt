package com.jeffpdavidson.kotwords.web

import com.jeffpdavidson.kotwords.KotwordsInternal
import com.jeffpdavidson.kotwords.model.MarchingBands
import com.jeffpdavidson.kotwords.model.Puzzle
import com.jeffpdavidson.kotwords.util.trimmedLines
import com.jeffpdavidson.kotwords.web.html.FormFields
import com.jeffpdavidson.kotwords.web.html.Html
import kotlinx.html.InputType
import kotlinx.html.div

@JsExport
@KotwordsInternal
class MarchingBandsForm {
    private val form = PuzzleFileForm("marching-bands", ::createPuzzle)
    private val grid: FormFields.TextBoxField = FormFields.TextBoxField("grid")
    private val bandClues: FormFields.TextBoxField = FormFields.TextBoxField("band-clues")
    private val rowClues: FormFields.TextBoxField = FormFields.TextBoxField("row-clues")
    private val includeRowNumbers: FormFields.CheckBoxField = FormFields.CheckBoxField("include-row-numbers")
    private val lightBandColor: FormFields.InputField = FormFields.InputField("light-band-color")
    private val darkBandColor: FormFields.InputField = FormFields.InputField("dark-band-color")

    init {
        Html.renderPage {
            form.render(this, bodyBlock = {
                grid.render(this, "Grid") {
                    placeholder =
                        "Letters of the grid, separated into rows. Use a period for the middle square. " +
                                "For rebuses, separate each cell in the row with whitespace."
                    rows = "13"
                }
                bandClues.render(this, "Band clues") {
                    placeholder =
                        "The clues for each band; one band per row. Ordered from the outside in. Separate " +
                                "multiple clues for a band with a /."
                    rows = "6"
                }
                rowClues.render(this, "Row clues") {
                    placeholder =
                        "The clues for each row; one line per row. Separate multiple clues for a row with a /."
                    rows = "13"
                }
            }, advancedOptionsBlock = {
                includeRowNumbers.render(this, "Include row numbers") {
                    checked = true
                }
                div(classes = "form-row") {
                    lightBandColor.render(this, "Light band color", flexCols = 6) {
                        type = InputType.color
                        value = "#FFFFFF"
                    }
                    darkBandColor.render(this, "Dark band color", flexCols = 6) {
                        type = InputType.color
                        value = "#C0C0C0"
                    }
                }
            })
        }
    }

    private suspend fun createPuzzle(): Puzzle {
        val marchingBands = MarchingBands(
            title = form.title,
            creator = form.creator,
            copyright = form.copyright,
            description = form.description,
            grid = grid.value.uppercase().trimmedLines().map { row ->
                val cells = if (!row.contains("\\s+".toRegex())) {
                    // No spaces - assume each character is a cell.
                    row.toCharArray().map { "$it" }
                } else {
                    // Split row into cells by whitespace.
                    row.split("\\s+".toRegex())
                }
                cells.map { cell -> if (cell == ".") "" else cell }
            },
            bandClues = bandClues.value.trimmedLines().map { clues ->
                clues.split("/").map { it.trim() }
            },
            rowClues = rowClues.value.trimmedLines().map { clues ->
                clues.split("/").map { it.trim() }
            },
            includeRowNumbers = includeRowNumbers.value,
            lightBandColor = lightBandColor.value,
            darkBandColor = darkBandColor.value,
        )
        return marchingBands.asPuzzle()
    }
}