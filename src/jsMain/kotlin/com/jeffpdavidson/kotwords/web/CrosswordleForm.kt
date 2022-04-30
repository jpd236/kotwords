package com.jeffpdavidson.kotwords.web

import com.jeffpdavidson.kotwords.KotwordsInternal
import com.jeffpdavidson.kotwords.formats.Pdf.asPdf
import com.jeffpdavidson.kotwords.formats.PdfFonts
import com.jeffpdavidson.kotwords.model.Crosswordle
import com.jeffpdavidson.kotwords.model.Puzzle
import com.jeffpdavidson.kotwords.util.trimmedLines
import com.jeffpdavidson.kotwords.web.html.FormFields
import com.jeffpdavidson.kotwords.web.html.Html.renderPage

@JsExport
@KotwordsInternal
class CrosswordleForm {
    private val form = PuzzleFileForm("crosswordle", ::createPuzzle, createPdfFn = ::createPdf)
    private val grid = FormFields.TextBoxField("grid")
    private val answer = FormFields.InputField("answer")
    private val acrossClues = FormFields.TextBoxField("across-clues")
    private val downClues = FormFields.TextBoxField("down-clues")

    init {
        renderPage {
            form.render(this, bodyBlock = {
                grid.render(this, "Grid") {
                    placeholder = "The solution grid. Omit the answer to the Wordle."
                    rows = "5"
                }
                answer.render(this, "Answer") {
                    placeholder = "Answer to the Wordle puzzle."
                }
                acrossClues.render(this, "Across clues") {
                    placeholder = "One clue per row. Omit clue numbers."
                    rows = "5"
                }
                downClues.render(this, "Down clues") {
                    placeholder = "One clue per row. Omit clue numbers."
                    rows = "5"
                }
            })
        }
    }

    private suspend fun createPuzzle(): Puzzle =
        Crosswordle(
            title = form.title,
            creator = form.creator,
            copyright = form.copyright,
            description = form.description,
            grid = grid.value.uppercase().trimmedLines().map { it.toList() },
            answer = answer.value.uppercase(),
            acrossClues = acrossClues.value.trimmedLines(),
            downClues = downClues.value.trimmedLines(),
        ).asPuzzle()

    private suspend fun createPdf(blackSquareLightnessAdjustment: Float): ByteArray =
        createPuzzle().asPdf(
            fontFamily = PdfFonts.getNotoFontFamily(),
            blackSquareLightnessAdjustment = blackSquareLightnessAdjustment,
        )
}