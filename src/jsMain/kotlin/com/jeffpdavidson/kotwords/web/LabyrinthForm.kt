package com.jeffpdavidson.kotwords.web

import com.jeffpdavidson.kotwords.KotwordsInternal
import com.jeffpdavidson.kotwords.model.Labyrinth
import com.jeffpdavidson.kotwords.model.Puzzle
import com.jeffpdavidson.kotwords.web.html.FormFields
import com.jeffpdavidson.kotwords.web.html.Html
import kotlin.js.Promise

@JsExport
@KotwordsInternal
class LabyrinthForm {
    private val jpzForm = PuzzleFileForm("labyrinth", ::createPuzzle)
    private val title: FormFields.InputField = FormFields.InputField("title")
    private val creator: FormFields.InputField = FormFields.InputField("creator")
    private val copyright: FormFields.InputField = FormFields.InputField("copyright")
    private val description: FormFields.TextBoxField = FormFields.TextBoxField("description")
    private val grid: FormFields.TextBoxField = FormFields.TextBoxField("grid")
    private val gridKey: FormFields.TextBoxField = FormFields.TextBoxField("grid-key")
    private val rowClues: FormFields.TextBoxField = FormFields.TextBoxField("row-clues")
    private val windingClues: FormFields.TextBoxField = FormFields.TextBoxField("winding-clues")
    private val alphabetizeWindingClues: FormFields.CheckBoxField =
        FormFields.CheckBoxField("alphabetize-winding-clues")

    init {
        Html.renderPage {
            jpzForm.render(this, bodyBlock = {
                this@LabyrinthForm.title.render(this, "Title")
                creator.render(this, "Creator (optional)")
                copyright.render(this, "Copyright (optional)")
                description.render(this, "Description (optional)") {
                    rows = "5"
                }
                grid.render(this, "Grid") {
                    placeholder = "Letters of the grid, separated into rows."
                    rows = "14"
                }
                gridKey.render(this, "Grid key") {
                    placeholder = "The numeric positions of each grid letter in the winding answer, in the same " +
                            "shape as the grid. Separate numbers with spaces."
                    rows = "14"
                }
                rowClues.render(this, "Row clues") {
                    placeholder =
                        "The clues for each row; one line per row. Separate multiple clues for a row with a /."
                    rows = "14"
                }
                windingClues.render(this, "Winding clues") {
                    placeholder = "The clues for the winding answers, separated with a /."
                    rows = "6"
                }
            }, advancedOptionsBlock = {
                alphabetizeWindingClues.render(this, "Alphabetize winding clues")
            })
        }
    }

    private fun createPuzzle(): Promise<Puzzle> {
        val labyrinth = Labyrinth(
            title = title.getValue(),
            creator = creator.getValue(),
            copyright = copyright.getValue(),
            description = description.getValue(),
            grid = grid.getValue().split("\n").map { row ->
                row.uppercase().replace("[^A-Z.]".toRegex(), "").toList()
            },
            gridKey = gridKey.getValue().split("\n").map { row ->
                row.trim().split(" ").map { it.toInt() }
            },
            rowClues = rowClues.getValue().split("\n").map { clues ->
                clues.trim().split("/").map { it.trim() }
            },
            windingClues = windingClues.getValue().split("/").map { it.trim() },
            alphabetizeWindingClues = alphabetizeWindingClues.getValue(),
        )
        return Promise.resolve(labyrinth.asPuzzle())
    }
}