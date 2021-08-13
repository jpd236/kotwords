package com.jeffpdavidson.kotwords.web

import com.jeffpdavidson.kotwords.model.MarchingBands
import com.jeffpdavidson.kotwords.model.Puzzle
import com.jeffpdavidson.kotwords.web.html.FormFields
import com.jeffpdavidson.kotwords.web.html.Html
import kotlinx.html.InputType
import kotlinx.html.div
import kotlin.js.Promise

internal class MarchingBandsForm {
    private val jpzForm = PuzzleFileForm("marching-bands", ::createPuzzle)
    private val title: FormFields.InputField = FormFields.InputField("title")
    private val creator: FormFields.InputField = FormFields.InputField("creator")
    private val copyright: FormFields.InputField = FormFields.InputField("copyright")
    private val description: FormFields.TextBoxField = FormFields.TextBoxField("description")
    private val grid: FormFields.TextBoxField = FormFields.TextBoxField("grid")
    private val bandClues: FormFields.TextBoxField = FormFields.TextBoxField("band-clues")
    private val rowClues: FormFields.TextBoxField = FormFields.TextBoxField("row-clues")
    private val includeRowNumbers: FormFields.CheckBoxField = FormFields.CheckBoxField("include-row-numbers")
    private val lightBandColor: FormFields.InputField = FormFields.InputField("light-band-color")
    private val darkBandColor: FormFields.InputField = FormFields.InputField("dark-band-color")

    init {
        Html.renderPage {
            jpzForm.render(this, bodyBlock = {
                this@MarchingBandsForm.title.render(this, "Title")
                creator.render(this, "Creator (optional)")
                copyright.render(this, "Copyright (optional)")
                description.render(this, "Description (optional)") {
                    rows = "5"
                }
                grid.render(this, "Grid") {
                    placeholder = "Letters of the grid, separated into rows. Use a period for the middle square."
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

    private fun createPuzzle(): Promise<Puzzle> {
        val marchingBands = MarchingBands(
            title = title.getValue(),
            creator = creator.getValue(),
            copyright = copyright.getValue(),
            description = description.getValue(),
            grid = grid.getValue().split("\n").map { row ->
                row.uppercase().replace("[^A-Z.]".toRegex(), "").map { ch -> if (ch == '.') null else ch }
            },
            bandClues = bandClues.getValue().split("\n").map { clues ->
                clues.trim().split("/").map { it.trim() }
            },
            rowClues = rowClues.getValue().split("\n").map { clues ->
                clues.trim().split("/").map { it.trim() }
            },
            includeRowNumbers = includeRowNumbers.getValue(),
            lightBandColor = lightBandColor.getValue(),
            darkBandColor = darkBandColor.getValue(),
        )
        return Promise.resolve(marchingBands.asPuzzle())
    }
}