package com.jeffpdavidson.kotwords.web

import com.github.ajalt.colormath.RGB
import com.jeffpdavidson.kotwords.formats.Pdf
import com.jeffpdavidson.kotwords.js.Interop.toArrayBuffer
import com.jeffpdavidson.kotwords.model.Puzzle
import com.jeffpdavidson.kotwords.web.html.FormFields
import com.jeffpdavidson.kotwords.web.html.Html
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import kotlinx.html.FlowContent
import kotlinx.html.InputType
import kotlinx.html.a
import kotlinx.html.classes
import kotlinx.html.div
import kotlinx.html.dom.append
import kotlinx.html.form
import kotlinx.html.id
import kotlinx.html.js.onSubmitFunction
import kotlinx.html.p
import kotlinx.html.role
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import org.w3c.files.Blob
import kotlin.js.Promise

/**
 * Container for an HTML form for creating digital files for a particular puzzle input.
 *
 * @param createPuzzleFn function to return a [Promise] of the created [Puzzle] from the form input. Given the
 *                       [Puzzle.CrosswordSolverSettings] from advanced settings as input.
 * @param getFileNameFn optional function to return the base filename that should be used when downloading the puzzle.
 *                      Defaults to the alphanumerical characters of the title. Omit any extension.
 * @param id optional ID to use for form elements in case there are multiple forms on a single page.
 * @param includeCompletionMessage whether to include the completion message input field under advanced settings.
 *                                 Callers may opt out of this to show their own completion message field. The
 *                                 [Puzzle.CrosswordSolverSettings] object passed to `createPuzzleFn` will have an empty
 *                                 completion message.
 * @param completionMessageDefaultValue optional default value to use for the completion message.
 * @param completionMessageHelpText optional help text to use for the completion message.
 * @param createPdfFn optional function to return a [Promise] of the created PDF (as a byte array) from the form input.
 *                    Given the [Puzzle.CrosswordSolverSettings] from advanced settings as input.
 */
