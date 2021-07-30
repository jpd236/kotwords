package com.jeffpdavidson.kotwords.web

import com.github.ajalt.colormath.RGB
import com.jeffpdavidson.kotwords.formats.Pdf
import com.jeffpdavidson.kotwords.js.Interop
import com.jeffpdavidson.kotwords.js.Interop.toArrayBuffer
import com.jeffpdavidson.kotwords.model.Puzzle
import com.jeffpdavidson.kotwords.web.html.FormFields
import com.jeffpdavidson.kotwords.web.html.Html
import com.soywiz.klock.DateFormat
import com.soywiz.klock.DateTime
import kotlinx.browser.document
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
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onSubmitFunction
import kotlinx.html.p
import kotlinx.html.role
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.asList
import org.w3c.dom.events.Event
import org.w3c.files.Blob
import org.w3c.files.get
import kotlin.js.Json
import kotlin.js.Promise
import kotlin.js.json

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
    private val puzzleType: String,
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
    enableSaveData: Boolean = true,
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
    private val saveDataButton: FormFields.Button? =
        if (enableSaveData) FormFields.Button(elementId("save-data")) else null
    private val loadDataButton: FormFields.Button? =
        if (enableSaveData) FormFields.Button(elementId("load-data")) else null
    private val loadDataFile: FormFields.FileField? =
        if (enableSaveData) FormFields.FileField(elementId("load-data-file")) else null
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
            id = elementId("form")

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

                saveDataButton?.render(this, "Save data") {
                    classes = classes + "btn-secondary ml-3"
                }
                loadDataButton?.render(this, "Load data") {
                    classes = classes + "btn-secondary ml-3"
                }
                loadDataFile?.render(this) {
                    classes = classes + "d-none"
                    accept = ".json"
                    onChangeFunction = {
                        val files = loadDataFile.input.files
                        if (files != null && files.length > 0 && files[0] != null) {
                            GlobalScope.launch {
                                val saveDataJson = Interop.readFile(files[0]!!).await().decodeToString()
                                // Clear the input value in case the user makes an edit and retries the same file.
                                loadDataFile.input.value = ""
                                loadSaveDataJson(saveDataJson)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun onSubmit(event: Event) {
        val submitter = event.asDynamic().submitter as HTMLElement?
        if (saveDataButton != null && submitter == saveDataButton.button) {
            // Since the form may not be complete, we can't generate the Puzzle to obtain the title. Look for it
            // directly from the form, or else just use the puzzle type and date.
            val title = (document.getElementById("title") as HTMLInputElement?)?.value
            val fileName = if (title == null || title.isBlank()) {
                "$puzzleType-${DateTime.nowLocal().format(DateFormat.FORMAT_DATE)}"
            } else {
                getDefaultFileName(title)
            }
            download("$fileName.json", createSaveDataJson().encodeToByteArray())
        } else if (loadDataButton != null && loadDataFile != null && submitter == loadDataButton.button) {
            loadDataFile.input.click()
        } else if (submitter == jpzButton.button || (pdfButton != null && submitter == pdfButton.button)) {
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
        }
        event.preventDefault()
    }

    internal fun createSaveDataJson(): String {
        val formElement = document.getElementById(elementId("form"))!!
        val formFields = formElement.querySelectorAll("input,select,textarea")
        val saveData = json(KEY_PUZZLE_TYPE to puzzleType)
        formFields.asList().forEach {
            val element = it as HTMLElement
            if (element.classList.contains("d-none")) {
                return@forEach
            }
            val id = element.id
            val value = when (it) {
                is HTMLInputElement -> {
                    when (it.type) {
                        InputType.checkBox.realValue -> it.checked
                        else -> it.value
                    }
                }
                is HTMLTextAreaElement -> it.value
                else -> throw IllegalStateException("")
            }
            saveData[id] = value
        }
        return JSON.stringify(saveData, space = 2)
    }

    internal fun loadSaveDataJson(saveDataJson: String) {
        val saveData: Json = JSON.parse(saveDataJson)
        val savedPuzzleType = saveData[KEY_PUZZLE_TYPE]
        if (puzzleType != savedPuzzleType) {
            errorMessage.setMessage("Invalid save data - unknown puzzle type $savedPuzzleType")
            return
        }
        errorMessage.setMessage("")
        val ids = (js("Object").keys(saveData) as Array<String>).filterNot { it == KEY_PUZZLE_TYPE }
        ids.forEach { id ->
            val value = saveData[id]
            val element = document.getElementById(id) ?: return@forEach
            when (element) {
                is HTMLInputElement -> {
                    when (element.type) {
                        InputType.checkBox.realValue -> element.checked = value as Boolean
                        else -> element.value = value as String
                    }
                    // Invoke the oninput listener, if any, to emulate the user entering the value.
                    val event = document.createEvent("Event")
                    event.initEvent("input", bubbles = true, cancelable = true)
                    element.dispatchEvent(event)
                }
                is HTMLTextAreaElement -> element.value = value as String
            }
        }
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
        private const val KEY_PUZZLE_TYPE = "puzzle-type"

        private fun getDefaultFileName(puzzle: Puzzle): String = getDefaultFileName(puzzle.title)
        private fun getDefaultFileName(title: String): String = title.replace("[^A-Za-z0-9]".toRegex(), "")
    }
}