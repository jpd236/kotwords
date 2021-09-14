package com.jeffpdavidson.kotwords.web

import com.jeffpdavidson.kotwords.readStringResource
import com.jeffpdavidson.kotwords.runTest
import com.jeffpdavidson.kotwords.web.html.FormFields
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PuzzleFileFormTest {
    private val textInput = FormFields.InputField("text-input")
    private val textBoxInput = FormFields.TextBoxField("text-box-input")
    private val checkBoxInput = FormFields.CheckBoxField("check-box-input")
    private val colorInput = FormFields.ColorRangeSlider("color-input") { "#abcdef" }
    private val form = PuzzleFileForm("test-format", { throw UnsupportedOperationException() }, id = "this-form")

    @Test
    fun saveJsonData() = runTest {
        val form = renderForm(withGivenValues = true)
        val expected = readStringResource(PuzzleFileFormTest::class, "save-data.json").replace("\r\n", "\n")
        val saveDataJson = form.createSaveDataJson()
        assertEquals(expected, saveDataJson)
    }

    @Test
    fun loadJsonData() = runTest {
        val form = renderForm(withGivenValues = false)
        val saveDataJson = readStringResource(PuzzleFileFormTest::class, "save-data.json").replace("\r\n", "\n")
        form.loadSaveDataJson(saveDataJson.encodeToByteArray())
        assertEquals("abcde", textInput.getValue())
        assertEquals("fghij", textBoxInput.getValue())
        assertTrue(checkBoxInput.getValue())
        assertEquals(50, colorInput.getValue())
        val colorText = document.getElementById("color-input-text") as HTMLElement
        assertEquals("50%", colorText.innerText)
    }

    private fun renderForm(withGivenValues: Boolean): PuzzleFileForm {
        form.render(window.document.body!!, bodyBlock = {
            textInput.render(this, "Text input") {
                if (withGivenValues) {
                    value = "abcde"
                }
            }
            textBoxInput.render(this, "Text box input") {
                if (withGivenValues) {
                    +"fghij"
                }
            }
            checkBoxInput.render(this, "Check box input") {
                if (withGivenValues) {
                    checked = true
                }
            }
        }, advancedOptionsBlock = {
            colorInput.render(this, "Color input") {
                if (withGivenValues) {
                    value = "50"
                }
            }
        })
        return form
    }
}