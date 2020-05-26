package com.jeffpdavidson.kotwords.web

import com.jeffpdavidson.kotwords.js.Interop.toArrayBuffer
import com.jeffpdavidson.kotwords.model.Puzzle
import com.jeffpdavidson.kotwords.web.html.FormFields
import com.jeffpdavidson.kotwords.web.html.Html
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import kotlinx.html.ButtonType
import kotlinx.html.FlowContent
import kotlinx.html.InputType
import kotlinx.html.a
import kotlinx.html.button
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
 * Container for an HTML form for creating a JPZ file for a particular puzzle input.
 *
 * @param createPuzzleFn function to return a [Promise] of the created [Puzzle] from the form input. Given the
 *                       [Puzzle.CrosswordSolverSettings] from advanced settings as input.
 * @param getFileNameFn optional function to return the filename that should be used when downloading the puzzle.
 *                      Defaults to the alphanumerical characters of the title plus ".jpz".
 * @param id optional ID to use for form elements in case there are multiple forms on a single page.
 * @param includeCompletionMessage whether to include the completion message input field under advanced settings.
 *                                 Callers may opt out of this to show their own completion message field. The
 *                                 [Puzzle.CrosswordSolverSettings] object passed to `createPuzzleFn` will have an empty
 *                                 completion message.
 * @param completionMessageDefaultValue optional default value to use for the completion message.
 * @param completionMessageHelpText optional help text to use for the completion message.
 */
class JpzForm(private val createPuzzleFn: (Puzzle.CrosswordSolverSettings) -> Promise<Puzzle>,
              private val getFileNameFn: (Puzzle) -> String = ::getDefaultFileName,
              private val id: String = "",
              includeCompletionMessage: Boolean = true,
              private val completionMessageDefaultValue: String = "Congratulations! The puzzle is solved correctly.",
              private val completionMessageHelpText: String = "") {
    private val cursorColor: FormFields.InputField = FormFields.InputField(elementId("cursor-color"))
    private val selectionColor: FormFields.InputField = FormFields.InputField(elementId("selection-color"))
    private val completionMessage: FormFields.InputField? =
            if (includeCompletionMessage) FormFields.InputField(elementId("completion-message")) else null
    private val errorMessage: FormFields.ErrorMessage = FormFields.ErrorMessage(elementId("error-message"))

    /**
     * Render the form.
     *
     * @param parent parent to render the form into
     * @param bodyBlock block to render the form contents
     * @param advancedOptionsBlock optional block to render advanced options (along with the default advanced options).
     */
    fun render(parent: HTMLElement, bodyBlock: FlowContent.() -> Unit,
               advancedOptionsBlock: FlowContent.() -> Unit = {}) {
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
    fun render(parent: FlowContent, bodyBlock: FlowContent.() -> Unit,
               advancedOptionsBlock: FlowContent.() -> Unit = {}) {
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
                }
            }

            errorMessage.render(this)

            p {
                button(classes = "btn btn-primary") {
                    type = ButtonType.submit
                    +"Generate JPZ"
                }
            }
        }
    }

    private fun onSubmit(event: Event) {
        try {
            val puzzlePromise = createPuzzleFn(createCrosswordSolverSettings())
            puzzlePromise.then { puzzle ->
                GlobalScope.promise {
                    val fileName = "${puzzle.title.replace("[^A-Za-z0-9]".toRegex(), "")}.xml"
                    val data = puzzle.asJpzFile().toCompressedFile(fileName)
                    val blob = Blob(arrayOf(data.toArrayBuffer()))
                    errorMessage.setMessage("")
                    Html.downloadBlob(getFileNameFn(puzzle), blob)
                }
            }.catch {
                errorMessage.setMessage("Error generating JPZ: ${it.message}")
            }
        } catch (t: Throwable) {
            errorMessage.setMessage("Error generating JPZ: ${t.message}")
        } finally {
            event.preventDefault()
        }
    }

    private fun createCrosswordSolverSettings(): Puzzle.CrosswordSolverSettings {
        return Puzzle.CrosswordSolverSettings(
                cursorColor = cursorColor.getValue(),
                selectedCellsColor = selectionColor.getValue(),
                completionMessage = completionMessage?.getValue() ?: "")
    }

    private fun elementId(elementId: String) = if (id.isNotEmpty()) "$id-$elementId" else elementId

    companion object {
        private fun getDefaultFileName(puzzle: Puzzle): String {
            return "${puzzle.title.replace("[^A-Za-z0-9]".toRegex(), "")}.jpz"
        }
    }
}