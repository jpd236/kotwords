package com.jeffpdavidson.kotwords.web

import com.jeffpdavidson.kotwords.js.Interop
import com.jeffpdavidson.kotwords.model.Acrostic
import com.jeffpdavidson.kotwords.model.Puzzle
import com.jeffpdavidson.kotwords.web.html.FormFields.fileField
import com.jeffpdavidson.kotwords.web.html.FormFields.inputField
import com.jeffpdavidson.kotwords.web.html.FormFields.textBoxField
import com.jeffpdavidson.kotwords.web.html.Html
import com.jeffpdavidson.kotwords.web.html.Html.renderPage
import com.jeffpdavidson.kotwords.web.html.Tabs
import com.jeffpdavidson.kotwords.web.html.Tabs.tabs
import kotlinx.html.InputType
import kotlinx.html.dom.append
import kotlinx.io.charsets.Charsets
import kotlinx.io.core.String
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLTextAreaElement
import kotlin.js.Promise

private const val ID_MANUAL_ENTRY = "manual-entry"
private const val ID_TITLE = "title"
private const val ID_CREATOR = "creator"
private const val ID_COPYRIGHT = "copyright"
private const val ID_DESCRIPTION = "description"
private const val ID_SUGGESTED_WIDTH = "suggested-width"
private const val ID_SOLUTION = "solution"
private const val ID_GRID_KEY = "grid-key"
private const val ID_CLUES = "clues"
private const val ID_ANSWERS = "answers"
private const val ID_COMPLETION_MESSAGE = "completion-message"

private const val ID_APZ_FILE = "apz-file"
private const val ID_FILE = "file"

/** Form to convert Acrostic puzzles into JPZ files. */
class AcrosticForm {
    private val manualEntryForm = JpzForm(
            ::createPuzzleFromManualEntry,
            id = ID_MANUAL_ENTRY,
            includeCompletionMessage = false)
    private val title: HTMLInputElement by Html.getElementById(ID_TITLE)
    private val creator: HTMLInputElement by Html.getElementById(ID_CREATOR)
    private val copyright: HTMLInputElement by Html.getElementById(ID_COPYRIGHT)
    private val description: HTMLTextAreaElement by Html.getElementById(ID_DESCRIPTION)
    private val suggestedWidth: HTMLInputElement by Html.getElementById(ID_SUGGESTED_WIDTH)
    private val solution: HTMLTextAreaElement by Html.getElementById(ID_SOLUTION)
    private val gridKey: HTMLTextAreaElement by Html.getElementById(ID_GRID_KEY)
    private val clues: HTMLTextAreaElement by Html.getElementById(ID_CLUES)
    private val answers: HTMLTextAreaElement by Html.getElementById(ID_ANSWERS)
    private val completionMessage: HTMLTextAreaElement by Html.getElementById(ID_COMPLETION_MESSAGE)

    private val apzFileForm = JpzForm(
            ::createPuzzleFromApzFile,
            { getApzFileName() },
            id = ID_APZ_FILE,
            completionMessageDefaultValue = "",
            completionMessageHelpText = "If blank, will be generated from the quote and source.")
    private val file: HTMLInputElement by Html.getElementById(ID_FILE)

    init {
        renderPage {
            append.tabs(Tabs.Tab(ID_MANUAL_ENTRY, "Form") {
                with(manualEntryForm) {
                    jpzForm(bodyBlock = {
                        inputField(ID_TITLE, "Title")
                        inputField(ID_CREATOR, "Creator (optional)")
                        inputField(ID_COPYRIGHT, "Copyright (optional)")
                        textBoxField(ID_DESCRIPTION, "Description (optional)") {
                            rows = "5"
                        }
                        inputField(ID_SUGGESTED_WIDTH, "Suggested width (optional)") {
                            type = InputType.number
                        }
                        textBoxField(ID_SOLUTION, "Solution") {
                            rows = "2"
                            placeholder =
                                    "The quote of the acrostic. Use spaces for word breaks. Any non-alphabetical " +
                                            "characters will be prefilled and uneditable in the quote grid."
                        }
                        textBoxField(ID_GRID_KEY, "Grid key") {
                            rows = "10"
                            placeholder =
                                    "The numeric positions in the quote of each letter in the clue answers. One " +
                                            "clue per row, with numbers separated by spaces. Omit clue letters."
                        }
                        textBoxField(ID_CLUES, "Clues") {
                            rows = "10"
                            placeholder = "One clue per row. Omit clue letters."
                        }
                        textBoxField(ID_ANSWERS, "Answers (optional)") {
                            rows = "10"
                            placeholder =
                                    "One answer per row. Only use alphabetical characters. Only used to validate " +
                                            "that the quote and grid key are consistent with the intended answers."
                        }
                        textBoxField(ID_COMPLETION_MESSAGE, label = "Completion message",
                                help = "For acrostics, typically the quote and source of the quote.") {
                            rows = "3"
                            +"Congratulations! The puzzle is solved correctly."
                        }
                    })
                }
            }, Tabs.Tab(ID_APZ_FILE, "APZ file") {
                with(apzFileForm) {
                    jpzForm(bodyBlock = {
                        fileField(ID_FILE, "APZ file")
                    })
                }
            })
        }
    }

    private fun createPuzzleFromApzFile(crosswordSolverSettings: Puzzle.CrosswordSolverSettings): Promise<Puzzle> {
        val apzFile = Html.getSelectedFile(file)
        return Interop.readFile(apzFile).then {
            Acrostic.fromApz(
                    apzContents = String(it, charset = Charsets.UTF_8),
                    crosswordSolverSettings = crosswordSolverSettings).asPuzzle()
        }
    }

    private fun createPuzzleFromManualEntry(crosswordSolverSettings: Puzzle.CrosswordSolverSettings): Promise<Puzzle> {
        val acrostic = Acrostic.fromRawInput(
                title = title.value,
                creator = creator.value,
                copyright = copyright.value,
                description = description.value,
                suggestedWidth = suggestedWidth.value,
                solution = solution.value,
                gridKey = gridKey.value,
                clues = clues.value,
                answers = answers.value,
                crosswordSolverSettings = crosswordSolverSettings.copy(completionMessage = completionMessage.value))
        return Promise.resolve(acrostic.asPuzzle())
    }

    private fun getApzFileName(): String {
        return Html.getSelectedFile(file).name.replace(".apz", ".jpz")
    }
}