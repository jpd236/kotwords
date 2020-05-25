package com.jeffpdavidson.kotwords.web.html

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
import kotlinx.html.small
import kotlinx.html.textArea

/** Templates for constructing form fields. */
object FormFields {

    /**
     * Create an <input> field for standard input types.
     *
     * @param id the ID to be used for the input field (and as an ID prefix for associated tags)
     * @param label the label for the input field
     * @param help optional help text used to describe the field in more detail
     * @param flexCols optional number of columns this field should take up in the parent container.
     * @param block optional block run in the scope of the [INPUT] tag for further customization.
     */
    fun FlowContent.inputField(id: String, label: String, help: String = "", flexCols: Int? = null,
                               block: INPUT.() -> Unit = {}) {
        formGroup(flexCols) {
            label {
                htmlFor = id
                +label
            }
            input(classes = "form-control") {
                this.id = id
                this.type = InputType.text
                if (help.isNotBlank()) {
                    attributes["aria-describedby"] = "$id-help"
                }
                block()
            }
            if (help.isNotBlank()) {
                help(id, help)
            }
        }
    }

    /**
     * Create an <input> field for a checkbox.
     *
     * @param id the ID to be used for the input field (and as an ID prefix for associated tags)
     * @param label the label for the input field
     * @param block optional block run in the scope of the [INPUT] tag for further customization.
     */
    fun FlowContent.checkField(id: String, label: String, block: INPUT.() -> Unit = {}) {
        formGroup {
            div(classes = "form-check") {
                input(classes = "form-check-input") {
                    this.id = id
                    this.type = InputType.checkBox
                    block()
                }
                label(classes = "form-check-label") {
                    htmlFor = id
                    +label
                }
            }
        }
    }

    /**
     * Create an <input> field for a file selector.
     *
     * @param id the ID to be used for the input field (and as an ID prefix for associated tags)
     * @param label the label for the input field
     * @param help optional help text used to describe the field in more detail
     * @param block optional block run in the scope of the [INPUT] tag for further customization.
     */
    fun FlowContent.fileField(id: String, label: String, help: String = "", block: INPUT.() -> Unit = {}) {
        formGroup {
            label {
                htmlFor = id
                +label
            }
            input(classes = "form-control-file") {
                this.id = id
                this.type = InputType.file
                if (help.isNotBlank()) {
                    attributes["aria-describedby"] = "$id-help"
                }
                block()
            }
            if (help.isNotBlank()) {
                help(id, help)
            }
        }
    }

    /**
     * Create an <input> field for a multi-line text box.
     *
     * @param id the ID to be used for the input field (and as an ID prefix for associated tags)
     * @param label the label for the input field
     * @param help optional help text used to describe the field in more detail
     * @param flexCols optional number of columns this field should take up in the parent container.
     * @param block optional block run in the scope of the [INPUT] tag for further customization.
     */
    fun FlowContent.textBoxField(id: String, label: String, help: String = "", flexCols: Int? = null,
                                 block: TEXTAREA.() -> Unit = {}) {
        formGroup(flexCols) {
            label {
                htmlFor = id
                +label
            }
            textArea(classes = "form-control") {
                this.id = id
                if (help.isNotBlank()) {
                    attributes["aria-describedby"] = "$id-help"
                }
                block()
            }
            if (help.isNotBlank()) {
                help(id, help)
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

    private fun FlowContent.help(id: String, help: String) {
        small(classes = "form-text text-muted") {
            this.id = "$id-help"
            +help
        }
    }
}