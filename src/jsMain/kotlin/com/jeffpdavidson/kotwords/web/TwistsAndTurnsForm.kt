package com.jeffpdavidson.kotwords.web

import com.jeffpdavidson.kotwords.KotwordsInternal
import com.jeffpdavidson.kotwords.formats.PdfFonts
import com.jeffpdavidson.kotwords.model.Puzzle
import com.jeffpdavidson.kotwords.model.TwistsAndTurns
import com.jeffpdavidson.kotwords.util.trimmedLines
import com.jeffpdavidson.kotwords.web.html.FormFields
import com.jeffpdavidson.kotwords.web.html.Html.renderPage
import kotlinx.html.InputType
import kotlinx.html.div

/** Form to convert Twists and Turns puzzles into digital puzzle files. */
@JsExport
@KotwordsInternal
class TwistsAndTurnsForm {
    private val form = PuzzleFileForm("twists-and-turns", ::createPuzzle, createPdfFn = ::createPdf)
    private val width: FormFields.InputField = FormFields.InputField("width")
    private val height: FormFields.InputField = FormFields.InputField("height")
    private val twistBoxSize: FormFields.InputField = FormFields.InputField("twist-box-size")
    private val turnsAnswers: FormFields.TextBoxField = FormFields.TextBoxField("turns-answers")
    private val turnsClues: FormFields.TextBoxField = FormFields.TextBoxField("turns-clues")
    private val twistsClues: FormFields.TextBoxField = FormFields.TextBoxField("twists-clues")
    private val alphabetizeTwistsClues: FormFields.CheckBoxField = FormFields.CheckBoxField("alphabetize-twists-clues")
    private val lightTwistsColor: FormFields.InputField = FormFields.InputField("light-twists-color")
    private val darkTwistsColor: FormFields.InputField = FormFields.InputField("dark-twists-color")

    init {
        renderPage {
            form.render(this, bodyBlock = {
                div(classes = "form-row") {
                    width.render(this, "Width", flexCols = 4) {
                        type = InputType.number
                    }
                    height.render(this, "Height", flexCols = 4) {
                        type = InputType.number
                    }
                    twistBoxSize.render(this, "Twist width/height", flexCols = 4) {
                        type = InputType.number
                    }
                }
                turnsAnswers.render(this, "Turns answers") {
                    placeholder =
                        "In sequential order, separated by whitespace. Non-alphabetical characters are ignored."
                    rows = "2"
                }
                turnsClues.render(this, "Turns clues") {
                    placeholder = "One clue per row. Omit clue numbers."
                    rows = "10"
                }
                twistsClues.render(this, "Twists clues") {
                    placeholder = "Ordered from left-to-right, top-to-bottom. One clue per row. Omit clue numbers."
                    rows = "10"
                }
            }, advancedOptionsBlock = {
                alphabetizeTwistsClues.render(this, "Alphabetize twists clues (for PDFs)")
                div(classes = "form-row") {
                    lightTwistsColor.render(this, "Light twists color", flexCols = 6) {
                        type = InputType.color
                        value = "#FFFFFF"
                    }
                    darkTwistsColor.render(this, "Dark twists color", flexCols = 6) {
                        type = InputType.color
                        value = "#888888"
                    }
                }
            })
        }
    }

    private suspend fun createPuzzle(): Puzzle =
        createTwistsAndTurns(
            separateLightAndDarkTwists = false,
            numberTwists = true,
            sortTwists = false,
        ).asPuzzle()

    private suspend fun createPdf(blackSquareLightnessAdjustment: Float): ByteArray =
        createTwistsAndTurns(
            separateLightAndDarkTwists = true,
            numberTwists = false,
            sortTwists = alphabetizeTwistsClues.value,
        ).asPdf(
            fontFamily = PdfFonts.getNotoFontFamily(),
            blackSquareLightnessAdjustment = blackSquareLightnessAdjustment,
        )

    private fun createTwistsAndTurns(
        separateLightAndDarkTwists: Boolean,
        numberTwists: Boolean,
        sortTwists: Boolean
    ): TwistsAndTurns {
        return TwistsAndTurns(
            title = form.title,
            creator = form.creator,
            copyright = form.copyright,
            description = form.description,
            width = width.value.toInt(),
            height = height.value.toInt(),
            twistBoxSize = twistBoxSize.value.toInt(),
            turnsAnswers = turnsAnswers.value.uppercase().replace("[^A-Z\\s]".toRegex(), "")
                .split("\\s+".toRegex()),
            turnsClues = turnsClues.value.trimmedLines(),
            twistsClues = twistsClues.value.trimmedLines(),
            lightTwistsColor = lightTwistsColor.value,
            darkTwistsColor = darkTwistsColor.value,
            separateLightAndDarkTwists = separateLightAndDarkTwists,
            numberTwists = numberTwists,
            sortTwists = sortTwists,
        )
    }
}