internal class PuzzleFileForm(
    private val createPuzzleFn: (Puzzle.CrosswordSolverSettings) -> Promise<Puzzle>,
    private val getFileNameFn: (Puzzle) -> String = ::getDefaultFileName,
    private val id: String = "",
    includeCompletionMessage: Boolean = true,
    private val completionMessageDefaultValue: String = "Congratulations! The puzzle is solved correctly.",
    private val completionMessageHelpText: String = "",
    private val createPdfFn: ((
        crosswordSolverSettings: Puzzle.CrosswordSolverSettings,
        blackSquareLightnessAdjustment: Float
    ) -> Promise<ByteArray>)? = null,
) {
    private val cursorColor: FormFields.InputField = FormFields.InputField(elementId("cursor-color"))
    private val selectionColor: FormFields.InputField = FormFields.InputField(elementId("selection-color"))
    private val completionMessage: FormFields.InputField? =
        if (includeCompletionMessage) FormFields.InputField(elementId("completion-message")) else null
    private val inkSaverPercentage: FormFields.ColorRangeSlider? =
        if (createPdfFn != null) {
            FormFields.ColorRangeSlider(elementId("ink-saver-percentage"), ::getInkSaverColor)
        } else {
            null
        }
    private val jpzButton: FormFields.Button = FormFields.Button(elementId("generate-jpz"))
    private val pdfButton: FormFields.Button? =
        if (createPdfFn != null) FormFields.Button(elementId("generate-pdf")) else null
    private val errorMessage: FormFields.ErrorMessage = FormFields.ErrorMessage(elementId("error-message"))

    /**
     * Render the form.
     *
     * @param parent parent to render the form into
     * @param bodyBlock block to render the form contents
     * @param advancedOptionsBlock optional block to render advanced options (along with the default advanced options).
     */
    fun render(
        parent: HTMLElement, bodyBlock: FlowContent.() -> Unit,
        advancedOptionsBlock: FlowContent.() -> Unit = {}
    ) {
        parent.append.div {
            render(this, bodyBlock, advancedOptionsBlock)
        }
    }

    /**
     * Render the form.
     *
     * @param parent parent to render the form into
     * @param bodyBlock block to render the form contents
     * @param advancedOptionsBlock optional block to render advanced options (along with the default advanced options).
     */
    fun render(
        parent: FlowContent, bodyBlock: FlowContent.() -> Unit,
        advancedOptionsBlock: FlowContent.() -> Unit = {}
    ) {
        parent.form {
            onSubmitFunction = ::onSubmit

            bodyBlock()

            val advancedOptionsId = elementId("advanced-options")

            p {
                a(classes = "btn btn-secondary") {
                    href = "#$advancedOptionsId"
                    role = "button"
                    attributes["data-toggle"] = "collapse"
                    attributes["aria-expanded"] = "false"
                    attributes["aria-controls"] = advancedOptionsId
                    +"Advanced options"
                }
            }
            div(classes = "collapse") {
                this.id = advancedOptionsId
                div(classes = "card card-body form-group") {
                    advancedOptionsBlock()

                    div(classes = "form-row") {
                        cursorColor.render(this, "Crossword Solver cursor color", flexCols = 6) {
                            type = InputType.color
                            value = "#00B100"
                        }
                        selectionColor.render(this, "Crossword Solver selection color", flexCols = 6) {
                            type = InputType.color
                            value = "#80FF80"
                        }
                    }
                    completionMessage?.render(this, "Completion message", help = completionMessageHelpText) {
                        value = completionMessageDefaultValue
                    }

                    inkSaverPercentage?.render(
                        this,
                        "Ink saver percentage (for PDFs)",
                        help = "Percentage to lighten colors in the grid. " +
                                "0% keeps colors unchanged; 100% lightens everything to pure white."
                    )
                }
            }

            errorMessage.render(this)

            p {
                jpzButton.render(this, "Generate JPZ") {
                    classes = classes + "btn-primary"
                }

                pdfButton?.render(this, "Generate PDF") {
                    classes = classes + "btn-primary ml-3"
                }
            }
        }
    }

    private fun onSubmit(event: Event) {
        val submitter = event.asDynamic().submitter as HTMLElement?
        GlobalScope.launch {
            try {
                val puzzle = createPuzzleFn(createCrosswordSolverSettings()).await()
                if (pdfButton != null && submitter == pdfButton.button) {
                    // TODO: Avoid this unnecessary Puzzle generation just to get the title/filename.
                    downloadPdf(puzzle)
                } else {
                    downloadJpz(puzzle)
                }
            } catch (t: Throwable) {
                errorMessage.setMessage("Error generating puzzle file: ${t.message}")
            }
        }
        event.preventDefault()
    }

    private suspend fun downloadPdf(puzzle: Puzzle) {
        val data = createPdfFn!!(createCrosswordSolverSettings(), inkSaverPercentage!!.getValue() / 100f).await()
        download("${getFileNameFn(puzzle)}.pdf", data)
    }

    private suspend fun downloadJpz(puzzle: Puzzle) {
        val fileName = "${puzzle.title.replace("[^A-Za-z0-9]".toRegex(), "")}.xml"
        val data = puzzle.asJpzFile().toCompressedFile(fileName)
        download("${getFileNameFn(puzzle)}.jpz", data)
    }

    private fun download(fileName: String, data: ByteArray) {
        val blob = Blob(arrayOf(data.toArrayBuffer()))
        errorMessage.setMessage("")
        Html.downloadBlob(fileName, blob)
    }

    private fun createCrosswordSolverSettings(): Puzzle.CrosswordSolverSettings {
        return Puzzle.CrosswordSolverSettings(
            cursorColor = cursorColor.getValue(),
            selectedCellsColor = selectionColor.getValue(),
            completionMessage = completionMessage?.getValue() ?: ""
        )
    }

    private fun elementId(elementId: String) = if (id.isNotEmpty()) "$id-$elementId" else elementId

    private fun getInkSaverColor(value: Int): String = Pdf.getAdjustedColor(RGB("#000000"), value / 100f).toHex()

    companion object {
        private fun getDefaultFileName(puzzle: Puzzle): String {
            return puzzle.title.replace("[^A-Za-z0-9]".toRegex(), "")
        }
    }
}