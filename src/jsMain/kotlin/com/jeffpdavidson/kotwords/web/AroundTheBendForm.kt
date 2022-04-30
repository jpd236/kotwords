package com.jeffpdavidson.kotwords.web

import com.jeffpdavidson.kotwords.KotwordsInternal
import com.jeffpdavidson.kotwords.model.AroundTheBend
import com.jeffpdavidson.kotwords.model.Puzzle
import com.jeffpdavidson.kotwords.util.trimmedLines
import com.jeffpdavidson.kotwords.web.html.FormFields
import com.jeffpdavidson.kotwords.web.html.Html

@JsExport
@KotwordsInternal
class AroundTheBendForm {
    private val form = PuzzleFileForm("around-the-bend", ::createPuzzle)
    private val rows: FormFields.TextBoxField = FormFields.TextBoxField("rows")
    private val clues: FormFields.TextBoxField = FormFields.TextBoxField("clues")

    init {
        Html.renderPage {
            form.render(this, bodyBlock = {
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

    private suspend fun createPuzzle(): Puzzle {
        val aroundTheBend = AroundTheBend(
            title = form.title,
            creator = form.creator,
            copyright = form.copyright,
            description = form.description,
            rows = rows.value.uppercase().trimmedLines(),
            clues = clues.value.trimmedLines(),
        )
        return aroundTheBend.asPuzzle()
    }
}