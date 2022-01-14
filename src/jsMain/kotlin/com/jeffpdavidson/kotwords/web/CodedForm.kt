package com.jeffpdavidson.kotwords.web

import com.jeffpdavidson.kotwords.KotwordsInternal
import com.jeffpdavidson.kotwords.formats.AcrossLite
import com.jeffpdavidson.kotwords.model.Coded
import com.jeffpdavidson.kotwords.model.Puzzle
import com.jeffpdavidson.kotwords.web.html.FormFields
import com.jeffpdavidson.kotwords.web.html.Html.renderPage
import kotlinx.html.classes

/** Form to convert Coded puzzles into JPZ files. */
@JsExport
@KotwordsInternal
class CodedForm {
    private val form = PuzzleFileForm("coded", ::createPuzzle)
    private val title: FormFields.InputField = FormFields.InputField("title")
    private val creator: FormFields.InputField = FormFields.InputField("creator")
    private val copyright: FormFields.InputField = FormFields.InputField("copyright")
    private val description: FormFields.TextBoxField = FormFields.TextBoxField("description")
    private val grid: FormFields.TextBoxField = FormFields.TextBoxField("grid")
    private val assignments: FormFields.InputField = FormFields.InputField("assignments")
    private val givens: FormFields.InputField = FormFields.InputField("givens")
    private val importPuzButton: FormFields.Button = FormFields.Button("import-puz")
    private val importPuzFile: FormFields.FileField = FormFields.FileField("import-puz-file")

    init {
        renderPage {
            form.render(this, bodyBlock = {
                this@CodedForm.title.render(this, "Title")
                creator.render(this, "Creator (optional)")
                copyright.render(this, "Copyright (optional)")
                description.render(this, "Description (optional)") {
                    rows = "5"
                }
                grid.render(this, "Grid") {
                    rows = "13"
                    placeholder = "The solution grid. Use \".\" to represent black squares."
                }
                assignments.render(
                    this,
                    label = "Letter assignments (optional)",
                    help = "The first letter will be assigned the number 1 in the grid, the second 2, and so on. " +
                            "Leave this blank to generate a random assignment."
                ) {
                    placeholder = "In order, the letters to assign to each number, without spaces."
                }
                givens.render(this, "Given letters (optional)") {
                    placeholder = "Letters to prefill at the start of the puzzle (as hints)."
                }
            }, additionalButtonsBlock = {
                importPuzButton.render(this, "Import PUZ") {
                    classes = classes + "btn-secondary ml-3"
                }
                importPuzFile.renderAsHiddenSelector(this, ".puz", ::loadPuzFile)
            }, submitHandler = { submitter ->
                if (submitter == importPuzButton.button) {
                    importPuzFile.input.click()
                    true
                } else {
                    false
                }
            })
        }
    }

    private suspend fun createPuzzle(): Puzzle {
        if (assignments.getValue().isBlank()) {
            // While fromRawInput will generate and use an assignment if the provided one is blank, we proactively
            // generate it here so we can update the form with the result, ensuring the exact same output can be
            // reproduced.
            val gridChars = grid.getValue().lines().map { line ->
                line.trim().map { ch -> if (ch == '.') null else ch }
            }
            val generatedAssignments = Coded.generateAssignments(gridChars)
            assignments.setValue(generatedAssignments.joinToString(""))
        }
        val coded = Coded.fromRawInput(
            title = title.getValue(),
            creator = creator.getValue(),
            copyright = copyright.getValue(),
            description = description.getValue(),
            grid = grid.getValue(),
            assignments = assignments.getValue(),
            givens = givens.getValue(),
        )
        return coded.asPuzzle()
    }

    private suspend fun loadPuzFile(puzFile: ByteArray) {
        val puzzle = AcrossLite(puzFile).asPuzzle()
        title.setValue(puzzle.title)
        creator.setValue(puzzle.creator)
        copyright.setValue(puzzle.copyright)
        description.setValue(puzzle.description)
        grid.setValue(puzzle.grid.joinToString("\n") { row ->
            row.joinToString("") {
                if (it.cellType.isBlack()) {
                    "."
                } else {
                    it.solution.substring(0, 1)
                }
            }
        })
    }
}