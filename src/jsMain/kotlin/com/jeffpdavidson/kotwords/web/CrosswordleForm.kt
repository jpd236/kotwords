package com.jeffpdavidson.kotwords.web

import com.jeffpdavidson.kotwords.KotwordsInternal
import com.jeffpdavidson.kotwords.formats.Pdf.asPdf
import com.jeffpdavidson.kotwords.formats.PdfFonts
import com.jeffpdavidson.kotwords.model.Crosswordle
import com.jeffpdavidson.kotwords.model.Puzzle
import com.jeffpdavidson.kotwords.web.html.FormFields
import com.jeffpdavidson.kotwords.web.html.Html.renderPage

@JsExport
@KotwordsInternal
class CrosswordleForm {
    private val puzzleFileForm = PuzzleFileForm("crosswordle", ::createPuzzle, createPdfFn = ::createPdf)
    private val title = FormFields.InputField("title")
    private val creator = FormFields.InputField("creator")
    private val copyright = FormFields.InputField("copyright")
    private val description = FormFields.TextBoxField("description")
    private val grid = FormFields.TextBoxField("grid")
    private val answer = FormFields.InputField("answer")
    private val acrossClues = FormFields.TextBoxField("across-clues")
    private val downClues = FormFields.TextBoxField("down-clues")

    init {
        renderPage {
            puzzleFileForm.render(this, bodyBlock = {
                this@CrosswordleForm.title.render(this, "Title")
                creator.render(this, "Creator (optional)")
                copyright.render(this, "Copyright (optional)")
                description.render(this, "Description (optional)") {
                    rows = "5"
                }
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

    private fun createPuzzle(): Puzzle =
        Crosswordle(
            title = title.getValue(),
            creator = creator.getValue(),
            copyright = copyright.getValue(),
            description = description.getValue(),
            grid = grid.getValue().split("\n").map { it.trim().toList() },
            answer = answer.getValue(),
            acrossClues = acrossClues.getValue().split("\n").map { it.trim() },
            downClues = downClues.getValue().split("\n").map { it.trim() },
        ).asPuzzle()

    private suspend fun createPdf(blackSquareLightnessAdjustment: Float): ByteArray =
        createPuzzle().asPdf(
            fontFamily = PdfFonts.getNotoFontFamily(),
            blackSquareLightnessAdjustment = blackSquareLightnessAdjustment,
        )
}