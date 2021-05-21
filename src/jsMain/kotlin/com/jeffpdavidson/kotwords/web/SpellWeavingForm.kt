package com.jeffpdavidson.kotwords.web

import com.jeffpdavidson.kotwords.model.Puzzle
import com.jeffpdavidson.kotwords.model.SpellWeaving
import com.jeffpdavidson.kotwords.web.html.FormFields
import com.jeffpdavidson.kotwords.web.html.Html
import kotlin.js.Promise

internal class SpellWeavingForm {
    private val jpzForm = JpzForm(::createPuzzle)
    private val title: FormFields.InputField = FormFields.InputField("title")
    private val creator: FormFields.InputField = FormFields.InputField("creator")
    private val copyright: FormFields.InputField = FormFields.InputField("copyright")
    private val description: FormFields.TextBoxField = FormFields.TextBoxField("description")
    private val answers: FormFields.TextBoxField = FormFields.TextBoxField("answers")
    private val clues: FormFields.TextBoxField = FormFields.TextBoxField("clues")

    init {
        Html.renderPage {
            jpzForm.render(this, bodyBlock = {
                this@SpellWeavingForm.title.render(this, "Title")
                creator.render(this, "Creator (optional)")
                copyright.render(this, "Copyright (optional)")
                description.render(this, "Description (optional)") {
                    rows = "5"
                }
                answers.render(this, "Answers") {
                    placeholder = "In sequential order, separated by whitespace. " +
                            "Non-alphabetical characters are ignored."
                    rows = "5"
                }
                clues.render(this, "Clues") {
                    placeholder = "One clue per row. Omit clue numbers."
                    rows = "10"
                }
            })
        }
    }

    private fun createPuzzle(crosswordSolverSettings: Puzzle.CrosswordSolverSettings): Promise<Puzzle> {
        val spellWeaving = SpellWeaving(
            title = title.getValue(),
            creator = creator.getValue(),
            copyright = copyright.getValue(),
            description = description.getValue(),
            answers = answers.getValue().split("\\s+".toRegex()),
            clues = clues.getValue().split("\n").map { it.trim() })
        return Promise.resolve(spellWeaving.asPuzzle(crosswordSolverSettings = crosswordSolverSettings))
    }
}