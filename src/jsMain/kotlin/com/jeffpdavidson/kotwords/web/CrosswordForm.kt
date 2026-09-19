package com.jeffpdavidson.kotwords.web

import com.jeffpdavidson.kotwords.KotwordsInternal
import com.jeffpdavidson.kotwords.formats.AcrossLite
import com.jeffpdavidson.kotwords.formats.PdfFonts
import com.jeffpdavidson.kotwords.formats.pdf.GridCorner
import com.jeffpdavidson.kotwords.js.Interop
import com.jeffpdavidson.kotwords.model.Crossword
import com.jeffpdavidson.kotwords.model.Puzzle
import com.jeffpdavidson.kotwords.web.html.FormFields
import com.jeffpdavidson.kotwords.web.html.Html.renderPage
import com.jeffpdavidson.kotwords.web.html.Tabs
import com.jeffpdavidson.kotwords.web.html.Tabs.tabs
import kotlinx.html.div
import kotlinx.html.dom.append

@JsExport
@KotwordsInternal
class CrosswordForm {
    private val manualEntryForm = PuzzleFileForm(
        "crossword",
        ::createPuzzleFromManualEntry,
        id = "manual-entry",
        createPdfFn = ::createPdfFromManualEntry,
    )
    private val grid = FormFields.TextBoxField("grid")
    private val acrossAnswerLengths = FormFields.TextBoxField("across-answer-lengths")
    private val downAnswerLengths = FormFields.TextBoxField("down-answer-lengths")
    private val acrossClues = FormFields.TextBoxField("across-clues")
    private val downClues = FormFields.TextBoxField("down-clues")

    private val puzFileForm = PuzzleFileForm(
        "crossword",
        ::createPuzzleFromPuzFile,
        { getFileName() },
        id = "puz-file",
        createPdfFn = ::createPdfFromPuzFile,
        enableSaveData = false,
        enableMetadataInput = false,
    )
    private val file: FormFields.FileField = FormFields.FileField("file")

    init {
        renderPage {
            append.tabs(Tabs.Tab("manual-entry-tab", "Form") {
                manualEntryForm.render(this, bodyBlock = {
                    grid.render(this, "Grid") {
                        placeholder = "Cells of the grid, separated into rows. Use periods for black squares, and " +
                                "hyphens for squares which should be empty. For rebus puzzles, separate each row's " +
                                "cells with whitespace."
                        rows = "15"
                    }
                    div(classes = "form-row") {
                        acrossAnswerLengths.render(this, "Across answer lengths (for barred grids)", flexCols = 6) {
                            rows = "15"
                            placeholder = "Lengths of the across answers; one line per row. Separate multiple " +
                                    "answers for a row with whitespace. Use \"1\" to represent unchecked or black " +
                                    "squares. Only required for barred grids."
                        }
                        downAnswerLengths.render(this, "Down answer lengths (for barred grids)", flexCols = 6) {
                            rows = "15"
                            placeholder = "Lengths of the down answers; one line per column. Separate multiple " +
                                    "answers for a column with whitespace. Use \"1\" to represent unchecked or black " +
                                    "squares. Only required for barred grids."
                        }
                    }
                    acrossClues.render(this, "Across clues") {
                        placeholder = "One clue per row. Omit clue numbers."
                        rows = "10"
                    }
                    downClues.render(this, "Down clues") {
                        placeholder = "One clue per row. Omit clue numbers."
                        rows = "10"
                    }
                })
            }, Tabs.Tab("puz-file-tab", "PUZ file") {
                puzFileForm.render(this, bodyBlock = {
                    file.render(this, "Across Lite (.puz) file")
                })
            })
        }
    }

    private suspend fun createPuzzleFromManualEntry(): Puzzle {
        return Crossword.fromRawInput(
            title = manualEntryForm.title,
            creator = manualEntryForm.creator,
            copyright = manualEntryForm.copyright,
            description = manualEntryForm.description,
            grid = grid.value,
            acrossClues = acrossClues.value,
            downClues = downClues.value,
            acrossAnswerLengths = acrossAnswerLengths.value,
            downAnswerLengths = downAnswerLengths.value,
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

    private suspend fun createPuzzleFromPuzFile(): Puzzle = AcrossLite(Interop.readBlob(file.value)).asPuzzle()

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