package com.jeffpdavidson.kotwords.web

import com.jeffpdavidson.kotwords.js.Interop.toArrayBuffer
import com.jeffpdavidson.kotwords.model.Puzzle
import com.jeffpdavidson.kotwords.web.html.FormFields.inputField
import com.jeffpdavidson.kotwords.web.html.Html
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import kotlinx.html.ButtonType
import kotlinx.html.FlowContent
import kotlinx.html.InputType
import kotlinx.html.TagConsumer
import kotlinx.html.a
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.form
import kotlinx.html.id
import kotlinx.html.js.onSubmitFunction
import kotlinx.html.p
import kotlinx.html.role
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event
import org.w3c.files.Blob
import kotlin.dom.addClass
import kotlin.dom.removeClass
import kotlin.js.Promise

private const val ID_ADVANCED_OPTIONS = "advanced-options"
private const val ID_CURSOR_COLOR = "cursor-color"
private const val ID_SELECTION_COLOR = "selection-color"
private const val ID_COMPLETION_MESSAGE = "completion-message"
private const val ID_ERROR_MESSAGE = "error-message"

private const val DEFAULT_COMPLETION_MESSAGE = "Congratulations! The puzzle is solved correctly."

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
              private val includeCompletionMessage: Boolean = true,
              private val completionMessageDefaultValue: String = DEFAULT_COMPLETION_MESSAGE,
              private val completionMessageHelpText: String = "") {
    private val cursorColor: HTMLInputElement by Html.getElementById(elementId(ID_CURSOR_COLOR))
    private val selectionColor: HTMLInputElement by Html.getElementById(elementId(ID_SELECTION_COLOR))
    private val completionMessage: HTMLInputElement by Html.getElementById(elementId(ID_COMPLETION_MESSAGE))
    private val errorMessage: HTMLInputElement by Html.getElementById(elementId(ID_ERROR_MESSAGE))

    /**
     * Render the form.
     *
     * @param bodyBlock block to render the form contents
     * @param advancedOptionsBlock optional block to render advanced options (along with the default advanced options).
     */
    fun <T> TagConsumer<T>.jpzForm(bodyBlock: FlowContent.() -> Unit,
                                   advancedOptionsBlock: FlowContent.() -> Unit = {}) {
        div {
            jpzForm(bodyBlock, advancedOptionsBlock)
        }
    }

    /**
     * Render the form.
     *
     * @param bodyBlock block to render the form contents
     * @param advancedOptionsBlock optional block to render advanced options (along with the default advanced options).
     */
    fun FlowContent.jpzForm(bodyBlock: FlowContent.() -> Unit,
                            advancedOptionsBlock: FlowContent.() -> Unit = {}) {
        form {
            onSubmitFunction = ::onSubmit

            bodyBlock()

            p {
                a(classes = "btn btn-secondary") {
                    href = "#${elementId(ID_ADVANCED_OPTIONS)}"
                    role = "button"
                    attributes["data-toggle"] = "collapse"
                    attributes["aria-expanded"] = "false"
                    attributes["aria-controls"] = elementId(ID_ADVANCED_OPTIONS)
                    +"Advanced options"
                }
            }
            div(classes = "collapse") {
                this.id = elementId(ID_ADVANCED_OPTIONS)
                div(classes = "card card-body form-group") {
                    advancedOptionsBlock()

                    div(classes = "form-row") {
                        inputField(elementId(ID_CURSOR_COLOR), "Crossword Solver cursor color", flexCols = 6) {
                            type = InputType.color
                            value = "#00B100"
                        }
                        inputField(
                                elementId(ID_SELECTION_COLOR), "Crossword Solver selection color", flexCols = 6) {
                            type = InputType.color
                            value = "#80FF80"
                        }
                    }
                    if (includeCompletionMessage) {
                        inputField(elementId(ID_COMPLETION_MESSAGE),
                                "Completion message", help = completionMessageHelpText) {
                            value = completionMessageDefaultValue
                        }
                    }

                }
            }
            div(classes = "alert alert-primary d-none") {
                this.id = elementId(ID_ERROR_MESSAGE)
                role = "alert"
            }

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
                    errorMessage.addClass("d-none")
                    Html.downloadBlob(getFileNameFn(puzzle), blob)
                }
            }.catch {
                onError(it)
            }
        } catch (t: Throwable) {
            onError(t)
        } finally {
            event.preventDefault()
        }
    }

    private fun createCrosswordSolverSettings(): Puzzle.CrosswordSolverSettings {
        return Puzzle.CrosswordSolverSettings(
                cursorColor = cursorColor.value.trim(),
                selectedCellsColor = selectionColor.value.trim(),
                completionMessage = if (includeCompletionMessage) completionMessage.value.trim() else "")
    }

    private fun onError(t: Throwable) {
        errorMessage.removeClass("d-none")
        errorMessage.innerText = "Error generating JPZ: ${t.message}"
    }

    private fun elementId(elementId: String) = if (id.isNotEmpty()) "$id-$elementId" else elementId

    companion object {
        private fun getDefaultFileName(puzzle: Puzzle): String {
            return "${puzzle.title.replace("[^A-Za-z0-9]".toRegex(), "")}.jpz"
        }
    }
}