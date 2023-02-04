package com.jeffpdavidson.kotwords.web.html

import com.jeffpdavidson.kotwords.js.Interop
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.dom.addClass
import kotlinx.dom.removeClass
import kotlinx.html.BUTTON
import kotlinx.html.ButtonType
import kotlinx.html.DIV
import kotlinx.html.FlowContent
import kotlinx.html.INPUT
import kotlinx.html.InputType
import kotlinx.html.TEXTAREA
import kotlinx.html.button
import kotlinx.html.classes
import kotlinx.html.div
import kotlinx.html.id
import kotlinx.html.input
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onInputFunction
import kotlinx.html.label
import kotlinx.html.role
import kotlinx.html.small
import kotlinx.html.span
import kotlinx.html.style
import kotlinx.html.textArea
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLSpanElement
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.files.File
import org.w3c.files.get

/** Classes to encapsulate and render form fields. */
internal object FormFields {

    private val mainScope = MainScope()

    interface FormField<T> {
        var value: T
    }

    /**
     * Create an <input> field for standard input types.
     *
     * @param htmlId the ID to be used for the input field (and as an ID prefix for associated tags)
     */
    class InputField(private val htmlId: String) : FormField<String> {
        private val input: HTMLInputElement by Html.getElementById(htmlId)

        override var value: String
            get() = input.value.trim()
            set(value) {
                input.value = value
            }

        /**
         * Render the field into the given [FlowContent].
         *
         * @param parent the parent [FlowContent] to render into
         * @param label the label for the input field
         * @param help optional help text used to describe the field in more detail
         * @param flexCols optional number of columns this field should take up in the parent container.
         * @param block optional block run in the scope of the [INPUT] tag for further customization.
         */
        fun render(
            parent: FlowContent, label: String, help: String = "", flexCols: Int? = null,
            block: INPUT.() -> Unit = {}
        ) {
            with(parent) {
                formGroup(flexCols) {
                    label {
                        htmlFor = htmlId
                        +label
                    }
                    input(classes = "form-control") {
                        this.id = htmlId
                        this.type = InputType.text
                        if (help.isNotBlank()) {
                            attributes["aria-describedby"] = "$htmlId-help"
                        }
                        block()
                    }
                    if (help.isNotBlank()) {
                        help(htmlId, help)
                    }
                }
            }
        }
    }

    /**
     * Create an <input> field for a check box.
     *
     * @param htmlId the ID to be used for the input field (and as an ID prefix for associated tags)
     */
    class CheckBoxField(private val htmlId: String) : FormField<Boolean> {
        private val input: HTMLInputElement by Html.getElementById(htmlId)

        override var value: Boolean
            get() = input.checked
            set(value) {
                input.checked = value
            }

        /**
         * Render the field into the given [FlowContent].
         *
         * @param parent the parent [FlowContent] to render into
         * @param label the label for the input field
         * @param block optional block run in the scope of the [INPUT] tag for further customization.
         */
        fun render(parent: FlowContent, label: String, block: INPUT.() -> Unit = {}) {
            with(parent) {
                formGroup {
                    div(classes = "form-check") {
                        input(classes = "form-check-input") {
                            this.id = htmlId
                            this.type = InputType.checkBox
                            block()
                        }
                        label(classes = "form-check-label") {
                            htmlFor = htmlId
                            +label
                        }
                    }
                }
            }
        }
    }

    /**
     * Create an <input> field for a file selector.
     *
     * @param htmlId the ID to be used for the input field (and as an ID prefix for associated tags)
     */
    class FileField(private val htmlId: String) : FormField<File> {
        val input: HTMLInputElement by Html.getElementById(htmlId)

        override var value: File
            get() {
                if (input.files?.length == 0 || input.files!![0] == null) {
                    throw IllegalArgumentException("No file selected")
                }
                return input.files!![0]!!
            }
            set(_) {
                throw UnsupportedOperationException()
            }

        /**
         * Render the file selector into the given [FlowContent].
         *
         * @param parent the parent [FlowContent] to render into
         * @param label the label for the input field
         * @param help optional help text used to describe the field in more detail
         * @param block optional block run in the scope of the [INPUT] tag for further customization.
         */
        fun render(parent: FlowContent, label: String = "", help: String = "", block: INPUT.() -> Unit = {}) {
            with(parent) {
                formGroup {
                    if (label.isNotBlank()) {
                        label {
                            htmlFor = htmlId
                            +label
                        }
                    }
                    input(classes = "form-control-file") {
                        this.id = htmlId
                        this.type = InputType.file
                        if (help.isNotBlank()) {
                            attributes["aria-describedby"] = "$htmlId-help"
                        }
                        block()
                    }
                    if (help.isNotBlank()) {
                        help(htmlId, help)
                    }
                }
            }
        }

