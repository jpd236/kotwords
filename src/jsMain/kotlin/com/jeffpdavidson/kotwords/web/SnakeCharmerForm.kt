package com.jeffpdavidson.kotwords.web

import com.jeffpdavidson.kotwords.KotwordsInternal
import com.jeffpdavidson.kotwords.model.Puzzle
import com.jeffpdavidson.kotwords.model.SnakeCharmer
import com.jeffpdavidson.kotwords.web.html.FormFields
import com.jeffpdavidson.kotwords.web.html.Html
import kotlinx.html.classes
import kotlin.js.Promise

@JsExport
@KotwordsInternal
class SnakeCharmerForm {
    private val jpzForm = PuzzleFileForm("snake-charmer", ::createPuzzle)
    private val title: FormFields.InputField = FormFields.InputField("title")
    private val creator: FormFields.InputField = FormFields.InputField("creator")
    private val copyright: FormFields.InputField = FormFields.InputField("copyright")
    private val description: FormFields.TextBoxField = FormFields.TextBoxField("description")
    private val answers: FormFields.TextBoxField = FormFields.TextBoxField("answers")
    private val clues: FormFields.TextBoxField = FormFields.TextBoxField("clues")
    private val gridShape: FormFields.TextBoxField = FormFields.TextBoxField("grid-shape")

    init {
        Html.renderPage {
            jpzForm.render(this, bodyBlock = {
                this@SnakeCharmerForm.title.render(this, "Title")
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
                gridShape.render(this, "Grid shape", "Use '*' characters to represent white squares.") {
                    rows = "10"
                    classes = classes + "text-monospace"
                    +"""
                     | ************
                     | *          *
                     | *          *
                     | *          *
                     | * **********
                     | * *
                     | * *
                     | * *
                     | * **********
                     | *          *
                     | *          *
                     | ********** *
                     |          * *
                     |          * *
                     |          * *
                     |*********** *
                     |*           *
                     |*           *
                     |*           *
                     |*************
                    """.trimMargin()
                }
            })
        }
    }

    private fun createPuzzle(): Promise<Puzzle> {
        val grid = gridShape.getValue(trim = false).lines()
        require(grid.isNotEmpty()) {
            "Grid shape is required"
        }
        var y = 0
        var x = grid[0].indexOfFirst { it == '*' }
        require(x >= 0) {
            "First row of grid must contain a white square ('*')"
        }
        val gridCoordinates = linkedSetOf<Pair<Int, Int>>()
        do {
            gridCoordinates += x to y
            val nextPoint = listOf(x + 1 to y, x to y + 1, x - 1 to y, x to y - 1).find { (i, j) ->
                j >= 0 && j < grid.size && i >= 0 && i < grid[j].length && !gridCoordinates.contains(i to j)
                        && grid[j][i] == '*'
            } ?: break
            x = nextPoint.first
            y = nextPoint.second
        } while (true)
        val snakeCharmer = SnakeCharmer(
            title = title.getValue(),
            creator = creator.getValue(),
            copyright = copyright.getValue(),
            description = description.getValue(),
            answers = answers.getValue().split("\\s+".toRegex()),
            clues = clues.getValue().split("\n").map { it.trim() },
            gridCoordinates = gridCoordinates.toList()
        )
        return Promise.resolve(snakeCharmer.asPuzzle())
    }
}