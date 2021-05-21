package com.jeffpdavidson.kotwords.web

import com.jeffpdavidson.kotwords.formats.ApzFile
import com.jeffpdavidson.kotwords.js.Interop
import com.jeffpdavidson.kotwords.model.Acrostic
import com.jeffpdavidson.kotwords.model.Puzzle
import com.jeffpdavidson.kotwords.web.html.FormFields
import com.jeffpdavidson.kotwords.web.html.Html.renderPage
import com.jeffpdavidson.kotwords.web.html.Tabs
import com.jeffpdavidson.kotwords.web.html.Tabs.tabs
import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.core.String
import kotlinx.html.InputType
import kotlinx.html.dom.append
import kotlin.js.Promise

/** Form to convert Acrostic puzzles into JPZ files. */
internal class AcrosticForm {
    private val manualEntryForm = JpzForm(
        ::createPuzzleFromManualEntry,
        id = "manual-entry",
        includeCompletionMessage = false
    )
    private val title: FormFields.InputField = FormFields.InputField("title")
    private val creator: FormFields.InputField = FormFields.InputField("creator")
    private val copyright: FormFields.InputField = FormFields.InputField("copyright")
    private val description: FormFields.TextBoxField = FormFields.TextBoxField("description")
    private val suggestedWidth: FormFields.InputField = FormFields.InputField("suggested-width")
    private val solution: FormFields.TextBoxField = FormFields.TextBoxField("solution")
    private val gridKey: FormFields.TextBoxField = FormFields.TextBoxField("grid-key")
    private val clues: FormFields.TextBoxField = FormFields.TextBoxField("clues")
    private val answers: FormFields.TextBoxField = FormFields.TextBoxField("answers")
    private val completionMessage: FormFields.TextBoxField = FormFields.TextBoxField("completion-message")

    private val apzFileForm = JpzForm(
        ::createPuzzleFromApzFile,
        { getApzFileName() },
        id = "apz-file",
        completionMessageDefaultValue = "",
        completionMessageHelpText = "If blank, will be generated from the quote and source."
    )
    private val file: FormFields.FileField = FormFields.FileField("file")

    init {
        renderPage {
            append.tabs(Tabs.Tab("manual-entry-tab", "Form") {
                manualEntryForm.render(this, bodyBlock = {
                    this@AcrosticForm.title.render(this, "Title")
                    creator.render(this, "Creator (optional)")
                    copyright.render(this, "Copyright (optional)")
                    description.render(this, "Description (optional)") {
                        rows = "5"
                    }
                    suggestedWidth.render(this, "Suggested width (optional)") {
                        type = InputType.number
                    }
                    solution.render(this, "Solution") {
                        rows = "2"
                        placeholder =
                            "The quote of the acrostic. Use spaces for word breaks. Any non-alphabetical " +
                                    "characters will be prefilled and uneditable in the quote grid."
                    }
                    gridKey.render(this, "Grid key") {
                        rows = "10"
                        placeholder =
                            "The numeric positions in the quote of each letter in the clue answers. One " +
                                    "clue per row, with numbers separated by spaces. Omit clue letters."
                    }
                    clues.render(this, "Clues") {
                        rows = "10"
                        placeholder = "One clue per row. Omit clue letters."
                    }
                    answers.render(this, "Answers (optional)") {
                        rows = "10"
                        placeholder =
                            "One answer per row. Only use alphabetical characters. Only used to validate " +
                                    "that the quote and grid key are consistent with the intended answers."
                    }
                    completionMessage.render(
                        this, label = "Completion message",
                        help = "For acrostics, typically the quote and source of the quote."
                    ) {
                        rows = "3"
                        +"Congratulations! The puzzle is solved correctly."
                    }
                })
            }, Tabs.Tab("apz-file-tab", "APZ file") {
                apzFileForm.render(this, bodyBlock = {
                    file.render(this, "APZ file")
                })
            })
        }
    }

    private fun createPuzzleFromApzFile(crosswordSolverSettings: Puzzle.CrosswordSolverSettings): Promise<Puzzle> {
        return Interop.readFile(file.getValue()).then {
            ApzFile.parse(String(it, charset = Charsets.UTF_8)).toAcrostic(crosswordSolverSettings).asPuzzle()
        }
    }

    private fun createPuzzleFromManualEntry(crosswordSolverSettings: Puzzle.CrosswordSolverSettings): Promise<Puzzle> {
        val acrostic = Acrostic.fromRawInput(
            title = title.getValue(),
            creator = creator.getValue(),
            copyright = copyright.getValue(),
            description = description.getValue(),
            suggestedWidth = suggestedWidth.getValue(),
            solution = solution.getValue(),
            gridKey = gridKey.getValue(),
            clues = clues.getValue(),
            answers = answers.getValue(),
            crosswordSolverSettings = crosswordSolverSettings.copy(
                completionMessage = completionMessage.getValue()
            )
        )
        return Promise.resolve(acrostic.asPuzzle())
    }

    private fun getApzFileName(): String {
        return file.getValue().name.replace(".apz", ".jpz")
    }
}