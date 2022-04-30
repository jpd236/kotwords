package com.jeffpdavidson.kotwords.web

import com.jeffpdavidson.kotwords.KotwordsInternal
import com.jeffpdavidson.kotwords.model.Puzzle
import com.jeffpdavidson.kotwords.model.Spiral
import com.jeffpdavidson.kotwords.util.trimmedLines
import com.jeffpdavidson.kotwords.web.html.FormFields
import com.jeffpdavidson.kotwords.web.html.Html

@JsExport
@KotwordsInternal
class SpiralForm {
    private val form = PuzzleFileForm("spiral", ::createPuzzle)
    private val inwardAnswers: FormFields.TextBoxField = FormFields.TextBoxField("inward-answers")
    private val inwardClues: FormFields.TextBoxField = FormFields.TextBoxField("inward-clues")
    private val outwardAnswers: FormFields.TextBoxField = FormFields.TextBoxField("outward-answers")
    private val outwardClues: FormFields.TextBoxField = FormFields.TextBoxField("outward-clues")
    private val inwardCells: FormFields.TextBoxField = FormFields.TextBoxField("inward-cells")

    init {
        Html.renderPage {
            form.render(this, bodyBlock = {
                inwardAnswers.render(this, "Inward answers") {
                    placeholder = "In sequential order, separated by whitespace. " +
                            "Non-alphabetical characters are ignored."
                    rows = "5"
                }
                inwardClues.render(this, "Inward clues") {
                    placeholder = "One clue per row. Omit clue numbers."
                    rows = "10"
                }
                outwardAnswers.render(this, "Outward answers") {
                    placeholder = "In sequential order, separated by whitespace. " +
                            "Non-alphabetical characters are ignored."
                    rows = "5"
                }
                outwardClues.render(this, "Outward clues") {
                    placeholder = "One clue per row. Omit clue numbers."
                    rows = "10"
                }
                inwardCells.render(this, "Inward cells (optional)") {
                    placeholder = "Each cell of the spiral, in sequential order, separated by whitespace. " +
                            "Non-alphabetical characters are ignored. " +
                            "Defaults to each letter of the inward answers. " +
                            "May be used to place more than one letter in some or all cells, " +
                            "e.g. for a Crushword Spiral."
                    rows = "5"
                }
            })
        }
    }

    private suspend fun createPuzzle(): Puzzle {
        val spiral = Spiral(
            title = form.title,
            creator = form.creator,
            copyright = form.copyright,
            description = form.description,
            inwardAnswers = inwardAnswers.value.uppercase().split("\\s+".toRegex()),
            inwardClues = inwardClues.value.trimmedLines(),
            outwardAnswers = outwardAnswers.value.uppercase().split("\\s+".toRegex()),
            outwardClues = outwardClues.value.trimmedLines(),
            inwardCellsInput = if (inwardCells.value.isBlank()) {
                listOf()
            } else {
                inwardCells.value.uppercase().split("\\s+".toRegex())
            },
        )
        return spiral.asPuzzle()
    }
}