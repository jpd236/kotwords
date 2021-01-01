package com.jeffpdavidson.kotwords.web

import com.jeffpdavidson.kotwords.formats.RowsGarden
import com.jeffpdavidson.kotwords.js.Interop
import com.jeffpdavidson.kotwords.model.Puzzle
import com.jeffpdavidson.kotwords.web.html.FormFields
import com.jeffpdavidson.kotwords.web.html.Html.renderPage
import com.jeffpdavidson.kotwords.web.html.Tabs
import com.jeffpdavidson.kotwords.web.html.Tabs.tabs
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.await
import kotlinx.coroutines.promise
import kotlinx.html.FlowContent
import kotlinx.html.InputType
import kotlinx.html.div
import kotlinx.html.dom.append
import kotlinx.html.p
import kotlin.js.Promise

/** Form to convert Rows Garden puzzles into JPZ files. */
class RowsGardenForm {
    private val manualEntryForm = JpzForm(::createPuzzleFromManualEntry, id = "manual-entry")
    private val title: FormFields.InputField = FormFields.InputField("title")
    private val author: FormFields.InputField = FormFields.InputField("author")
    private val copyright: FormFields.InputField = FormFields.InputField("copyright")
    private val notes: FormFields.TextBoxField = FormFields.TextBoxField("notes")
    private val rowClues: FormFields.TextBoxField = FormFields.TextBoxField("row-clues")
    private val rowAnswers: FormFields.TextBoxField = FormFields.TextBoxField("row-answers")
    private val lightClues: FormFields.TextBoxField = FormFields.TextBoxField("light-clues")
    private val lightAnswers: FormFields.TextBoxField = FormFields.TextBoxField("light-answers")
    private val mediumClues: FormFields.TextBoxField = FormFields.TextBoxField("medium-clues")
    private val mediumAnswers: FormFields.TextBoxField = FormFields.TextBoxField("medium-answers")
    private val darkClues: FormFields.TextBoxField = FormFields.TextBoxField("dark-clues")
    private val darkAnswers: FormFields.TextBoxField = FormFields.TextBoxField("dark-answers")
    private val manualEntryAdvancedOptions = AdvancedOptions("manual-entry")

    private val rgzFileForm = JpzForm(::createPuzzleFromRgzFile, { getRgzFileName() }, id = "rgz-file")
    private val file: FormFields.FileField = FormFields.FileField("file")
    private val rgzFileAdvancedOptions = AdvancedOptions("rgz-file")

    init {
        renderPage {
            append.p {
                +"Note: bloom colors are determined by making the top-left bloom light, cycling between light, medium, "
                +"and dark in the first column, and then ensuring that no two adjacent blooms have the same color."
            }
            append.tabs(Tabs.Tab("manual-entry-tab", "Form") {
                manualEntryForm.render(this, bodyBlock = {
                    this@RowsGardenForm.title.render(this, "Title")
                    author.render(this, "Author (optional)")
                    copyright.render(this, "Copyright (optional)")
                    notes.render(this, "Notes (optional)") {
                        rows = "5"
                    }
                    div(classes = "form-row") {
                        rowClues.render(this, "Row clues", flexCols = 6) {
                            rows = "12"
                            placeholder =
                                "The clues for each row; one line per row. Separate multiple clues for a row " +
                                        "with a /."
                        }
                        rowAnswers.render(this, "Row answers", flexCols = 6) {
                            rows = "12"
                            placeholder =
                                "The answers for each row; one line per row. Separate multiple answers for a " +
                                        "row with a /."
                        }
                    }
                    div(classes = "form-row") {
                        lightClues.render(this, "Light clues", flexCols = 6) {
                            rows = "14"
                            placeholder =
                                "The light bloom clues; one answer per line."
                        }
                        lightAnswers.render(this, "Light answers", flexCols = 6) {
                            rows = "14"
                            placeholder =
                                "The light bloom answers; one answer per line."
                        }
                    }
                    div(classes = "form-row") {
                        mediumClues.render(this, "Medium clues", flexCols = 6) {
                            rows = "14"
                            placeholder =
                                "The medium bloom clues; one answer per line."
                        }
                        mediumAnswers.render(this, "Medium answers", flexCols = 6) {
                            rows = "14"
                            placeholder =
                                "The medium bloom answers; one answer per line."
                        }
                    }
                    div(classes = "form-row") {
                        darkClues.render(this, "Dark clues", flexCols = 6) {
                            rows = "10"
                            placeholder =
                                "The dark bloom clues; one answer per line."
                        }
                        darkAnswers.render(this, "Dark answers", flexCols = 6) {
                            rows = "10"
                            placeholder =
                                "The dark bloom answers; one answer per line."
                        }
                    }
                }, advancedOptionsBlock = manualEntryAdvancedOptions.block)
            }, Tabs.Tab("rgz-file-tab", "RG or RGZ file") {
                rgzFileForm.render(this, bodyBlock = {
                    file.render(this, "RG or RGZ file")
                }, advancedOptionsBlock = rgzFileAdvancedOptions.block)
            })
        }
    }

