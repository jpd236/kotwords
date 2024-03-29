package com.jeffpdavidson.kotwords.web

import com.jeffpdavidson.kotwords.KotwordsInternal
import com.jeffpdavidson.kotwords.formats.Rgz
import com.jeffpdavidson.kotwords.js.Interop
import com.jeffpdavidson.kotwords.model.Puzzle
import com.jeffpdavidson.kotwords.model.RowsGarden
import com.jeffpdavidson.kotwords.util.trimmedLines
import com.jeffpdavidson.kotwords.web.html.FormFields
import com.jeffpdavidson.kotwords.web.html.Html.renderPage
import com.jeffpdavidson.kotwords.web.html.Tabs
import com.jeffpdavidson.kotwords.web.html.Tabs.tabs
import kotlinx.html.FlowContent
import kotlinx.html.InputType
import kotlinx.html.div
import kotlinx.html.dom.append
import kotlinx.html.p

/** Form to convert Rows Garden puzzles into digital puzzle files. */
@JsExport
@KotwordsInternal
class RowsGardenForm {
    private val manualEntryForm = PuzzleFileForm(
        "rows-garden",
        ::createPuzzleFromManualEntry,
        id = "manual-entry",
    )
    private val rowClues: FormFields.TextBoxField = FormFields.TextBoxField("row-clues")
    private val rowAnswers: FormFields.TextBoxField = FormFields.TextBoxField("row-answers")
    private val lightClues: FormFields.TextBoxField = FormFields.TextBoxField("light-clues")
    private val lightAnswers: FormFields.TextBoxField = FormFields.TextBoxField("light-answers")
    private val mediumClues: FormFields.TextBoxField = FormFields.TextBoxField("medium-clues")
    private val mediumAnswers: FormFields.TextBoxField = FormFields.TextBoxField("medium-answers")
    private val darkClues: FormFields.TextBoxField = FormFields.TextBoxField("dark-clues")
    private val darkAnswers: FormFields.TextBoxField = FormFields.TextBoxField("dark-answers")
    private val manualEntryAdvancedOptions = RowsGardenAdvancedOptions("manual-entry")

    private val rgzFileForm = PuzzleFileForm(
        "rows-garden",
        ::createPuzzleFromRgzFile,
        { getRgzFileName() },
        id = "rgz-file",
        enableSaveData = false,
        enableMetadataInput = false,
    )
    private val file: FormFields.FileField = FormFields.FileField("file")
    private val rgzFileAdvancedOptions = RowsGardenAdvancedOptions("rgz-file")

    init {
        renderPage {
            append.p {
                +"Note: bloom colors are determined by making the top-left bloom light, cycling between light, medium, "
                +"and dark in the first column, and then ensuring that no two adjacent blooms have the same color."
            }
            append.tabs(Tabs.Tab("manual-entry-tab", "Form") {
                manualEntryForm.render(this, bodyBlock = {
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

    private suspend fun createPuzzleFromRgzFile(): Puzzle {
        val rgz = Interop.readBlob(file.value)
        return Rgz.fromRgzFile(rgz).asRowsGarden(
            lightBloomColor = rgzFileAdvancedOptions.lightBloomColor.value,
            mediumBloomColor = rgzFileAdvancedOptions.mediumBloomColor.value,
            darkBloomColor = rgzFileAdvancedOptions.darkBloomColor.value,
            addHyphenated = rgzFileAdvancedOptions.addAnnotations.value,
            addWordCount = rgzFileAdvancedOptions.addAnnotations.value,
        ).asPuzzle()
    }

    private suspend fun createPuzzleFromManualEntry(): Puzzle {
        val rowsGarden = RowsGarden(
            title = manualEntryForm.title,
            creator = manualEntryForm.creator,
            copyright = manualEntryForm.copyright,
            description = manualEntryForm.description,
            rows = rowClues.value.trimmedLines().zip(rowAnswers.value.uppercase().trimmedLines())
                .map { (clues, answers) ->
                    clues.split("/")
                        .zip(answers.split("/"))
                        .map { (clue, answer) -> RowsGarden.Entry(clue.trim(), answer.trim()) }
                },
            light = lightClues.value.trimmedLines().zip(lightAnswers.value.uppercase().trimmedLines())
                .map { (clue, answer) -> RowsGarden.Entry(clue, answer) },
            medium = mediumClues.value.trimmedLines().zip(mediumAnswers.value.uppercase().trimmedLines())
                .map { (clue, answer) -> RowsGarden.Entry(clue, answer) },
            dark = darkClues.value.trimmedLines().zip(darkAnswers.value.uppercase().trimmedLines())
                .map { (clue, answer) -> RowsGarden.Entry(clue, answer) },
            lightBloomColor = manualEntryAdvancedOptions.lightBloomColor.value,
            mediumBloomColor = manualEntryAdvancedOptions.mediumBloomColor.value,
            darkBloomColor = manualEntryAdvancedOptions.darkBloomColor.value,
            addHyphenated = manualEntryAdvancedOptions.addAnnotations.value,
            addWordCount = manualEntryAdvancedOptions.addAnnotations.value,
        )
        return rowsGarden.asPuzzle()
    }

    private fun getRgzFileName(): String {
        return file.value.name.removeSuffix(".rgz").removeSuffix(".rg")
    }
}

private class RowsGardenAdvancedOptions(val id: String) {
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