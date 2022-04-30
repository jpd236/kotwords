package com.jeffpdavidson.kotwords.web

import com.github.ajalt.colormath.model.RGB
import com.jeffpdavidson.kotwords.formats.CrosswordCompilerApplet
import com.jeffpdavidson.kotwords.formats.Jpz.Companion.asJpzFile
import com.jeffpdavidson.kotwords.formats.Pdf
import com.jeffpdavidson.kotwords.js.Interop.toArrayBuffer
import com.jeffpdavidson.kotwords.model.Puzzle
import com.jeffpdavidson.kotwords.web.html.FormFields
import com.jeffpdavidson.kotwords.web.html.Html
import com.soywiz.klock.DateFormat
import com.soywiz.klock.DateTime
import kotlinx.browser.document
import kotlinx.coroutines.GlobalScope
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
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.asList
import org.w3c.files.Blob
import kotlin.collections.set
import kotlin.js.Json
import kotlin.js.json
import kotlin.reflect.KProperty

/**
 * Container for an HTML form for creating digital files for a particular puzzle input.
 *
 * @param createPuzzleFn function to return the created [Puzzle] from the form input.
 * @param getFileNameFn optional function to return the base filename that should be used when downloading the puzzle.
 *                      Defaults to the alphanumerical characters of the title. Omit any extension.
 * @param id optional ID to use for form elements in case there are multiple forms on a single page.
 * @param includeCompletionMessage whether to include the completion message input field under advanced settings.
 *                                 Callers may opt out of this to show their own completion message field. If true, the
 *                                 completion message will be provided as part of the applet settings when converting
 *                                 the puzzle generated by createPuzzleFn to a JPZ file; otherwise, the applet settings
 *                                 will have an empty completion message, though the puzzle may still provide one in the
 *                                 generated Puzzle. The completion message can also be obtained directly with
 *                                 [completionMessage] if it is needed prior to JPZ generation.
 * @param completionMessageDefaultValue optional default value to use for the completion message.
 * @param completionMessageHelpText optional help text to use for the completion message.
 * @param createPdfFn optional function to return the created PDF (as a byte array) from the form input.
 * @param enableSaveData whether to enable the save/load data functions from form input. Defaults to true.
 * @param enableMetadataInput whether to enable the default metadata form fields. Defaults to true.
 */
