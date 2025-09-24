package com.jeffpdavidson.kotwords.web

import com.jeffpdavidson.kotwords.KotwordsInternal
import com.jeffpdavidson.kotwords.model.Puzzle
import com.jeffpdavidson.kotwords.model.SpellWeaving
import com.jeffpdavidson.kotwords.util.trimmedAlphabeticalWords
import com.jeffpdavidson.kotwords.util.trimmedLines
import com.jeffpdavidson.kotwords.web.html.FormFields
import com.jeffpdavidson.kotwords.web.html.Html

@JsExport
@KotwordsInternal
class SpellWeavingForm {
    private val form = PuzzleFileForm("spell-weaving", ::createPuzzle)
    private val answers: FormFields.TextBoxField = FormFields.TextBoxField("answers")
    private val clues: FormFields.TextBoxField = FormFields.TextBoxField("clues")

    init {
        Html.renderPage {
            form.render(this, bodyBlock = {
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

    private suspend fun createPuzzle(): Puzzle {
        val spellWeaving = SpellWeaving(
            title = form.title,
            creator = form.creator,
            copyright = form.copyright,
            description = form.description,
            answers = answers.value.trimmedAlphabeticalWords(),
            clues = clues.value.trimmedLines(),
        )
        return spellWeaving.asPuzzle()
    }
}