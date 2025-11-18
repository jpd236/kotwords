package com.jeffpdavidson.kotwords.web

import com.jeffpdavidson.kotwords.KotwordsInternal
import com.jeffpdavidson.kotwords.formats.Apz
import com.jeffpdavidson.kotwords.js.Interop
import com.jeffpdavidson.kotwords.model.Acrostic
import com.jeffpdavidson.kotwords.model.Puzzle
import com.jeffpdavidson.kotwords.util.trimmedLines
import com.jeffpdavidson.kotwords.web.html.FormFields
import com.jeffpdavidson.kotwords.web.html.Html.renderPage
import com.jeffpdavidson.kotwords.web.html.Tabs
import com.jeffpdavidson.kotwords.web.html.Tabs.tabs
import kotlinx.html.InputType
import kotlinx.html.a
import kotlinx.html.div
import kotlinx.html.dom.append
import kotlinx.html.role
import kotlinx.html.strong

/** Form to convert Acrostic puzzles into digital puzzle files. */
@JsExport
@KotwordsInternal
class AcrosticForm {
    private val manualEntryForm = PuzzleFileForm(
        "acrostic",
        ::createPuzzleFromManualEntry,
        id = "manual-entry",
        includeCompletionMessage = false,
        // TODO: Remove once Ipuz supports Acrostics.
        supportsIpuz = false,
    )
    private val gridWidth: FormFields.InputField = FormFields.InputField("grid-width")
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
        enableMetadataInput = false,
        // TODO: Remove once Ipuz supports Acrostics.
        supportsIpuz = false,
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
                    gridWidth.render(this, "Grid width (optional)",
                        help = "Width to use for the quote grid. If blank, a reasonable default will be selected."
                    ) {
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

    private suspend fun createPuzzleFromApzFile(): Puzzle {
        val apz = Interop.readBlob(file.value).decodeToString()
        return Apz.fromXmlString(apz).toAcrostic(
            completionMessage = apzFileForm.completionMessage,
            includeAttribution = apzFileIncludeAttribution.value,
        ).asPuzzle()
    }

    private suspend fun createPuzzleFromManualEntry(): Puzzle {
        if (gridKey.value.isBlank() && answers.value.isBlank()) {
            throw IllegalArgumentException("Must provide either grid key or answers")
        }
        if (gridKey.value.isBlank()) {
            // While fromRawInput will generate and use a grid key if the provided one is blank, we proactively generate
            // it here so we can update the form with the result, ensuring the exact same output can be reproduced.
            val answersList = answers.value.uppercase().trimmedLines()
            val generatedGridKey = Acrostic.generateGridKey(solution.value.uppercase(), answersList)
            gridKey.value = generatedGridKey.joinToString("\n") { it.joinToString(" ") }
        }
        val acrostic = Acrostic.fromRawInput(
            title = manualEntryForm.title,
            creator = manualEntryForm.creator,
            copyright = manualEntryForm.copyright,
            description = manualEntryForm.description,
            gridWidth = gridWidth.value,
            solution = solution.value,
            gridKey = gridKey.value,
            clues = clues.value,
            answers = answers.value,
            completionMessage = completionMessage.value,
            includeAttribution = includeAttribution.value,
        )
        return acrostic.asPuzzle()
    }

    private fun getApzFileName(): String {
        return file.value.name.removeSuffix(".apz")
    }

    companion object {
        const val INCLUDE_ATTRIBUTION_DESCRIPTION = "Include attribution as a separate entry"
    }
}