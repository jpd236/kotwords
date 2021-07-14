package com.jeffpdavidson.kotwords.web

import com.jeffpdavidson.kotwords.model.AroundTheBend
import com.jeffpdavidson.kotwords.model.Puzzle
import com.jeffpdavidson.kotwords.web.html.FormFields
import com.jeffpdavidson.kotwords.web.html.Html
import kotlin.js.Promise

internal class AroundTheBendForm {
    private val jpzForm = PuzzleFileForm(::createPuzzle)
    private val title: FormFields.InputField = FormFields.InputField("title")
    private val creator: FormFields.InputField = FormFields.InputField("creator")
    private val copyright: FormFields.InputField = FormFields.InputField("copyright")
    private val description: FormFields.TextBoxField = FormFields.TextBoxField("description")
    private val rows: FormFields.TextBoxField = FormFields.TextBoxField("rows")
    private val clues: FormFields.TextBoxField = FormFields.TextBoxField("clues")

    init {
        Html.renderPage {
            jpzForm.render(this, bodyBlock = {
                this@AroundTheBendForm.title.render(this, "Title")
                creator.render(this, "Creator (optional)")
                copyright.render(this, "Copyright (optional)")
                description.render(this, "Description (optional)") {
                    rows = "5"
                }
                rows.render(this, "Rows") {
                    placeholder = "The letters in each row, one line per row."
                    rows = "10"
                }
                clues.render(this, "Clues") {
                    placeholder = "One clue per row. Omit clue numbers."
                    rows = "10"
                }
            })
        }
    }

    private fun createPuzzle(crosswordSolverSettings: Puzzle.CrosswordSolverSettings): Promise<Puzzle> {
        val aroundTheBend = AroundTheBend(
            title = title.getValue(),
            creator = creator.getValue(),
            copyright = copyright.getValue(),
            description = description.getValue(),
            rows = rows.getValue().split("\n").map { it.trim() },
            clues = clues.getValue().split("\n").map { it.trim() })
        return Promise.resolve(aroundTheBend.asPuzzle(crosswordSolverSettings = crosswordSolverSettings))
    }
}