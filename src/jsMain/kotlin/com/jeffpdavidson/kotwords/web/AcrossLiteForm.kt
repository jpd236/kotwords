package com.jeffpdavidson.kotwords.web

import com.jeffpdavidson.kotwords.formats.AcrossLite
import com.jeffpdavidson.kotwords.js.Interop
import com.jeffpdavidson.kotwords.model.Puzzle
import com.jeffpdavidson.kotwords.web.html.FormFields
import com.jeffpdavidson.kotwords.web.html.Html.renderPage
import kotlin.js.Promise

/** Form to convert Across Lite files into JPZ files. */
internal class AcrossLiteForm {
    private val jpzForm = PuzzleFileForm(::createPuzzle, { getFileName() })
    private val file: FormFields.FileField = FormFields.FileField("file")

    init {
        renderPage {
            jpzForm.render(this, bodyBlock = {
                file.render(this, "Across Lite (.puz) file")
            })
        }
    }

    private fun createPuzzle(crosswordSolverSettings: Puzzle.CrosswordSolverSettings): Promise<Puzzle> {
        return Interop.readFile(file.getValue()).then {
            val crossword = AcrossLite(it).asCrossword()
            Puzzle.fromCrossword(crossword, crosswordSolverSettings)
        }
    }

    private fun getFileName(): String {
        return file.getValue().name.removeSuffix(".puz")
    }
}