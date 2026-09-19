package com.jeffpdavidson.kotwords.web

import com.jeffpdavidson.kotwords.KotwordsInternal
import com.jeffpdavidson.kotwords.formats.AcrossLite
import com.jeffpdavidson.kotwords.formats.PdfFonts
import com.jeffpdavidson.kotwords.formats.pdf.GridCorner
import com.jeffpdavidson.kotwords.js.Interop
import com.jeffpdavidson.kotwords.model.Crossword
import com.jeffpdavidson.kotwords.model.GoingTooFar
import com.jeffpdavidson.kotwords.model.Puzzle
import com.jeffpdavidson.kotwords.web.html.FormFields
import com.jeffpdavidson.kotwords.web.html.Html.renderPage
import com.jeffpdavidson.kotwords.web.html.Tabs
import com.jeffpdavidson.kotwords.web.html.Tabs.tabs
import kotlinx.html.InputType
import kotlinx.html.dom.append

@JsExport
@KotwordsInternal
class GoingTooFarForm {
    private val manualEntryForm = PuzzleFileForm(
        "going-too-far",
        ::createPuzzleFromManualEntry,
        id = "manual-entry",
        createPdfFn = ::createPdfFromManualEntry,
    )
    private val quote = FormFields.TextBoxField("quote")
    private val grid = FormFields.TextBoxField("grid")
    private val acrossClues = FormFields.TextBoxField("across-clues")
    private val downClues = FormFields.TextBoxField("down-clues")
    private val shadedSquareColor = FormFields.InputField("shaded-square-color")

    private val puzFileForm = PuzzleFileForm(
        "going-too-far",
        ::createPuzzleFromPuzFile,
        { getFileName() },
        id = "puz-file",
        createPdfFn = ::createPdfFromPuzFile,
        enableSaveData = false,
        enableMetadataInput = false,
    )
    private val file: FormFields.FileField = FormFields.FileField("file")
    private val puzQuote = FormFields.TextBoxField("puz-quote")
    private val puzShadedSquareColor = FormFields.InputField("puz-shaded-square-color")

    init {
        renderPage {
            append.tabs(Tabs.Tab("manual-entry-tab", "Form") {
                manualEntryForm.render(this, bodyBlock = {
                    quote.render(this, "Quote") {
                        placeholder =
                            "The quote whose letters will fill the shaded squares. Must have the same length " +
                                    "as the number of black squares."
                        rows = "2"
                    }
                    grid.render(this, "Grid") {
                        placeholder = "Cells of the grid, separated into rows. Use periods for black squares, and " +
                                "hyphens for squares which should be empty. For rebus puzzles, separate each row's " +
                                "cells with whitespace."
                        rows = "15"
                    }
                    acrossClues.render(this, "Across clues") {
                        placeholder = "One clue per row. Omit clue numbers."
                        rows = "10"
                    }
                    downClues.render(this, "Down clues") {
                        placeholder = "One clue per row. Omit clue numbers."
                        rows = "10"
                    }
                }, advancedOptionsBlock = {
                    shadedSquareColor.render(this, "Shaded square color") {
                        type = InputType.color
                        value = "#C0C0C0"
                    }
                })
            }, Tabs.Tab("puz-file-tab", "PUZ file") {
                puzFileForm.render(this, bodyBlock = {
                    file.render(this, "Across Lite (.puz) file")
                    puzQuote.render(this, "Quote") {
                        placeholder =
                            "The quote whose letters will fill the shaded squares. Must have the same length " +
                                    "as the number of black squares."
                        rows = "2"
                    }
                }, advancedOptionsBlock = {
                    puzShadedSquareColor.render(this, "Shaded square color") {
                        type = InputType.color
                        value = "#C0C0C0"
                    }
                })
            })
        }
    }

    private suspend fun createPuzzleFromManualEntry(): Puzzle {
        val crossword = Crossword.fromRawInput(
            title = manualEntryForm.title,
            creator = manualEntryForm.creator,
            copyright = manualEntryForm.copyright,
            description = manualEntryForm.description,
            grid = grid.value,
            acrossClues = acrossClues.value,
            downClues = downClues.value,
        )
        return GoingTooFar(
            crossword = crossword,
            quote = quote.value,
            shadedSquareColor = shadedSquareColor.value,
        ).asPuzzle()
    }

    private suspend fun createPdfFromManualEntry(
        gridCorner: GridCorner,
        blackSquareLightnessAdjustment: Double,
    ): ByteArray =
        createPuzzleFromManualEntry().asPdf(
            fontFamily = PdfFonts.NOTO_FONT_FAMILY,
            blackSquareLightnessAdjustment = blackSquareLightnessAdjustment,
            gridCorner = gridCorner,
        )

    private suspend fun createPuzzleFromPuzFile(): Puzzle {
        val crossword = AcrossLite(Interop.readBlob(file.value)).asCrossword()
        return GoingTooFar(
            crossword = crossword,
            quote = puzQuote.value,
            shadedSquareColor = puzShadedSquareColor.value,
        ).asPuzzle()
    }

    private suspend fun createPdfFromPuzFile(
        gridCorner: GridCorner,
        blackSquareLightnessAdjustment: Double,
    ): ByteArray =
        createPuzzleFromPuzFile().asPdf(
            fontFamily = PdfFonts.NOTO_FONT_FAMILY,
            blackSquareLightnessAdjustment = blackSquareLightnessAdjustment,
            gridCorner = gridCorner,
        )

    private fun getFileName(): String {
        return file.value.name.removeSuffix(".puz")
    }
}
