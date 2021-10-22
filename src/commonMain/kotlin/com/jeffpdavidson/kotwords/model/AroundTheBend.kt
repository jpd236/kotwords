package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.formats.FONT_FAMILY_TIMES_ROMAN
import com.jeffpdavidson.kotwords.formats.Pdf
import com.jeffpdavidson.kotwords.formats.Pdf.asPdf
import com.jeffpdavidson.kotwords.formats.PdfDocument
import com.jeffpdavidson.kotwords.formats.PdfFontFamily
import com.jeffpdavidson.kotwords.formats.Puzzleable

data class AroundTheBend(
    val title: String,
    val creator: String,
    val copyright: String,
    val description: String,
    var rows: List<String>,
    val clues: List<String>
) : Puzzleable {
    override fun asPuzzle(): Puzzle {
        val maxWidth = rows.maxByOrNull { it.length }!!.length
        val grid = rows.mapIndexed { y, row ->
            val padding = maxWidth - row.length
            (0 until padding).map {
                Puzzle.Cell(cellType = Puzzle.CellType.VOID)
            } + row.mapIndexed { i, ch ->
                Puzzle.Cell(
                    solution = "$ch",
                    number = if (i == 0) "${y + 1}" else ""
                )
            }
        }
        val (puzzleClues, puzzleWords) = clues.mapIndexed { y, clue ->
            val nextY = (y + 1) % grid.size
            Puzzle.Clue(
                y,
                "${y + 1}",
                clue
            ) to Puzzle.Word(
                y,
                grid[y].mapIndexedNotNull { x, cell ->
                    if (cell.cellType == Puzzle.CellType.VOID) null else Puzzle.Coordinate(x = x, y = y)
                } + grid[nextY].mapIndexedNotNull { x, cell ->
                    if (cell.cellType == Puzzle.CellType.VOID) null else Puzzle.Coordinate(x = x, y = nextY)
                }.reversed()
            )
        }.unzip()
        return Puzzle(
            title = title,
            creator = creator,
            copyright = copyright,
            description = description,
            grid = grid,
            clues = listOf(Puzzle.ClueList("Clues", puzzleClues)),
            words = puzzleWords,
        )
    }

    fun asPdf(
        fontFamily: PdfFontFamily = FONT_FAMILY_TIMES_ROMAN,
        blackSquareLightnessAdjustment: Float = 0f,
    ): ByteArray {
        val puzzle = asPuzzle()
        return puzzle.asPdf(fontFamily, blackSquareLightnessAdjustment, ::drawGrid)
    }

    private fun drawGrid(
        document: PdfDocument,
        grid: List<List<Puzzle.Cell>>,
        blackSquareLightnessAdjustment: Float,
        gridWidth: Float,
        gridX: Float,
        gridY: Float,
        fontFamily: PdfFontFamily,
    ): Pdf.DrawGridResult = document.run {
        // Use the regular grid drawing function, but padded on the right to have space for the arrows between each row.
        val originalGridSquareSize = gridWidth / grid.maxOf { it.size }
        val arrowWidth = originalGridSquareSize / 2
        val adjustedGridWidth = gridWidth - arrowWidth
        val drawGridResult = Pdf.drawGrid(
            document = document,
            grid = grid,
            blackSquareLightnessAdjustment = blackSquareLightnessAdjustment,
            gridWidth = adjustedGridWidth,
            gridX = gridX,
            gridY = gridY,
            fontFamily = fontFamily
        )
        drawGridResult
    }
}