package com.jeffpdavidson.kotwords.web

import com.jeffpdavidson.kotwords.model.MarchingBands
import com.jeffpdavidson.kotwords.model.Puzzle
import com.jeffpdavidson.kotwords.web.html.FormFields.checkField
import com.jeffpdavidson.kotwords.web.html.FormFields.inputField
import com.jeffpdavidson.kotwords.web.html.FormFields.textBoxField
import com.jeffpdavidson.kotwords.web.html.Html
import com.jeffpdavidson.kotwords.web.html.Html.getElementById
import kotlinx.html.InputType
import kotlinx.html.div
import kotlinx.html.dom.append
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLTextAreaElement
import kotlin.js.Promise

private const val ID_TITLE = "title"
private const val ID_CREATOR = "creator"
private const val ID_COPYRIGHT = "copyright"
private const val ID_DESCRIPTION = "description"
private const val ID_GRID = "grid"
private const val ID_BAND_CLUES = "band-clues"
private const val ID_ROW_CLUES = "row-clues"
private const val ID_INCLUDE_ROW_NUMBERS = "include-row-numbers"
private const val ID_LIGHT_BAND_COLOR = "light-band-color"
private const val ID_DARK_BAND_COLOR = "dark-band-color"

class MarchingBandsForm {
    private val jpzForm = JpzForm(::createPuzzle)
    private val title: HTMLInputElement by getElementById(ID_TITLE)
    private val creator: HTMLInputElement by getElementById(ID_CREATOR)
    private val copyright: HTMLInputElement by getElementById(ID_COPYRIGHT)
    private val description: HTMLTextAreaElement by getElementById(ID_DESCRIPTION)
    private val grid: HTMLTextAreaElement by getElementById(ID_GRID)
    private val bandClues: HTMLTextAreaElement by getElementById(ID_BAND_CLUES)
    private val rowClues: HTMLTextAreaElement by getElementById(ID_ROW_CLUES)
    private val includeRowNumbers: HTMLInputElement by getElementById(ID_INCLUDE_ROW_NUMBERS)
    private val lightBandColor: HTMLInputElement by getElementById(ID_LIGHT_BAND_COLOR)
    private val darkBandColor: HTMLInputElement by getElementById(ID_DARK_BAND_COLOR)

    init {
        Html.renderPage {
            with(jpzForm) {
                append.jpzForm(bodyBlock = {
                    inputField(ID_TITLE, "Title")
                    inputField(ID_CREATOR, "Creator (optional)")
                    inputField(ID_COPYRIGHT, "Copyright (optional)")
                    textBoxField(ID_DESCRIPTION, "Description (optional)") {
                        rows = "5"
                    }
                    textBoxField(ID_GRID, "Grid") {
                        placeholder = "Letters of the grid, separated into rows. Use a period for the middle square."
                        rows = "13"
                    }
                    textBoxField(ID_BAND_CLUES, "Band clues") {
                        placeholder =
                                "The clues for each band; one band per row. Ordered from the outside in. Separate " +
                                        "multiple clues for a band with a /."
                        rows = "6"
                    }
                    textBoxField(ID_ROW_CLUES, "Row clues") {
                        placeholder =
                                "The clues for each row; one line per row. Separate multiple clues for a row with a /."
                        rows = "13"
                    }
                }, advancedOptionsBlock = {
                    checkField(ID_INCLUDE_ROW_NUMBERS, "Include row numbers") {
                        checked = true
                    }
                    div(classes = "form-row") {
                        inputField(ID_LIGHT_BAND_COLOR, "Light band color", flexCols = 6) {
                            type = InputType.color
                            value = "#FFFFFF"
                        }
                        inputField(ID_DARK_BAND_COLOR, "Dark band color", flexCols = 6) {
                            type = InputType.color
                            value = "#C0C0C0"
                        }
                    }
                })
            }
        }
    }

    private fun createPuzzle(crosswordSolverSettings: Puzzle.CrosswordSolverSettings): Promise<Puzzle> {
        val marchingBands = MarchingBands(
                title = title.value.trim(),
                creator = creator.value.trim(),
                copyright = copyright.value.trim(),
                description = description.value.trim(),
                grid = grid.value.trim().split("\n").map { row ->
                    row.toUpperCase().replace("[^A-Z.]".toRegex(), "").map { ch -> if (ch == '.') null else ch }
                },
                bandClues = bandClues.value.trim().split("\n").map { clues ->
                    clues.trim().split("/").map { it.trim() }
                },
                rowClues = rowClues.value.trim().split("\n").map { clues ->
                    clues.trim().split("/").map { it.trim() }
                })
        return Promise.resolve(marchingBands.asPuzzle(
                includeRowNumbers = includeRowNumbers.checked,
                lightBandColor = lightBandColor.value.trim(),
                darkBandColor = darkBandColor.value.trim(),
                crosswordSolverSettings = crosswordSolverSettings))
    }
}