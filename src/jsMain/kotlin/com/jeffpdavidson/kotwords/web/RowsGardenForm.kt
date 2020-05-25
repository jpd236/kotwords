package com.jeffpdavidson.kotwords.web

import com.jeffpdavidson.kotwords.formats.RowsGarden
import com.jeffpdavidson.kotwords.js.Interop
import com.jeffpdavidson.kotwords.model.Puzzle
import com.jeffpdavidson.kotwords.web.html.FormFields.checkField
import com.jeffpdavidson.kotwords.web.html.FormFields.fileField
import com.jeffpdavidson.kotwords.web.html.FormFields.inputField
import com.jeffpdavidson.kotwords.web.html.FormFields.textBoxField
import com.jeffpdavidson.kotwords.web.html.Html
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
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLTextAreaElement
import kotlin.js.Promise

private const val ID_MANUAL_ENTRY = "manual-entry"
private const val ID_TITLE = "title"
private const val ID_AUTHOR = "author"
private const val ID_COPYRIGHT = "copyright"
private const val ID_NOTES = "notes"
private const val ID_ROW_CLUES = "row-clues"
private const val ID_ROW_ANSWERS = "row-answers"
private const val ID_LIGHT_CLUES = "light-clues"
private const val ID_LIGHT_ANSWERS = "light-answers"
private const val ID_MEDIUM_CLUES = "medium-clues"
private const val ID_MEDIUM_ANSWERS = "medium-answers"
private const val ID_DARK_CLUES = "dark-clues"
private const val ID_DARK_ANSWERS = "dark-answers"

private const val ID_RGZ_FILE = "rgz-file"
private const val ID_FILE = "file"

private const val ID_ADD_ANNOTATIONS = "add-annotations"
private const val ID_LIGHT_BLOOM_COLOR = "light-bloom-color"
private const val ID_MEDIUM_BLOOM_COLOR = "medium-bloom-color"
private const val ID_DARK_BLOOM_COLOR = "dark-bloom-color"

/** Form to convert Rows Garden puzzles into JPZ files. */
class RowsGardenForm {
    private val manualEntryForm = JpzForm(::createPuzzleFromManualEntry, id = ID_MANUAL_ENTRY)
    private val title: HTMLInputElement by Html.getElementById(ID_TITLE)
    private val author: HTMLInputElement by Html.getElementById(ID_AUTHOR)
    private val copyright: HTMLInputElement by Html.getElementById(ID_COPYRIGHT)
    private val notes: HTMLInputElement by Html.getElementById(ID_NOTES)
    private val rowClues: HTMLTextAreaElement by Html.getElementById(ID_ROW_CLUES)
    private val rowAnswers: HTMLTextAreaElement by Html.getElementById(ID_ROW_ANSWERS)
    private val lightClues: HTMLTextAreaElement by Html.getElementById(ID_LIGHT_CLUES)
    private val lightAnswers: HTMLTextAreaElement by Html.getElementById(ID_LIGHT_ANSWERS)
    private val mediumClues: HTMLTextAreaElement by Html.getElementById(ID_MEDIUM_CLUES)
    private val mediumAnswers: HTMLTextAreaElement by Html.getElementById(ID_MEDIUM_ANSWERS)
    private val darkClues: HTMLTextAreaElement by Html.getElementById(ID_DARK_CLUES)
    private val darkAnswers: HTMLTextAreaElement by Html.getElementById(ID_DARK_ANSWERS)
    private val manualEntryAdvancedOptions = AdvancedOptions(ID_MANUAL_ENTRY)

    private val rgzFileForm = JpzForm(::createPuzzleFromRgzFile, { getRgzFileName() }, id = ID_RGZ_FILE)
    private val file: HTMLInputElement by Html.getElementById(ID_FILE)
    private val rgzFileAdvancedOptions = AdvancedOptions(ID_RGZ_FILE)

    init {
        renderPage {
            append.tabs(Tabs.Tab(ID_MANUAL_ENTRY, "Form") {
                with(manualEntryForm) {
                    jpzForm(bodyBlock = {
                        inputField(ID_TITLE, "Title")
                        inputField(ID_AUTHOR, "Author (optional)")
                        inputField(ID_COPYRIGHT, "Copyright (optional)")
                        textBoxField(ID_NOTES, "Notes (optional)") {
                            rows = "5"
                        }
                        div(classes = "form-row") {
                            textBoxField(ID_ROW_CLUES, "Row clues", flexCols = 6) {
                                rows = "12"
                                placeholder =
                                        "The clues for each row; one line per row. Separate multiple clues for a row " +
                                                "with a /."
                            }
                            textBoxField(ID_ROW_ANSWERS, "Row answers", flexCols = 6) {
                                rows = "12"
                                placeholder =
                                        "The answers for each row; one line per row. Separate multiple answers for a " +
                                                "row with a /."
                            }
                        }
                        div(classes = "form-row") {
                            textBoxField(ID_LIGHT_CLUES, "Light clues", flexCols = 6) {
                                rows = "14"
                                placeholder =
                                        "The light bloom clues; one answer per line."
                            }
                            textBoxField(ID_LIGHT_ANSWERS, "Light answers", flexCols = 6) {
                                rows = "14"
                                placeholder =
                                        "The light bloom answers; one answer per line."
                            }
                        }
                        div(classes = "form-row") {
                            textBoxField(ID_MEDIUM_CLUES, "Medium clues", flexCols = 6) {
                                rows = "14"
                                placeholder =
                                        "The medium bloom clues; one answer per line."
                            }
                            textBoxField(ID_MEDIUM_ANSWERS, "Medium answers", flexCols = 6) {
                                rows = "14"
                                placeholder =
                                        "The medium bloom answers; one answer per line."
                            }
                        }
                        div(classes = "form-row") {
                            textBoxField(ID_DARK_CLUES, "Dark clues", flexCols = 6) {
                                rows = "10"
                                placeholder =
                                        "The dark bloom clues; one answer per line."
                            }
                            textBoxField(ID_DARK_ANSWERS, "Dark answers", flexCols = 6) {
                                rows = "10"
                                placeholder =
                                        "The dark bloom answers; one answer per line."
                            }
                        }
                    }, advancedOptionsBlock = manualEntryAdvancedOptions.block)
                }
            }, Tabs.Tab(ID_RGZ_FILE, "RG or RGZ file") {
                with(rgzFileForm) {
                    jpzForm(bodyBlock = {
                        fileField(ID_FILE, "RG or RGZ file")
                    }, advancedOptionsBlock = rgzFileAdvancedOptions.block)
                }
            })
        }
    }

