package com.jeffpdavidson.kotwords.web

import com.jeffpdavidson.kotwords.KotwordsInternal
import com.jeffpdavidson.kotwords.formats.PdfFonts
import com.jeffpdavidson.kotwords.model.Cascades
import com.jeffpdavidson.kotwords.model.Puzzle
import com.jeffpdavidson.kotwords.util.trimmedLines
import com.jeffpdavidson.kotwords.web.html.FormFields
import com.jeffpdavidson.kotwords.web.html.Html
import kotlinx.html.InputType
import kotlinx.html.div

/** Form to convert Cascades puzzles into digital puzzle files. */
@JsExport
@KotwordsInternal
class CascadesForm {
    private val form = PuzzleFileForm("cascades", ::createPuzzle, createPdfFn = ::createPdf)
    private val grid: FormFields.TextBoxField = FormFields.TextBoxField("grid")
    private val cascadeClues: FormFields.TextBoxField = FormFields.TextBoxField("cascade-clues")
    private val rowClues: FormFields.TextBoxField = FormFields.TextBoxField("row-clues")
    private val includeRowNumbers: FormFields.CheckBoxField = FormFields.CheckBoxField("include-row-numbers")
    private val lightCascadeColor: FormFields.InputField = FormFields.InputField("light-cascade-color")
    private val darkCascadeColor: FormFields.InputField = FormFields.InputField("dark-cascade-color")

    init {
        Html.renderPage {
            form.render(this, bodyBlock = {
                grid.render(this, "Grid") {
                    placeholder =
                        "Letters of the grid, separated into rows. Grid must have width equal to height + 1. " +
                                "Use a period for empty corner cells. " +
                                "For rebuses, separate each cell in the row with whitespace."
                    rows = "12"
                }
                cascadeClues.render(this, "Cascade clues") {
                    placeholder =
                        "The clues for each cascade; one cascade per line. Ordered from upper-right to lower-left. " +
                                "Separate multiple clues for a cascade with a /."
                    rows = "11"
                }
                rowClues.render(this, "Row clues") {
                    placeholder =
                        "The clues for each row; one line per row. Separate multiple clues for a row with a /."
                    rows = "12"
                }
            }, advancedOptionsBlock = {
                includeRowNumbers.render(this, "Include row numbers") {
                    checked = true
                }
                div(classes = "form-row") {
                    lightCascadeColor.render(this, "Light cascade color", flexCols = 6) {
                        type = InputType.color
                        value = "#FFFFFF"
                    }
                    darkCascadeColor.render(this, "Dark cascade color", flexCols = 6) {
                        type = InputType.color
                        value = "#C0C0C0"
                    }
                }
            })
        }
    }

    private suspend fun createPuzzle(): Puzzle = createCascades().asPuzzle()

    private suspend fun createPdf(blackSquareLightnessAdjustment: Double): ByteArray =
        createCascades().asPdf(
            fontFamily = PdfFonts.NOTO_FONT_FAMILY,
            blackSquareLightnessAdjustment = blackSquareLightnessAdjustment,
        )

    private fun createCascades(): Cascades {
        return Cascades(
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
            rowClues = rowClues.value.trimmedLines().map { clues ->
                clues.split("/").map { it.trim() }
            },
            cascadeClues = cascadeClues.value.trimmedLines().map { clues ->
                clues.split("/").map { it.trim() }
            },
            includeRowNumbers = includeRowNumbers.value,
            lightCascadeColor = lightCascadeColor.value,
            darkCascadeColor = darkCascadeColor.value,
        )
    }
}
