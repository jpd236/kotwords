package com.jeffpdavidson.kotwords.web

import com.jeffpdavidson.kotwords.KotwordsInternal
import com.jeffpdavidson.kotwords.formats.AcrossLite
import com.jeffpdavidson.kotwords.js.Interop
import com.jeffpdavidson.kotwords.model.Puzzle
import com.jeffpdavidson.kotwords.web.html.FormFields
import com.jeffpdavidson.kotwords.web.html.Html.renderPage

/** Form to convert Across Lite files into JPZ files. */
@JsExport
@KotwordsInternal
class AcrossLiteForm {
    private val jpzForm = PuzzleFileForm("across-lite", ::createPuzzle, { getFileName() }, enableSaveData = false)
    private val file: FormFields.FileField = FormFields.FileField("file")

    init {
        renderPage {
            jpzForm.render(this, bodyBlock = {
                file.render(this, "Across Lite (.puz) file")
            })
        }
    }

    private suspend fun createPuzzle(): Puzzle = AcrossLite(Interop.readFile(file.getValue())).asPuzzle()

    private fun getFileName(): String {
        return file.getValue().name.removeSuffix(".puz")
    }
}