internal class PuzzleFileForm(
    private val puzzleType: String,
    private val createPuzzleFn: suspend () -> Puzzle,
    private val getFileNameFn: (Puzzle) -> String = ::getDefaultFileName,
    private val id: String = "",
    includeCompletionMessage: Boolean = true,
    private val completionMessageDefaultValue: String = "Congratulations! The puzzle is solved correctly.",
    private val completionMessageHelpText: String = "",
    private val createPdfFn: (suspend (blackSquareLightnessAdjustment: Float) -> ByteArray)? = null,
    enableSaveData: Boolean = true,
    enableMetadataInput: Boolean = true,
) {
    private open class Field<T>(private val input: FormFields.FormField<T>?, private val defaultValue: T) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): T = input?.value ?: defaultValue
        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            input?.value = value
        }
    }

    private class StringField(private val input: FormFields.FormField<String>?) : Field<String>(input, "")

    private val titleField: FormFields.InputField? =
        if (enableMetadataInput) FormFields.InputField(elementId("title")) else null
    var title: String by StringField(titleField)

    private val creatorField: FormFields.InputField? =
        if (enableMetadataInput) FormFields.InputField(elementId("creator")) else null
    var creator: String by StringField(creatorField)

    private val copyrightField: FormFields.InputField? =
        if (enableMetadataInput) FormFields.InputField(elementId("copyright")) else null
    var copyright: String by StringField(copyrightField)

    private val descriptionField: FormFields.TextBoxField? =
        if (enableMetadataInput) FormFields.TextBoxField(elementId("description")) else null
    var description: String by StringField(descriptionField)

    private val cursorColorField: FormFields.InputField = FormFields.InputField(elementId("cursor-color"))
    private val selectionColorField: FormFields.InputField = FormFields.InputField(elementId("selection-color"))
    private val compressJpzField: FormFields.CheckBoxField = FormFields.CheckBoxField(elementId("compress-jpz"))
    private val completionMessageField: FormFields.InputField? =
        if (includeCompletionMessage) FormFields.InputField(elementId("completion-message")) else null
    val completionMessage: String
        get() = completionMessageField?.value ?: ""

    private val inkSaverPercentageField: FormFields.ColorRangeSlider? =
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
    private val loadDataFileField: FormFields.FileField? =
        if (enableSaveData) FormFields.FileField(elementId("load-data-file")) else null
    private val errorMessage: FormFields.ErrorMessage = FormFields.ErrorMessage(elementId("error-message"))

    /**
     * Render the form.
     *
     * @param parent parent to render the form into
     * @param bodyBlock block to render the form contents
     * @param advancedOptionsBlock optional block to render advanced options (along with the default advanced options)
     * @param additionalButtonsBlock optional block to render custom buttons (along with the default set)
     */
    fun render(
        parent: HTMLElement,
        bodyBlock: FlowContent.() -> Unit,
        advancedOptionsBlock: FlowContent.() -> Unit = {},
        additionalButtonsBlock: FlowContent.() -> Unit = {},
        submitHandler: (HTMLElement) -> Boolean = { false },
    ) {
        parent.append.div {
            render(
                parent = this,
                bodyBlock = bodyBlock,
                advancedOptionsBlock = advancedOptionsBlock,
                additionalButtonsBlock = additionalButtonsBlock,
                submitHandler = submitHandler,
            )
        }
    }

    /**
     * Render the form.
     *
     * @param parent parent to render the form into
     * @param bodyBlock block to render the form contents
     * @param advancedOptionsBlock optional block to render advanced options (along with the default advanced options)
     * @param additionalButtonsBlock optional block to render custom buttons (along with the default set)
     * @param submitHandler optional function invoked on form submission; passed the submitting element as an argument.
     *                      Return true to indicate that the event has been handled, and false for default handling to
     *                      apply.
     */
    fun render(
        parent: FlowContent, bodyBlock: FlowContent.() -> Unit,
        advancedOptionsBlock: FlowContent.() -> Unit = {},
        additionalButtonsBlock: FlowContent.() -> Unit = {},
        submitHandler: (HTMLElement) -> Boolean = { false },
    ) {
        parent.form {
            id = elementId("form")

            onSubmitFunction = { event ->
                val submitter = event.asDynamic().submitter as HTMLElement?
                if (submitter == null || !submitHandler(submitter)) {
                    onSubmit(submitter)
                }
                event.preventDefault()
            }

            titleField?.render(this, "Title")
            creatorField?.render(this, "Creator (optional)")
            copyrightField?.render(this, "Copyright (optional)")
            descriptionField?.render(this, "Description (optional)") {
                rows = "5"
            }

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
                        cursorColorField.render(this, "Crossword Solver cursor color", flexCols = 6) {
                            type = InputType.color
                            value = "#00B100"
                        }
                        selectionColorField.render(this, "Crossword Solver selection color", flexCols = 6) {
                            type = InputType.color
                            value = "#80FF80"
                        }
                    }

                    compressJpzField.render(this, "Compress JPZ file") {
                        checked = true
                    }

                    completionMessageField?.render(this, "Completion message", help = completionMessageHelpText) {
                        value = completionMessageDefaultValue
                    }

                    inkSaverPercentageField?.render(
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
                loadDataFileField?.renderAsHiddenSelector(this, ".json", ::loadSaveDataJson)
                additionalButtonsBlock()
            }
        }
    }

    private fun onSubmit(submitter: HTMLElement?) {
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
        } else if (loadDataButton != null && loadDataFileField != null && submitter == loadDataButton.button) {
            loadDataFileField.input.click()
        } else if (submitter == jpzButton.button || (pdfButton != null && submitter == pdfButton.button)) {
            GlobalScope.launch {
                try {
                    val puzzle = createPuzzleFn()
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

    internal fun loadSaveDataJson(saveDataJson: ByteArray) {
        val saveData: Json = JSON.parse(saveDataJson.decodeToString())
        val savedPuzzleType = saveData[KEY_PUZZLE_TYPE]
        if (puzzleType != savedPuzzleType) {
            errorMessage.setMessage("Invalid save data - unknown puzzle type $savedPuzzleType")
            return
        }
        errorMessage.setMessage("")
        val ids = (js("Object").keys(saveData) as Array<String>).filterNot { it == KEY_PUZZLE_TYPE }
        ids.forEach { id ->
            // Map legacy standalone metadata fields from old save data to current fields.
            val fieldId = when (id) {
                "title" -> elementId("title")
                "creator", "author" -> elementId("creator")
                "copyright" -> elementId("copyright")
                "description", "notes" -> elementId("description")
                else -> id
            }
            val element = document.getElementById(fieldId) ?: return@forEach

            val value = saveData[id]
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
        createPdfFn?.let {
            download("${getFileNameFn(puzzle)}.pdf", it(inkSaverPercentageField!!.value / 100f))
        }
    }

    private suspend fun downloadJpz(puzzle: Puzzle) {
        val fileName = puzzle.title.replace("[^A-Za-z0-9]".toRegex(), "")
        val jpzFile = puzzle.asJpzFile(appletSettings = createAppletSettings())
        val data = if (compressJpzField.value) {
            jpzFile.toCompressedFile("$fileName.xml")
        } else {
            jpzFile.toXmlString(prettyPrint = true).encodeToByteArray()
        }
        download("${getFileNameFn(puzzle)}.jpz", data)
    }

    private fun download(fileName: String, data: ByteArray) {
        val blob = Blob(arrayOf(data.toArrayBuffer()))
        errorMessage.setMessage("")
        Html.downloadBlob(fileName, blob)
    }

    private fun createAppletSettings(): CrosswordCompilerApplet.AppletSettings {
        return CrosswordCompilerApplet.AppletSettings(
            cursorColor = cursorColorField.value,
            selectedCellsColor = selectionColorField.value,
            completion = CrosswordCompilerApplet.AppletSettings.Completion(
                message = completionMessageField?.value ?: ""
            )
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