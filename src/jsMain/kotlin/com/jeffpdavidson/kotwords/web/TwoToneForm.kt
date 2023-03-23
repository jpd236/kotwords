package com.jeffpdavidson.kotwords.web

import com.jeffpdavidson.kotwords.KotwordsInternal
import com.jeffpdavidson.kotwords.model.Puzzle
import com.jeffpdavidson.kotwords.model.TwoTone
import com.jeffpdavidson.kotwords.util.trimmedLines
import com.jeffpdavidson.kotwords.web.html.FormFields
import com.jeffpdavidson.kotwords.web.html.Html
import kotlinx.html.InputType
import kotlinx.html.div

@JsExport
@KotwordsInternal
class TwoToneForm {
    private val form = PuzzleFileForm("two-tone", ::createPuzzle)
    private val allSquaresAnswers: FormFields.TextBoxField = FormFields.TextBoxField("all-squares-answers")
    private val allSquaresClues: FormFields.TextBoxField = FormFields.TextBoxField("all-squares-clues")
    private val oddSquaresAnswers: FormFields.TextBoxField = FormFields.TextBoxField("odd-squares-answers")
    private val oddSquaresClues: FormFields.TextBoxField = FormFields.TextBoxField("odd-squares-clues")
    private val oddSquaresColor: FormFields.InputField = FormFields.InputField("odd-squares-color")
    private val evenSquaresAnswers: FormFields.TextBoxField = FormFields.TextBoxField("even-squares-answers")
    private val evenSquaresClues: FormFields.TextBoxField = FormFields.TextBoxField("even-squares-clues")
    private val evenSquaresColor: FormFields.InputField = FormFields.InputField("even-squares-color")
    private val width: FormFields.InputField = FormFields.InputField("width")
    private val height: FormFields.InputField = FormFields.InputField("height")

    init {
        Html.renderPage {
            form.render(this, bodyBlock = {
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
                div(classes = "form-group") {
                    div(classes = "form-row mb-0") {
                        width.render(
                            this,
                            "Width (optional)",
                            help = "Width of the grid. By default, the smallest possible square grid is used.",
                            flexCols = 6,
                        ) {
                            type = InputType.number
                        }
                        height.render(
                            this,
                            "Height (optional)",
                            help = "Height of the grid. By default, the smallest possible square grid is used.",
                            flexCols = 6,
                        ) {
                            type = InputType.number
                        }
                    }
                }
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

    private suspend fun createPuzzle(): Puzzle {
        val twoTone = TwoTone(
            title = form.title,
            creator = form.creator,
            copyright = form.copyright,
            description = form.description,
            allSquaresAnswers = allSquaresAnswers.value.uppercase().split("\\s+".toRegex()),
            allSquaresClues = allSquaresClues.value.trimmedLines(),
            oddSquaresAnswers = oddSquaresAnswers.value.uppercase().split("\\s+".toRegex()),
            oddSquaresClues = oddSquaresClues.value.trimmedLines(),
            evenSquaresAnswers = evenSquaresAnswers.value.uppercase().split("\\s+".toRegex()),
            evenSquaresClues = evenSquaresClues.value.trimmedLines(),
            oddSquareBackgroundColor = oddSquaresColor.value,
            evenSquareBackgroundColor = evenSquaresColor.value,
            dimensions = width.value.ifEmpty { "0" }.toInt() to height.value.ifEmpty { "0" }.toInt(),
        )
        return twoTone.asPuzzle()
    }
}