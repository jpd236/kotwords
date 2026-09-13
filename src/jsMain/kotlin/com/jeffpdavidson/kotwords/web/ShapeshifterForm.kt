package com.jeffpdavidson.kotwords.web

import com.jeffpdavidson.kotwords.KotwordsInternal
import com.jeffpdavidson.kotwords.formats.PdfFonts
import com.jeffpdavidson.kotwords.model.Puzzle
import com.jeffpdavidson.kotwords.model.Shapeshifter
import com.jeffpdavidson.kotwords.util.trimmedLines
import com.jeffpdavidson.kotwords.web.html.FormFields
import com.jeffpdavidson.kotwords.web.html.Html

/** Form to convert Shapeshifter puzzles into digital puzzle files. */
@JsExport
@KotwordsInternal
class ShapeshifterForm {
    private val form = PuzzleFileForm("shapeshifter", ::createPuzzle, createPdfFn = ::createPdf)
    private val shorts: FormFields.TextBoxField = FormFields.TextBoxField("shorts")
    private val longsAnswers: FormFields.TextBoxField = FormFields.TextBoxField("longs-answers")
    private val shortsClues: FormFields.TextBoxField = FormFields.TextBoxField("shorts-clues")
    private val longsClues: FormFields.TextBoxField = FormFields.TextBoxField("longs-clues")
    private val labelClues: FormFields.CheckBoxField = FormFields.CheckBoxField("label-clues")

    init {
        Html.renderPage {
            form.render(this, bodyBlock = {
                shorts.render(this, "Short answers") {
                    placeholder = "The short answers; one line per short."
                    rows = "12"
                }
                longsAnswers.render(this, "Long answers (optional)") {
                    placeholder =
                        "The long answers; one line per long. " +
                                "If provided, the short and long answers will be checked for consistency."
                    rows = "11"
                }
                shortsClues.render(this, "Short clues") {
                    placeholder = "The short clues; one line per short."
                    rows = "12"
                }
                longsClues.render(this, "Long clues") {
                    placeholder = "The long clues; one line per long."
                    rows = "11"
                }
            }, advancedOptionsBlock = {
                labelClues.render(
                    this,
                    "Label clues (unlabeled requires Ipuz and the Crossword Nexus or squares.io solver)",
                ) {
                    checked = true
                }
            })
        }
    }

    private suspend fun createPuzzle(): Puzzle = createShapeshifter().asPuzzle()

    private suspend fun createPdf(blackSquareLightnessAdjustment: Double): ByteArray =
        createShapeshifter().asPdf(
            fontFamily = PdfFonts.NOTO_FONT_FAMILY,
            blackSquareLightnessAdjustment = blackSquareLightnessAdjustment,
        )

    private fun createShapeshifter(): Shapeshifter {
        return Shapeshifter(
            title = form.title,
            creator = form.creator,
            copyright = form.copyright,
            description = form.description,
            shortsAnswers = shorts.value.uppercase().trimmedLines(),
            shortsClues = shortsClues.value.trimmedLines(),
            longsClues = longsClues.value.trimmedLines(),
            longsAnswers = longsAnswers.value.uppercase().trimmedLines(),
            labelClues = labelClues.value,
        )
    }
}
