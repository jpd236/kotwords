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
        val borders = mutableMapOf<Pair<Int, Int>, MutableSet<Puzzle.BorderDirection>>()
        val acrossAnswerLengthRows = acrossAnswerLengths.value.trimmedLines()
        if (acrossAnswerLengthRows.isNotEmpty()) {
            acrossAnswerLengthRows.forEachIndexed { y, lengths ->
                var x = 0
                lengths.split("\\s+".toRegex()).forEachIndexed { i, length ->
                    if (i > 0) {
                        borders.getOrPut(x to y) { mutableSetOf() }.add(Puzzle.BorderDirection.LEFT)
                    }
                    x += length.toInt()
                }
            }
        }
        val downAnswerLengthRows = downAnswerLengths.value.trimmedLines()
        if (downAnswerLengthRows.isNotEmpty()) {
            downAnswerLengthRows.forEachIndexed { x, lengths ->
                var y = 0
                lengths.split("\\s+".toRegex()).forEachIndexed { i, length ->
                    if (i > 0) {
                        borders.getOrPut(x to y) { mutableSetOf() }.add(Puzzle.BorderDirection.TOP)
                    }
                    y += length.toInt()
                }
            }
        }

        val crosswordGrid = grid.value.uppercase().trimmedLines().mapIndexed { y, row ->
            val columns = if (row.contains("\\s".toRegex())) {
                row.split("\\s+".toRegex())
            } else {
                row.map { "$it" }
            }
            columns.mapIndexed { x, col ->
                when (col) {
                    "." -> Puzzle.Cell(cellType = Puzzle.CellType.BLOCK)
                    "-" -> Puzzle.Cell(
                        solution = "",
                        borderDirections = borders.getOrElse(x to y) { setOf() },
                    )
                    else -> Puzzle.Cell(
                        solution = col,
                        borderDirections = borders.getOrElse(x to y) { setOf() },
                    )
                }
            }
        }
        require(crosswordGrid.isNotEmpty() && crosswordGrid.all { it.size == crosswordGrid[0].size }) {
            "Crossword grid is not square"
        }

        val orderedAcrossClues = acrossClues.value.trimmedLines()
        val orderedDownClues = downClues.value.trimmedLines()
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
            "Too many down clues; expected $currentDownClue from grid but have ${orderedDownClues.size}"
        }

        return Crossword(
            title = manualEntryForm.title,
            creator = manualEntryForm.creator,
            copyright = manualEntryForm.copyright,
            description = manualEntryForm.description,
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

    private suspend fun createPuzzleFromPuzFile(): Puzzle = AcrossLite(Interop.readBlob(file.value)).asPuzzle()

    private suspend fun createPdfFromPuzFile(blackSquareLightnessAdjustment: Float): ByteArray =
        createPuzzleFromPuzFile().asPdf(
            fontFamily = PdfFonts.getNotoFontFamily(),
            blackSquareLightnessAdjustment = blackSquareLightnessAdjustment,
        )

    private fun getFileName(): String {
        return file.value.name.removeSuffix(".puz")
    }
}