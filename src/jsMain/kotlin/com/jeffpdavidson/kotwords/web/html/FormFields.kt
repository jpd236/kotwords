package com.jeffpdavidson.kotwords.web.html

import kotlinx.dom.addClass
import kotlinx.dom.removeClass
import kotlinx.html.DIV
import kotlinx.html.FlowContent
import kotlinx.html.INPUT
import kotlinx.html.InputType
import kotlinx.html.TEXTAREA
import kotlinx.html.classes
import kotlinx.html.div
import kotlinx.html.id
import kotlinx.html.input
import kotlinx.html.label
import kotlinx.html.role
import kotlinx.html.small
import kotlinx.html.textArea
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.files.File
import org.w3c.files.get

/** Classes to encapsulate and render form fields. */
internal object FormFields {

    /**
     * Create an <input> field for standard input types.
     *
     * @param htmlId the ID to be used for the input field (and as an ID prefix for associated tags)
     */
    class InputField(private val htmlId: String) {
        private val input: HTMLInputElement by Html.getElementById(htmlId)

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

        fun getValue(): String {
            return input.value.trim()
        }
    }

    /**
     * Create an <input> field for a check box.
     *
     * @param htmlId the ID to be used for the input field (and as an ID prefix for associated tags)
     */
    class CheckBoxField(private val htmlId: String) {
        private val input: HTMLInputElement by Html.getElementById(htmlId)

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

        fun getValue(): Boolean {
            return input.checked
        }
    }

    /**
     * Create an <input> field for a file selector.
     *
     * @param htmlId the ID to be used for the input field (and as an ID prefix for associated tags)
     */
    class FileField(private val htmlId: String) {
        private val input: HTMLInputElement by Html.getElementById(htmlId)

        /**
         * Render the file selector into the given [FlowContent].
         *
         * @param parent the parent [FlowContent] to render into
         * @param label the label for the input field
         * @param help optional help text used to describe the field in more detail
         * @param block optional block run in the scope of the [INPUT] tag for further customization.
         */
        fun render(parent: FlowContent, label: String, help: String = "", block: INPUT.() -> Unit = {}) {
            with(parent) {
                formGroup {
                    label {
                        htmlFor = htmlId
                        +label
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

        fun getValue(): File {
            if (input.files?.length == 0 || input.files!![0] == null) {
                throw IllegalArgumentException("No file selected")
            }
            return input.files!![0]!!
        }
    }

    /**
     * Create an <input> field for a multi-line text box.
     *
     * @param htmlId the ID to be used for the input field (and as an ID prefix for associated tags)
     */
    class TextBoxField(private val htmlId: String) {
        private val input: HTMLTextAreaElement by Html.getElementById(htmlId)

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

        fun getValue(trim: Boolean = true): String {
            val value = input.value
            return if (trim) value.trim() else value
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
                classes += "col-md-${flexCols}"
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