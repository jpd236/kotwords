package com.jeffpdavidson.kotwords.web

import com.jeffpdavidson.kotwords.formats.AcrossLite
import com.jeffpdavidson.kotwords.js.Interop
import com.jeffpdavidson.kotwords.model.Puzzle
import com.jeffpdavidson.kotwords.web.html.FormFields.fileField
import com.jeffpdavidson.kotwords.web.html.Html
import com.jeffpdavidson.kotwords.web.html.Html.getElementById
import com.jeffpdavidson.kotwords.web.html.Html.renderPage
import kotlinx.html.dom.append
import org.w3c.dom.HTMLInputElement
import kotlin.js.Promise

private const val ID_FILE = "file"

/** Form to convert Across Lite files into JPZ files. */
class AcrossLiteForm {
    private val jpzForm = JpzForm(::createPuzzle, { getFileName() })
    private val file: HTMLInputElement by getElementById(ID_FILE)

    init {
        renderPage {
            with(jpzForm) {
                append.jpzForm(bodyBlock = {
                    fileField(ID_FILE, "Across Lite (.puz) file")
                })
            }
        }
    }

    private fun createPuzzle(crosswordSolverSettings: Puzzle.CrosswordSolverSettings): Promise<Puzzle> {
        val puzFile = Html.getSelectedFile(file)
        return Interop.readFile(puzFile).then {
            val crossword = AcrossLite(it).asCrossword()
            Puzzle.fromCrossword(crossword, crosswordSolverSettings)
        }
    }

    private fun getFileName(): String {
        return Html.getSelectedFile(file).name.replace(".puz", ".jpz")
    }
}