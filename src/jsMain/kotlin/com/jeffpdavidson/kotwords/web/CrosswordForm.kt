package com.jeffpdavidson.kotwords.web

import com.jeffpdavidson.kotwords.KotwordsInternal
import com.jeffpdavidson.kotwords.formats.AcrossLite
import com.jeffpdavidson.kotwords.formats.Pdf.asPdf
import com.jeffpdavidson.kotwords.formats.PdfFonts
import com.jeffpdavidson.kotwords.js.Interop
import com.jeffpdavidson.kotwords.model.Crossword
import com.jeffpdavidson.kotwords.model.Puzzle
import com.jeffpdavidson.kotwords.util.trimmedLines
import com.jeffpdavidson.kotwords.web.html.FormFields
import com.jeffpdavidson.kotwords.web.html.Html.renderPage
import com.jeffpdavidson.kotwords.web.html.Tabs
import com.jeffpdavidson.kotwords.web.html.Tabs.tabs
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
    private val title: FormFields.InputField = FormFields.InputField("title")
    private val creator: FormFields.InputField = FormFields.InputField("creator")
    private val copyright: FormFields.InputField = FormFields.InputField("copyright")
    private val description: FormFields.TextBoxField = FormFields.TextBoxField("description")
    private val grid: FormFields.TextBoxField = FormFields.TextBoxField("grid")
    private val acrossClues: FormFields.TextBoxField = FormFields.TextBoxField("across-clues")
    private val downClues: FormFields.TextBoxField = FormFields.TextBoxField("down-clues")

    private val puzFileForm = PuzzleFileForm(
        "crossword",
        ::createPuzzleFromPuzFile,
        { getFileName() },
        id = "puz-file",
        createPdfFn = ::createPdfFromPuzFile,
        enableSaveData = false,
    )
    private val file: FormFields.FileField = FormFields.FileField("file")

    init {
        renderPage {
            append.tabs(Tabs.Tab("manual-entry-tab", "Form") {
                manualEntryForm.render(this, bodyBlock = {
                    this@CrosswordForm.title.render(this, "Title")
                    creator.render(this, "Creator (optional)")
                    copyright.render(this, "Copyright (optional)")
                    description.render(this, "Description (optional)") {
                        rows = "5"
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
                })
            }, Tabs.Tab("puz-file-tab", "PUZ file") {
                puzFileForm.render(this, bodyBlock = {
                    file.render(this, "Across Lite (.puz) file")
                })
            })
        }
    }

    private suspend fun createPuzzleFromManualEntry(): Puzzle {
        val crosswordGrid = grid.getValue().uppercase().trimmedLines().map { row ->
            val columns = if (row.contains("\\s".toRegex())) {
                row.split("\\s+".toRegex())
            } else {
                row.map { "$it" }
            }
            columns.map { col ->
                when (col) {
                    "." -> Puzzle.Cell(cellType = Puzzle.CellType.BLOCK)
                    "-" -> Puzzle.Cell(solution = "")
                    else -> Puzzle.Cell(solution = col)
                }
            }
        }
        require (crosswordGrid.isNotEmpty() && crosswordGrid.all { it.size == crosswordGrid[0].size }) {
            "Crossword grid is not square"
        }

        val orderedAcrossClues = acrossClues.getValue().trimmedLines()
        val orderedDownClues = downClues.getValue().trimmedLines()
        val acrossCluesByClueNumber = mutableMapOf<Int, String>()
        val downCluesByClueNumber = mutableMapOf<Int, String>()
        var currentAcrossClue = 0
        var currentDownClue = 0
        Crossword.forEachNumberedCell(crosswordGrid) { _, _, clueNumber, isAcross, isDown ->
            if (isAcross) {
                require(currentAcrossClue in orderedAcrossClues.indices) {
                    "Number of across clues does not match grid"
                }
                acrossCluesByClueNumber[clueNumber] = orderedAcrossClues[currentAcrossClue++]
            }
            if (isDown) {
                require(currentDownClue in orderedDownClues.indices) {
                    "Number of down clues does not match grid"
                }
                downCluesByClueNumber[clueNumber] = orderedDownClues[currentDownClue++]
            }
        }
        require(currentAcrossClue == orderedAcrossClues.size) {
            "Too many across clues; expected $currentAcrossClue from grid but have ${orderedAcrossClues.size}"
        }
        require(currentDownClue == orderedDownClues.size) {
            "Too many across clues; expected $currentDownClue from grid but have ${orderedDownClues.size}"
        }

        return Crossword(
            title = title.getValue(),
            creator = creator.getValue(),
            copyright = copyright.getValue(),
            description = description.getValue(),
            grid = crosswordGrid,
            acrossClues = acrossCluesByClueNumber,
            downClues = downCluesByClueNumber,
        ).asPuzzle()
    }

    private suspend fun createPdfFromManualEntry(blackSquareLightnessAdjustment: Float): ByteArray =
        createPuzzleFromManualEntry().asPdf(
            fontFamily = PdfFonts.getNotoFontFamily(),
            blackSquareLightnessAdjustment = blackSquareLightnessAdjustment,
        )

    private suspend fun createPuzzleFromPuzFile(): Puzzle = AcrossLite(Interop.readBlob(file.getValue())).asPuzzle()

    private suspend fun createPdfFromPuzFile(blackSquareLightnessAdjustment: Float): ByteArray =
        createPuzzleFromPuzFile().asPdf(
            fontFamily = PdfFonts.getNotoFontFamily(),
            blackSquareLightnessAdjustment = blackSquareLightnessAdjustment,
        )

    private fun getFileName(): String {
        return file.getValue().name.removeSuffix(".puz")
    }
}