    private class AdvancedOptions(val id: String) {
        val addAnnotations: HTMLInputElement by Html.getElementById("$id-$ID_ADD_ANNOTATIONS")
        val lightBloomColor: HTMLInputElement by Html.getElementById("$id-$ID_LIGHT_BLOOM_COLOR")
        val mediumBloomColor: HTMLInputElement by Html.getElementById("$id-$ID_MEDIUM_BLOOM_COLOR")
        val darkBloomColor: HTMLInputElement by Html.getElementById("$id-$ID_DARK_BLOOM_COLOR")

        val block: FlowContent.() -> Unit = {
            checkField("$id-$ID_ADD_ANNOTATIONS", "Add clue annotations (e.g. \"hyph.\", \"2 wds.\")") {
                checked = true
            }
            div(classes = "form-row") {
                inputField("$id-$ID_LIGHT_BLOOM_COLOR", "Light bloom color", flexCols = 4) {
                    type = InputType.color
                    value = "#FFFFFF"
                }
                inputField("$id-$ID_MEDIUM_BLOOM_COLOR", "Medium bloom color", flexCols = 4) {
                    type = InputType.color
                    value = "#C3C8FA"
                }
                inputField("$id-$ID_DARK_BLOOM_COLOR", "Dark bloom color", flexCols = 4) {
                    type = InputType.color
                    value = "#5765F7"
                }
            }
        }
    }

    private fun createPuzzleFromRgzFile(crosswordSolverSettings: Puzzle.CrosswordSolverSettings): Promise<Puzzle> {
        val rgzFile = Html.getSelectedFile(file)
        return GlobalScope.promise {
            val rgz = Interop.readFile(rgzFile).await()
            asPuzzle(RowsGarden.parse(rgz), rgzFileAdvancedOptions, crosswordSolverSettings)
        }
    }

    private fun createPuzzleFromManualEntry(crosswordSolverSettings: Puzzle.CrosswordSolverSettings): Promise<Puzzle> {
        val rowsGarden = RowsGarden(
                title = title.value.trim(),
                author = author.value.trim(),
                copyright = copyright.value.trim(),
                notes = notes.value.trim(),
                rows = rowClues.value.trim().split("\n").zip(rowAnswers.value.trim().split("\n"))
                        .map { (clues, answers) ->
                            clues.trim().split("/")
                                    .zip(answers.trim().split("/"))
                                    .map { (clue, answer) -> RowsGarden.Entry(clue.trim(), answer.trim()) }
                        },
                light = lightClues.value.trim().split("\n").zip(lightAnswers.value.trim().split("\n"))
                        .map { (clue, answer) -> RowsGarden.Entry(clue.trim(), answer.trim()) },
                medium = mediumClues.value.trim().split("\n").zip(mediumAnswers.value.trim().split("\n"))
                        .map { (clue, answer) -> RowsGarden.Entry(clue.trim(), answer.trim()) },
                dark = darkClues.value.trim().split("\n").zip(darkAnswers.value.trim().split("\n"))
                        .map { (clue, answer) -> RowsGarden.Entry(clue.trim(), answer.trim()) })
        return Promise.resolve(asPuzzle(rowsGarden, manualEntryAdvancedOptions, crosswordSolverSettings))
    }

    private fun asPuzzle(rowsGarden: RowsGarden, advancedOptions: AdvancedOptions,
                         crosswordSolverSettings: Puzzle.CrosswordSolverSettings): Puzzle {
        return rowsGarden.asPuzzle(
                lightBloomColor = advancedOptions.lightBloomColor.value.trim(),
                mediumBloomColor = advancedOptions.mediumBloomColor.value.trim(),
                darkBloomColor = advancedOptions.darkBloomColor.value.trim(),
                addHyphenated = advancedOptions.addAnnotations.checked,
                addWordCount = advancedOptions.addAnnotations.checked,
                crosswordSolverSettings = crosswordSolverSettings)
    }

    private fun getRgzFileName(): String {
        return Html.getSelectedFile(file).name.replace(".rgz", ".jpz").replace(".rg", ".jpz")
    }
}