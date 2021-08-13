package com.jeffpdavidson.kotwords.web

import com.jeffpdavidson.kotwords.model.JellyRoll
import com.jeffpdavidson.kotwords.model.Puzzle
import com.jeffpdavidson.kotwords.web.html.FormFields
import com.jeffpdavidson.kotwords.web.html.Html
import kotlinx.html.InputType
import kotlinx.html.div
import kotlin.js.Promise

internal class JellyRollForm {
    private val jpzForm = PuzzleFileForm("jelly-roll", ::createPuzzle)
    private val title: FormFields.InputField = FormFields.InputField("title")
    private val creator: FormFields.InputField = FormFields.InputField("creator")
    private val copyright: FormFields.InputField = FormFields.InputField("copyright")
    private val description: FormFields.TextBoxField = FormFields.TextBoxField("description")
    private val jellyRollAnswers: FormFields.TextBoxField = FormFields.TextBoxField("jelly-roll-answers")
    private val jellyRollClues: FormFields.TextBoxField = FormFields.TextBoxField("jelly-roll-clues")
    private val lightSquaresAnswers: FormFields.TextBoxField = FormFields.TextBoxField("light-squares-answers")
    private val lightSquaresClues: FormFields.TextBoxField = FormFields.TextBoxField("light-squares-clues")
    private val lightSquaresColor: FormFields.InputField = FormFields.InputField("light-squares-color")
    private val darkSquaresAnswers: FormFields.TextBoxField = FormFields.TextBoxField("dark-squares-answers")
    private val darkSquaresClues: FormFields.TextBoxField = FormFields.TextBoxField("dark-squares-clues")
    private val darkSquaresColor: FormFields.InputField = FormFields.InputField("dark-squares-color")
    private val combineJellyRollClues: FormFields.CheckBoxField = FormFields.CheckBoxField("combine-jelly-roll-clues")

    init {
        Html.renderPage {
            jpzForm.render(this, bodyBlock = {
                this@JellyRollForm.title.render(this, "Title")
                creator.render(this, "Creator (optional)")
                copyright.render(this, "Copyright (optional)")
                description.render(this, "Description (optional)") {
                    rows = "5"
                }
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

    private fun createPuzzle(): Promise<Puzzle> {
        val jellyRoll = JellyRoll(
            title = title.getValue(),
            creator = creator.getValue(),
            copyright = copyright.getValue(),
            description = description.getValue(),
            jellyRollAnswers = jellyRollAnswers.getValue().split("\\s+".toRegex()),
            jellyRollClues = jellyRollClues.getValue().split("\n").map { it.trim() },
            lightSquaresAnswers = lightSquaresAnswers.getValue().split("\\s+".toRegex()),
            lightSquaresClues = lightSquaresClues.getValue().split("\n").map { it.trim() },
            darkSquaresAnswers = darkSquaresAnswers.getValue().split("\\s+".toRegex()),
            darkSquaresClues = darkSquaresClues.getValue().split("\n").map { it.trim() },
            lightSquareBackgroundColor = lightSquaresColor.getValue(),
            darkSquareBackgroundColor = darkSquaresColor.getValue(),
            combineJellyRollClues = combineJellyRollClues.getValue(),
        )
        return Promise.resolve(jellyRoll.asPuzzle())
    }
}