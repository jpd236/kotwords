package com.jeffpdavidson.kotwords.web

import com.jeffpdavidson.kotwords.KotwordsInternal
import com.jeffpdavidson.kotwords.formats.Apz
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
import kotlinx.html.a
import kotlinx.html.div
import kotlinx.html.dom.append
import kotlinx.html.role
import kotlinx.html.strong
import kotlin.js.Promise

/** Form to convert Acrostic puzzles into JPZ files. */
@JsExport
@KotwordsInternal
class AcrosticForm {
    private val manualEntryForm = PuzzleFileForm(
        "acrostic",
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
    private val includeAttribution: FormFields.CheckBoxField = FormFields.CheckBoxField("include-attribution")

    private val apzFileForm = PuzzleFileForm(
        "acrostic",
        ::createPuzzleFromApzFile,
        { getApzFileName() },
        id = "apz-file",
        completionMessageDefaultValue = "",
        completionMessageHelpText = "If blank, will be generated from the quote and source.",
        enableSaveData = false,
    )
    private val file: FormFields.FileField = FormFields.FileField("file")
    private val apzFileIncludeAttribution: FormFields.CheckBoxField =
        FormFields.CheckBoxField("apz-file-include-attribution")

    init {
        renderPage {
            append.div("alert alert-info") {
                role = "alert"
                strong {
                    +"Note:"
                }
                +" Acrostic JPZ files are only supported in "
                a {
                    href = "https://mrichards42.github.io/xword/"
                    target = "_blank"
                    +"XWord"
                }
                +" and the "
                a {
                    href = "https://www.crosswordnexus.com/solve/"
                    target = "_blank"
                    +"Crossword Nexus solver"
                }
                +"."
            }
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
                    gridKey.render(this, "Grid key (optional)") {
                        rows = "10"
                        placeholder =
                            "The numeric positions in the quote of each letter in the clue answers. One " +
                                    "clue per row, with numbers separated by spaces. Omit clue letters. " +
                                    "Leave this blank and provide the answers to generate a random grid key."
                    }
                    clues.render(this, "Clues") {
                        rows = "10"
                        placeholder = "One clue per row. Omit clue letters."
                    }
                    answers.render(this, "Answers (optional)") {
                        rows = "10"
                        placeholder =
                            "One answer per row. Only use alphabetical characters. Either a grid key or answers must " +
                                    "must be provided."
                    }
                    completionMessage.render(
                        this, label = "Completion message",
                        help = "For acrostics, typically the quote and source of the quote."
                    ) {
                        rows = "3"
                        +"Congratulations! The puzzle is solved correctly."
                    }
                }, advancedOptionsBlock = {
                    includeAttribution.render(this, INCLUDE_ATTRIBUTION_DESCRIPTION)
                })
            }, Tabs.Tab("apz-file-tab", "APZ file") {
                apzFileForm.render(this, bodyBlock = {
                    file.render(this, "APZ file")
                }, advancedOptionsBlock = {
                    apzFileIncludeAttribution.render(this, INCLUDE_ATTRIBUTION_DESCRIPTION)
                })
            })
        }
    }

    private fun createPuzzleFromApzFile(): Promise<Puzzle> {
        return Interop.readFile(file.getValue()).then {
            Apz.fromXmlString(String(it, charset = Charsets.UTF_8)).toAcrostic(
                completionMessage = completionMessage.getValue(),
                includeAttribution = apzFileIncludeAttribution.getValue(),
            ).asPuzzle()
        }
    }

    private fun createPuzzleFromManualEntry(): Promise<Puzzle> {
        if (gridKey.getValue().isBlank() && answers.getValue().isBlank()) {
            return Promise.reject(IllegalArgumentException("Must provide either grid key or answers"))
        }
        if (gridKey.getValue().isBlank()) {
            // While fromRawInput will generate and use a grid key if the provided one is blank, we proactively generate
            // it here so we can update the form with the result, ensuring the exact same output can be reproduced.
            val answersList = if (answers.getValue().isEmpty()) {
                listOf()
            } else {
                answers.getValue().split("\n").map { it.trim() }
            }
            val generatedGridKey = Acrostic.generateGridKey(solution.getValue(), answersList)
            gridKey.setValue(generatedGridKey.joinToString("\n") { it.joinToString(" ") })
        }
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
            completionMessage = completionMessage.getValue(),
            includeAttribution = includeAttribution.getValue(),
        )
        return Promise.resolve(acrostic.asPuzzle())
    }

    private fun getApzFileName(): String {
        return file.getValue().name.removeSuffix(".apz")
    }

    companion object {
        const val INCLUDE_ATTRIBUTION_DESCRIPTION = "Include attribution as a separate entry"
    }
}