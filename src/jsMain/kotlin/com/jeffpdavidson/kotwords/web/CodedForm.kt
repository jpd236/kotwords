package com.jeffpdavidson.kotwords.web

import com.jeffpdavidson.kotwords.KotwordsInternal
import com.jeffpdavidson.kotwords.formats.AcrossLite
import com.jeffpdavidson.kotwords.model.Coded
import com.jeffpdavidson.kotwords.model.Puzzle
import com.jeffpdavidson.kotwords.util.trimmedLines
import com.jeffpdavidson.kotwords.web.html.FormFields
import com.jeffpdavidson.kotwords.web.html.Html.renderPage
import kotlinx.html.classes

/** Form to convert Coded puzzles into JPZ files. */
@JsExport
@KotwordsInternal
class CodedForm {
    private val form = PuzzleFileForm("coded", ::createPuzzle)
    private val grid: FormFields.TextBoxField = FormFields.TextBoxField("grid")
    private val assignments: FormFields.InputField = FormFields.InputField("assignments")
    private val givens: FormFields.InputField = FormFields.InputField("givens")
    private val importPuzButton: FormFields.Button = FormFields.Button("import-puz")
    private val importPuzFile: FormFields.FileField = FormFields.FileField("import-puz-file")

    init {
        renderPage {
            form.render(this, bodyBlock = {
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
        if (assignments.value.isBlank()) {
            // While fromRawInput will generate and use an assignment if the provided one is blank, we proactively
            // generate it here so we can update the form with the result, ensuring the exact same output can be
            // reproduced.
            val gridChars = grid.value.uppercase().trimmedLines().map { line ->
                line.map { ch -> if (ch == '.') null else ch }
            }
            val generatedAssignments = Coded.generateAssignments(gridChars)
            assignments.value = generatedAssignments.joinToString("")
        }
        val coded = Coded.fromRawInput(
            title = form.title,
            creator = form.creator,
            copyright = form.copyright,
            description = form.description,
            grid = grid.value,
            assignments = assignments.value,
            givens = givens.value,
        )
        return coded.asPuzzle()
    }

    private suspend fun loadPuzFile(puzFile: ByteArray) {
        val puzzle = AcrossLite(puzFile).asPuzzle()
        form.title = puzzle.title
        form.creator = puzzle.creator
        form.copyright = puzzle.copyright
        form.description = puzzle.description
        grid.value = puzzle.grid.joinToString("\n") { row ->
            row.joinToString("") {
                if (it.cellType.isBlack()) {
                    "."
                } else {
                    it.solution.substring(0, 1)
                }
            }
        }
    }
}