    private class AdvancedOptions(val id: String) {
        val addAnnotations: FormFields.CheckBoxField = FormFields.CheckBoxField(elementId("add-annotations"))
        val lightBloomColor: FormFields.InputField = FormFields.InputField(elementId("light-bloom-color"))
        val mediumBloomColor: FormFields.InputField = FormFields.InputField(elementId("medium-bloom-color"))
        val darkBloomColor: FormFields.InputField = FormFields.InputField(elementId("dark-bloom-color"))

        val block: FlowContent.() -> Unit = {
            addAnnotations.render(this, "Add clue annotations (e.g. \"hyph.\", \"2 wds.\")") {
                checked = true
            }
            div(classes = "form-row") {
                lightBloomColor.render(this, "Light bloom color", flexCols = 4) {
                    type = InputType.color
                    value = "#FFFFFF"
                }
                mediumBloomColor.render(this, "Medium bloom color", flexCols = 4) {
                    type = InputType.color
                    value = "#C3C8FA"
                }
                darkBloomColor.render(this, "Dark bloom color", flexCols = 4) {
                    type = InputType.color
                    value = "#5765F7"
                }
            }
        }

        private fun elementId(elementId: String) = if (id.isNotEmpty()) "$id-$elementId" else elementId
    }

    private fun createPuzzleFromRgzFile(crosswordSolverSettings: Puzzle.CrosswordSolverSettings): Promise<Puzzle> {
        return GlobalScope.promise {
            val rgz = Interop.readFile(file.getValue()).await()
            asPuzzle(RowsGarden.parse(rgz), rgzFileAdvancedOptions, crosswordSolverSettings)
        }
    }

    private fun createPuzzleFromManualEntry(crosswordSolverSettings: Puzzle.CrosswordSolverSettings): Promise<Puzzle> {
        val rowsGarden = RowsGarden(
            title = title.getValue(),
            author = author.getValue(),
            copyright = copyright.getValue(),
            notes = notes.getValue(),
            rows = rowClues.getValue().split("\n").zip(rowAnswers.getValue().split("\n"))
                .map { (clues, answers) ->
                    clues.trim().split("/")
                        .zip(answers.trim().split("/"))
                        .map { (clue, answer) -> RowsGarden.Entry(clue.trim(), answer.trim()) }
                },
            light = lightClues.getValue().split("\n").zip(lightAnswers.getValue().split("\n"))
                .map { (clue, answer) -> RowsGarden.Entry(clue.trim(), answer.trim()) },
            medium = mediumClues.getValue().split("\n").zip(mediumAnswers.getValue().split("\n"))
                .map { (clue, answer) -> RowsGarden.Entry(clue.trim(), answer.trim()) },
            dark = darkClues.getValue().split("\n").zip(darkAnswers.getValue().split("\n"))
                .map { (clue, answer) -> RowsGarden.Entry(clue.trim(), answer.trim()) })
        return Promise.resolve(asPuzzle(rowsGarden, manualEntryAdvancedOptions, crosswordSolverSettings))
    }

    private fun asPuzzle(
        rowsGarden: RowsGarden, advancedOptions: AdvancedOptions,
        crosswordSolverSettings: Puzzle.CrosswordSolverSettings
    ): Puzzle {
        return rowsGarden.asPuzzle(
            lightBloomColor = advancedOptions.lightBloomColor.getValue(),
            mediumBloomColor = advancedOptions.mediumBloomColor.getValue(),
            darkBloomColor = advancedOptions.darkBloomColor.getValue(),
            addHyphenated = advancedOptions.addAnnotations.getValue(),
            addWordCount = advancedOptions.addAnnotations.getValue(),
            crosswordSolverSettings = crosswordSolverSettings
        )
    }

    private fun getRgzFileName(): String {
        return file.getValue().name.replace(".rgz", ".jpz").replace(".rg", ".jpz")
    }
}