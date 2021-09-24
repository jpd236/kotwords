package com.jeffpdavidson.kotwords.web

import com.jeffpdavidson.kotwords.KotwordsInternal
import com.jeffpdavidson.kotwords.formats.AcrossLite
import com.jeffpdavidson.kotwords.formats.Pdf.asPdf
import com.jeffpdavidson.kotwords.formats.PdfFonts
import com.jeffpdavidson.kotwords.js.Interop
import com.jeffpdavidson.kotwords.model.Puzzle
import com.jeffpdavidson.kotwords.web.html.FormFields
import com.jeffpdavidson.kotwords.web.html.Html.renderPage

/** Form to convert Across Lite files into other formats. */
@JsExport
@KotwordsInternal
class AcrossLiteForm {
    private val form = PuzzleFileForm(
        "across-lite",
        ::createPuzzle,
        { getFileName() },
        createPdfFn = ::createPdf,
        enableSaveData = false,
    )
    private val file: FormFields.FileField = FormFields.FileField("file")

    init {
        renderPage {
            form.render(this, bodyBlock = {
                file.render(this, "Across Lite (.puz) file")
            })
        }
    }

    private suspend fun createPuzzle(): Puzzle = AcrossLite(Interop.readFile(file.getValue())).asPuzzle()

    private suspend fun createPdf(blackSquareLightnessAdjustment: Float): ByteArray =
        AcrossLite(Interop.readFile(file.getValue())).asPuzzle().asPdf(
            fontFamily = PdfFonts.getNotoFontFamily(),
            blackSquareLightnessAdjustment = blackSquareLightnessAdjustment,
        )

    private fun getFileName(): String {
        return file.getValue().name.removeSuffix(".puz")
    }
}