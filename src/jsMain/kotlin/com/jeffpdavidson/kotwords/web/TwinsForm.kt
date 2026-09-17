package com.jeffpdavidson.kotwords.web

import com.jeffpdavidson.kotwords.KotwordsInternal
import com.jeffpdavidson.kotwords.formats.PdfFonts
import com.jeffpdavidson.kotwords.model.Puzzle
import com.jeffpdavidson.kotwords.model.Twins
import com.jeffpdavidson.kotwords.web.html.FormFields
import com.jeffpdavidson.kotwords.web.html.Html
import kotlinx.html.div

@JsExport
@KotwordsInternal
class TwinsForm {
    private val form = PuzzleFileForm("twins", ::createPuzzle, createPdfFn = ::createPdf)
    private val grid1: FormFields.TextBoxField = FormFields.TextBoxField("grid-1")
    private val grid2: FormFields.TextBoxField = FormFields.TextBoxField("grid-2")
    private val acrossClues: FormFields.TextBoxField = FormFields.TextBoxField("across-clues")
    private val downClues: FormFields.TextBoxField = FormFields.TextBoxField("down-clues")

    init {
        Html.renderPage {
            form.render(this, bodyBlock = {
                div(classes = "form-row") {
                    grid1.render(this, "Grid 1 (Left)", flexCols = 6) {
                        placeholder = "Letters of the left grid, separated into rows. Use periods for black squares, " +
                                "and hyphens for squares which should be empty. For rebus puzzles, separate each " +
                                "row's cells with whitespace."
                        rows = "15"
                    }
                    grid2.render(this, "Grid 2 (Right)", flexCols = 6) {
                        placeholder =
                            "Letters of the right grid, separated into rows. Black square layout must match " +
                                    "Grid 1."
                        rows = "15"
                    }
                }
                acrossClues.render(this, "Across clues") {
                    placeholder = "One clue per row. Separate the two clues for each entry with a /. Omit clue numbers."
                    rows = "10"
                }
                downClues.render(this, "Down clues") {
                    placeholder = "One clue per row. Separate the two clues for each entry with a /. Omit clue numbers."
                    rows = "10"
                }
            })
        }
    }

    private suspend fun createPuzzle(): Puzzle = createTwins().asPuzzle()

    private suspend fun createPdf(blackSquareLightnessAdjustment: Double): ByteArray =
        createTwins().asPdf(
            fontFamily = PdfFonts.NOTO_FONT_FAMILY,
            blackSquareLightnessAdjustment = blackSquareLightnessAdjustment,
        )

    private fun createTwins(): Twins {
        return Twins.fromRawInput(
            title = form.title,
            creator = form.creator,
            copyright = form.copyright,
            description = form.description,
            grid1 = grid1.value,
            grid2 = grid2.value,
            acrossClues = acrossClues.value,
            downClues = downClues.value,
        )
    }
}