        /**
         * Add the file selector into the given [FlowContent] as a hidden file selector.
         *
         * In this mode, the input is not visible, but can be used to read a local file by emulating a click on the
         * input and acting on the file in the provided [onFileLoadedFn].
         *
         * @param parent the parent [FlowContent] to render into
         * @param extension the extension to accept, e.g. ".puz"
         * @param onFileLoadedFn function which will be invoked with the contents of the read file
         */
        fun renderAsHiddenSelector(
            parent: FlowContent,
            extension: String,
            onFileLoadedFn: suspend (ByteArray) -> Unit
        ) {
            with(parent) {
                input(classes = "form-control-file d-none") {
                    this.id = htmlId
                    this.type = InputType.file
                    accept = extension
                    onChangeFunction = {
                        val files = input.files
                        if (files != null && files.length > 0 && files[0] != null) {
                            mainScope.launch {
                                val data = Interop.readBlob(files[0]!!)
                                // Clear the input value in case the user makes an edit and retries the same file.
                                input.value = ""
                                onFileLoadedFn(data)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Create an <input> field for a multi-line text box.
     *
     * @param htmlId the ID to be used for the input field (and as an ID prefix for associated tags)
     */
    class TextBoxField(private val htmlId: String) : FormField<String> {
        private val input: HTMLTextAreaElement by Html.getElementById(htmlId)

        override var value: String
            get() = input.value.trim()
            set(value) {
                input.value = value
            }

        val untrimmedValue: String
            get() = input.value

        /**
         * Render the field into the given [FlowContent].
         *
         * @param parent the parent [FlowContent] to render into
         * @param label the label for the input field
         * @param help optional help text used to describe the field in more detail
         * @param flexCols optional number of columns this field should take up in the parent container.
         * @param block optional block run in the scope of the [TEXTAREA] tag for further customization.
         */
        fun render(
            parent: FlowContent, label: String, help: String = "", flexCols: Int? = null,
            block: TEXTAREA.() -> Unit = {}
        ) {
            with(parent) {
                formGroup(flexCols) {
                    label {
                        htmlFor = htmlId
                        +label
                    }
                    textArea(classes = "form-control") {
                        this.id = htmlId
                        if (help.isNotBlank()) {
                            attributes["aria-describedby"] = "$htmlId-help"
                        }
                        block()
                    }
                    if (help.isNotBlank()) {
                        help(htmlId, help)
                    }
                }
            }
        }
    }

    /**
     * Create an <input> field for a color range slider.
     *
     * @param htmlId the ID to be used for the input field (and as an ID prefix for associated tags)
     * @param colorGetter function returning the HTML color value for the given value in the range (from 0 to 100).
     */
    class ColorRangeSlider(
        private val htmlId: String,
        private val colorGetter: (Int) -> String,
    ) : FormField<Int> {
        private val input: HTMLInputElement by Html.getElementById(htmlId)
        private val square: HTMLDivElement by Html.getElementById("$htmlId-square")
        private val text: HTMLSpanElement by Html.getElementById("$htmlId-text")

        override var value: Int
            get() = input.value.toInt()
            set(value) {
                input.value = value.toString()
            }

        /**
         * Render the field into the given [FlowContent].
         *
         * @param parent the parent [FlowContent] to render into
         * @param label the label for the input field
         * @param help optional help text used to describe the field in more detail
         * @param block optional block run in the scope of the [INPUT] tag for further customization.
         */
        fun render(parent: FlowContent, label: String, help: String = "", block: INPUT.() -> Unit = {}) {
            with(parent) {
                label {
                    htmlFor = htmlId
                    +label
                }
                div("d-flex align-items-center") {
                    input(type = InputType.range, classes = "custom-range") {
                        this.id = htmlId
                        style = "max-width: 300px;"
                        value = "0"
                        onInputFunction = {
                            onColorChange()
                        }
                        if (help.isNotBlank()) {
                            attributes["aria-describedby"] = "$htmlId-help"
                        }
                        block()
                    }
                    div("ml-2 border d-block") {
                        this.id = "$htmlId-square"
                        style = "width: 1.5em; height: 1.5em; border-color: black !important; " +
                                "background-color: ${colorGetter(0)}"
                    }
                    span("ml-2") {
                        this.id = "$htmlId-text"
                        +"0%"
                    }
                }

                if (help.isNotBlank()) {
                    help(htmlId, help)
                }
            }
        }

        private fun onColorChange() {
            square.style.backgroundColor = colorGetter(value)
            text.innerText = "${value}%"
        }
    }

    class Button(private val htmlId: String) {
        val button: HTMLButtonElement by Html.getElementById(htmlId)

        /**
         * Render the field into the given [FlowContent].
         *
         * @param parent the parent [FlowContent] to render into
         * @param label the label for the button
         * @param block optional block run in the scope of the [BUTTON] tag for further customization.
         */
        fun render(parent: FlowContent, label: String, block: BUTTON.() -> Unit = {}) {
            with(parent) {
                button(classes = "btn") {
                    type = ButtonType.submit
                    this.id = htmlId
                    +label
                    block()
                }
            }
        }
    }

    class ErrorMessage(private val htmlId: String) {
        private val container: HTMLDivElement by Html.getElementById(htmlId)

        fun render(parent: FlowContent) {
            with(parent) {
                div(classes = "alert alert-primary d-none") {
                    this.id = htmlId
                    role = "alert"
                }
            }
        }

        fun setMessage(message: String) {
            container.innerText = message
            if (message.isEmpty()) {
                container.addClass("d-none")
            } else {
                container.removeClass("d-none")
            }
        }
    }

    private fun FlowContent.formGroup(flexCols: Int? = 0, block: DIV.() -> Unit) {
        div(classes = "form-group") {
            if (flexCols != null) {
                classes = classes + "col-md-${flexCols}"
            }
            block()
        }
    }

    private fun FlowContent.help(htmlId: String, help: String) {
        small(classes = "form-text text-muted") {
            id = "$htmlId-help"
            +help
        }
    }
}