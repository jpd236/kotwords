package com.jeffpdavidson.kotwords.web

import com.jeffpdavidson.kotwords.formats.PdfFonts
import com.jeffpdavidson.kotwords.model.Puzzle
import com.jeffpdavidson.kotwords.model.TwistsAndTurns
import com.jeffpdavidson.kotwords.web.html.FormFields
import com.jeffpdavidson.kotwords.web.html.Html.renderPage
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import kotlinx.html.InputType
import kotlinx.html.div
import kotlin.js.Promise

/** Form to convert Twists and Turns puzzles into digital files. */
internal class TwistsAndTurnsForm {
    private val puzzleFileForm = PuzzleFileForm(::createPuzzle, createPdfFn = ::createPdf)
    private val title: FormFields.InputField = FormFields.InputField("title")
    private val creator: FormFields.InputField = FormFields.InputField("creator")
    private val copyright: FormFields.InputField = FormFields.InputField("copyright")
    private val description: FormFields.TextBoxField = FormFields.TextBoxField("description")
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
            puzzleFileForm.render(this, bodyBlock = {
                this@TwistsAndTurnsForm.title.render(this, "Title")
                creator.render(this, "Creator (optional)")
                copyright.render(this, "Copyright (optional)")
                description.render(this, "Description (optional)") {
                    rows = "5"
                }
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

    private fun createPuzzle(crosswordSolverSettings: Puzzle.CrosswordSolverSettings): Promise<Puzzle> =
        Promise.resolve(createTwistsAndTurns(crosswordSolverSettings).asPuzzle())

    private fun createPdf(
        crosswordSolverSettings: Puzzle.CrosswordSolverSettings,
        blackSquareLightnessAdjustment: Float
    ): Promise<ByteArray> =
        GlobalScope.promise {
            createTwistsAndTurns(crosswordSolverSettings).asPdf(
                fontFamily = PdfFonts.getNotoFontFamily(),
                blackSquareLightnessAdjustment = blackSquareLightnessAdjustment,
                sortTwists = alphabetizeTwistsClues.getValue()
            )
        }

    private fun createTwistsAndTurns(crosswordSolverSettings: Puzzle.CrosswordSolverSettings): TwistsAndTurns {
        return TwistsAndTurns(
            title = title.getValue(),
            creator = creator.getValue(),
            copyright = copyright.getValue(),
            description = description.getValue(),
            width = width.getValue().toInt(),
            height = height.getValue().toInt(),
            twistBoxSize = twistBoxSize.getValue().toInt(),
            turnsAnswers = turnsAnswers.getValue().uppercase().replace("[^A-Z\\s]".toRegex(), "")
                .split("\\s+".toRegex()),
            turnsClues = turnsClues.getValue().split("\n").map { it.trim() },
            twistsClues = twistsClues.getValue().split("\n").map { it.trim() },
            lightTwistsColor = lightTwistsColor.getValue(),
            darkTwistsColor = darkTwistsColor.getValue(),
            crosswordSolverSettings = crosswordSolverSettings
        )
    }
}