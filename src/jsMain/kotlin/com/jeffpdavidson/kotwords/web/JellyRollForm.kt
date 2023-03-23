package com.jeffpdavidson.kotwords.web

import com.jeffpdavidson.kotwords.KotwordsInternal
import com.jeffpdavidson.kotwords.model.JellyRoll
import com.jeffpdavidson.kotwords.model.Puzzle
import com.jeffpdavidson.kotwords.util.trimmedLines
import com.jeffpdavidson.kotwords.web.html.FormFields
import com.jeffpdavidson.kotwords.web.html.Html
import kotlinx.html.InputType
import kotlinx.html.div

@JsExport
@KotwordsInternal
class JellyRollForm {
    private val form = PuzzleFileForm("jelly-roll", ::createPuzzle)
    private val jellyRollAnswers: FormFields.TextBoxField = FormFields.TextBoxField("jelly-roll-answers")
    private val jellyRollClues: FormFields.TextBoxField = FormFields.TextBoxField("jelly-roll-clues")
    private val lightSquaresAnswers: FormFields.TextBoxField = FormFields.TextBoxField("light-squares-answers")
    private val lightSquaresClues: FormFields.TextBoxField = FormFields.TextBoxField("light-squares-clues")
    private val lightSquaresColor: FormFields.InputField = FormFields.InputField("light-squares-color")
    private val darkSquaresAnswers: FormFields.TextBoxField = FormFields.TextBoxField("dark-squares-answers")
    private val darkSquaresClues: FormFields.TextBoxField = FormFields.TextBoxField("dark-squares-clues")
    private val darkSquaresColor: FormFields.InputField = FormFields.InputField("dark-squares-color")
    private val combineJellyRollClues: FormFields.CheckBoxField = FormFields.CheckBoxField("combine-jelly-roll-clues")
    private val width: FormFields.InputField = FormFields.InputField("width")
    private val height: FormFields.InputField = FormFields.InputField("height")

    init {
        Html.renderPage {
            form.render(this, bodyBlock = {
                jellyRollAnswers.render(this, "Jelly roll answers") {
                    placeholder = "In sequential order, separated by whitespace. " +
                            "Non-alphabetical characters are ignored."
                    rows = "5"
                }
                jellyRollClues.render(this, "Jelly roll clues") {
                    placeholder = "One clue per row. Omit clue numbers."
                    rows = "10"
                }
                lightSquaresAnswers.render(this, "Light squares answers (first, fourth, fifth, seventh, etc.)") {
                    placeholder = "In sequential order, separated by whitespace. " +
                            "Non-alphabetical characters are ignored."
                    rows = "5"
                }
                lightSquaresClues.render(this, "Light squares clues") {
                    placeholder = "One clue per row. Omit clue numbers."
                    rows = "10"
                }
                darkSquaresAnswers.render(this, "Dark squares answers (second, third, sixth, etc.)") {
                    placeholder = "In sequential order, separated by whitespace. " +
                            "Non-alphabetical characters are ignored."
                    rows = "5"
                }
                darkSquaresClues.render(this, "Dark squares clues") {
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
                combineJellyRollClues.render(this, "Combine Jelly Roll clues into one large clue")
                div(classes = "form-row") {
                    lightSquaresColor.render(this, "Light squares color", flexCols = 6) {
                        type = InputType.color
                        value = "#FFFFFF"
                    }
                    darkSquaresColor.render(this, "Dark squares color", flexCols = 6) {
                        type = InputType.color
                        value = "#C0C0C0"
                    }
                }
            })
        }
    }

    private suspend fun createPuzzle(): Puzzle {
        val jellyRoll = JellyRoll(
            title = form.title,
            creator = form.creator,
            copyright = form.copyright,
            description = form.description,
            jellyRollAnswers = jellyRollAnswers.value.uppercase().split("\\s+".toRegex()),
            jellyRollClues = jellyRollClues.value.trimmedLines(),
            lightSquaresAnswers = lightSquaresAnswers.value.uppercase().split("\\s+".toRegex()),
            lightSquaresClues = lightSquaresClues.value.trimmedLines(),
            darkSquaresAnswers = darkSquaresAnswers.value.uppercase().split("\\s+".toRegex()),
            darkSquaresClues = darkSquaresClues.value.trimmedLines(),
            lightSquareBackgroundColor = lightSquaresColor.value,
            darkSquareBackgroundColor = darkSquaresColor.value,
            combineJellyRollClues = combineJellyRollClues.value,
            dimensions = width.value.ifEmpty { "0" }.toInt() to height.value.ifEmpty { "0" }.toInt(),
        )
        return jellyRoll.asPuzzle()
    }
}