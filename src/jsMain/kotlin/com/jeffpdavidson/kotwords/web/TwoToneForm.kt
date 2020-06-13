package com.jeffpdavidson.kotwords.web

import com.jeffpdavidson.kotwords.model.Puzzle
import com.jeffpdavidson.kotwords.model.TwoTone
import com.jeffpdavidson.kotwords.web.html.FormFields
import com.jeffpdavidson.kotwords.web.html.Html
import kotlinx.html.InputType
import kotlinx.html.div
import kotlin.js.Promise

class TwoToneForm {
    private val jpzForm = JpzForm(::createPuzzle)
    private val title: FormFields.InputField = FormFields.InputField("title")
    private val creator: FormFields.InputField = FormFields.InputField("creator")
    private val copyright: FormFields.InputField = FormFields.InputField("copyright")
    private val description: FormFields.TextBoxField = FormFields.TextBoxField("description")
    private val allSquaresAnswers: FormFields.TextBoxField = FormFields.TextBoxField("all-squares-answers")
    private val allSquaresClues: FormFields.TextBoxField = FormFields.TextBoxField("all-squares-clues")
    private val oddSquaresAnswers: FormFields.TextBoxField = FormFields.TextBoxField("odd-squares-answers")
    private val oddSquaresClues: FormFields.TextBoxField = FormFields.TextBoxField("odd-squares-clues")
    private val oddSquaresColor: FormFields.InputField = FormFields.InputField("odd-squares-color")
    private val evenSquaresAnswers: FormFields.TextBoxField = FormFields.TextBoxField("even-squares-answers")
    private val evenSquaresClues: FormFields.TextBoxField = FormFields.TextBoxField("even-squares-clues")
    private val evenSquaresColor: FormFields.InputField = FormFields.InputField("even-squares-color")

    init {
        Html.renderPage {
            jpzForm.render(this, bodyBlock = {
                this@TwoToneForm.title.render(this, "Title")
                creator.render(this, "Creator (optional)")
                copyright.render(this, "Copyright (optional)")
                description.render(this, "Description (optional)") {
                    rows = "5"
                }
                allSquaresAnswers.render(this, "All squares answers") {
                    placeholder = "In sequential order, separated by whitespace. " +
                            "Non-alphabetical characters are ignored."
                    rows = "5"
                }
                allSquaresClues.render(this, "All squares clues") {
                    placeholder = "One clue per row. Omit clue numbers."
                    rows = "10"
                }
                oddSquaresAnswers.render(this, "Odd squares answers (first, third, etc.)") {
                    placeholder = "In sequential order, separated by whitespace. " +
                            "Non-alphabetical characters are ignored."
                    rows = "5"
                }
                oddSquaresClues.render(this, "Odd squares clues") {
                    placeholder = "One clue per row. Omit clue numbers."
                    rows = "10"
                }
                evenSquaresAnswers.render(this, "Even squares answers (second, fourth, etc.)") {
                    placeholder = "In sequential order, separated by whitespace. " +
                            "Non-alphabetical characters are ignored."
                    rows = "5"
                }
                evenSquaresClues.render(this, "Even squares clues") {
                    placeholder = "One clue per row. Omit clue numbers."
                    rows = "10"
                }
            }, advancedOptionsBlock = {
                div(classes = "form-row") {
                    oddSquaresColor.render(this, "Odd squares color", flexCols = 6) {
                        type = InputType.color
                        value = "#C0C0C0"
                    }
                    evenSquaresColor.render(this, "Even squares color", flexCols = 6) {
                        type = InputType.color
                        value = "#FFFFFF"
                    }
                }
            })
        }
    }

    private fun createPuzzle(crosswordSolverSettings: Puzzle.CrosswordSolverSettings): Promise<Puzzle> {
        val twoTone = TwoTone(
                title = title.getValue(),
                creator = creator.getValue(),
                copyright = copyright.getValue(),
                description = description.getValue(),
                allSquaresAnswers = allSquaresAnswers.getValue().split(" +".toRegex()),
                allSquaresClues = allSquaresClues.getValue().split("\n").map { it.trim() },
                oddSquaresAnswers = oddSquaresAnswers.getValue().split(" +".toRegex()),
                oddSquaresClues = oddSquaresClues.getValue().split("\n").map { it.trim() },
                evenSquaresAnswers = evenSquaresAnswers.getValue().split(" +".toRegex()),
                evenSquaresClues = evenSquaresClues.getValue().split("\n").map { it.trim() })
        return Promise.resolve(twoTone.asPuzzle(
                oddSquareBackgroundColor = oddSquaresColor.getValue(),
                evenSquareBackgroundColor = evenSquaresColor.getValue(),
                crosswordSolverSettings = crosswordSolverSettings))
    }
}