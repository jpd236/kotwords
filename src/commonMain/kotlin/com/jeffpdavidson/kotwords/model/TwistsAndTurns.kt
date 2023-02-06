package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.formats.FONT_FAMILY_TIMES_ROMAN
import com.jeffpdavidson.kotwords.formats.Pdf
import com.jeffpdavidson.kotwords.formats.PdfDocument
import com.jeffpdavidson.kotwords.formats.PdfFontFamily
import com.jeffpdavidson.kotwords.formats.Puzzleable

data class TwistsAndTurns(
    val title: String,
    val creator: String,
    val copyright: String,
    val description: String,
    val width: Int,
    val height: Int,
    val twistBoxSize: Int,
    val turnsAnswers: List<String>,
    val turnsClues: List<String>,
    val twistsClues: List<String>,
    val lightTwistsColor: String,
    val darkTwistsColor: String,
    val separateLightAndDarkTwists: Boolean = false,
    val numberTwists: Boolean = true,
    val sortTwists: Boolean = false,
) : Puzzleable() {
    init {
        require(width % twistBoxSize == 0 && height % twistBoxSize == 0) {
            "Width $width and height $height must evenly divide twist box size $twistBoxSize"
        }
        val neededTwistClues = (width / twistBoxSize) * (height / twistBoxSize)
        require(twistsClues.size == neededTwistClues) {
            "Grid size requires $neededTwistClues twist clues but have ${twistsClues.size}"
        }
        require(turnsAnswers.size == turnsClues.size) {
            "Have ${turnsAnswers.size} turns answers but ${turnsClues.size} turns clues"
        }
        val cellCount = turnsAnswers.fold(0) { acc, str -> acc + str.length }
        require(cellCount == width * height) {
            "Have $cellCount letters in turns answers but need ${width * height}"
        }
    }

    override suspend fun createPuzzle(): Puzzle {
        var x = 1
        var y = 1
        val turnsCluesList = mutableListOf<Puzzle.Clue>()
        val turnsWordsList = mutableListOf<Puzzle.Word>()
        val cellMap = mutableMapOf<Pair<Int, Int>, Puzzle.Cell>()
        turnsAnswers.forEachIndexed { answerIndex, answer ->
            val word = mutableListOf<Puzzle.Coordinate>()
            val clueNumber = answerIndex + 1
            answer.forEachIndexed { chIndex, ch ->
                val number = if (chIndex == 0) {
                    "$clueNumber"
                } else {
                    ""
                }
                val backgroundColor =
                    if (isLightTwist(x, y, twistBoxSize)) {
                        lightTwistsColor
                    } else {
                        darkTwistsColor
                    }
                val cell = Puzzle.Cell(
                    solution = "$ch",
                    backgroundColor = backgroundColor,
                    number = if (y % 2 == 1) number else "",
                    topRightNumber = if (y % 2 == 0) number else "",
                )
                cellMap[x to y] = cell
                word.add(Puzzle.Coordinate(x = x - 1, y = y - 1))

                // Move to the next cell
                if ((y - 1) % 2 == 0) {
                    if (x == width) {
                        y++
                    } else {
                        x++
                    }
                } else {
                    if (x == 1) {
                        y++
                    } else {
                        x--
                    }
                }
            }
            turnsWordsList.add(Puzzle.Word(clueNumber, word))
            turnsCluesList.add(Puzzle.Clue(clueNumber, "$clueNumber", turnsClues[answerIndex]))
        }

        val twistsClues = generateTwistsClues(numberTwists)
        val transformClues = { cluesList: List<Puzzle.Clue> ->
            if (sortTwists) cluesList.sortedBy { it.text } else cluesList
        }
        val twistsClueLists = if (separateLightAndDarkTwists) {
            listOf(
                Puzzle.ClueList("Light Twists", transformClues(twistsClues.lightTwistsClues)),
                Puzzle.ClueList("Dark Twists", transformClues(twistsClues.darkTwistsClues))
            )
        } else {
            val allTwistsClues = (twistsClues.lightTwistsClues + twistsClues.darkTwistsClues).sortedBy { it.wordId }
            listOf(Puzzle.ClueList("Twists", transformClues(allTwistsClues)))
        }

        return Puzzle(
            title,
            creator,
            copyright,
            description,
            generateGrid(cellMap),
            listOf(Puzzle.ClueList("Turns", turnsCluesList)) + twistsClueLists,
            turnsWordsList + twistsClues.twistsWords,
        )
    }

    suspend fun asPdf(
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
        // Use the regular grid drawing function, but padded on the left and right to have space for the "START" and
        // "END" text along with the arrow between each row.
        val originalGridSquareSize = gridWidth / this@TwistsAndTurns.width
        val startEndFontSize = originalGridSquareSize / 4
        val startEndMargin = startEndFontSize / 2
        val startWidth = getTextWidth("START", fontFamily.baseFont, startEndFontSize)
        val endWidth = getTextWidth("END", fontFamily.baseFont, startEndFontSize)
        val arrowWidth = endWidth / 2
        val leftPadding = startWidth + startEndMargin
        val rightPadding = startEndMargin + if (this@TwistsAndTurns.height % 2 == 0) {
            arrowWidth
        } else {
            endWidth
        }
        val adjustedGridWidth = gridWidth - leftPadding - rightPadding
        val drawGridResult = Pdf.drawGrid(
            document = document,
            grid = grid,
            blackSquareLightnessAdjustment = blackSquareLightnessAdjustment,
            gridWidth = adjustedGridWidth,
            gridX = gridX + leftPadding,
            gridY = gridY,
            fontFamily = fontFamily
        )
        val gridSquareSize = adjustedGridWidth / this@TwistsAndTurns.width

        // Draw the START and END text.
        beginText()
        setFont(fontFamily.baseFont, startEndFontSize)
        newLineAtOffset(gridX, gridY + drawGridResult.gridHeight - gridSquareSize / 2 - startEndFontSize / 2)
        drawText("START")
        endText()

        beginText()
        if (this@TwistsAndTurns.height % 2 == 0) {
            newLineAtOffset(gridX + startWidth - endWidth, gridY + gridSquareSize / 2 - startEndFontSize / 2)
        } else {
            newLineAtOffset(gridX + gridWidth - endWidth, gridY + gridSquareSize / 2 - startEndFontSize / 2)
        }
        drawText("END")
        endText()

        // Draw the arrows between each row.
        (0 until this@TwistsAndTurns.height - 1).forEach { y ->
            if (y % 2 == 0) {
                drawArrow(
                    x = gridX + gridWidth - rightPadding + startEndMargin,
                    y = gridY + drawGridResult.gridHeight - (y + 0.75f) * gridSquareSize,
                    width = arrowWidth,
                    height = 0.5f * gridSquareSize,
                    leftSide = false,
                )
            } else {
                drawArrow(
                    x = gridX + startWidth,
                    y = gridY + drawGridResult.gridHeight - (y + 0.75f) * gridSquareSize,
                    width = arrowWidth,
                    height = 0.5f * gridSquareSize,
                    leftSide = true,
                )
            }
        }

        Pdf.DrawGridResult(
            gridHeight = drawGridResult.gridHeight,
            bottomRowStartOffset = drawGridResult.bottomRowStartOffset + leftPadding
        )
    }

    private fun generateGrid(cellMap: Map<Pair<Int, Int>, Puzzle.Cell>): List<List<Puzzle.Cell>> {
        val grid = mutableListOf<MutableList<Puzzle.Cell>>()
        (0 until height).forEach { y ->
            val row = mutableListOf<Puzzle.Cell>()
            (0 until width).forEach { x ->
                row.add(cellMap[x + 1 to y + 1] ?: throw IllegalStateException())
            }
            grid.add(row)
        }
        return grid
    }

    private data class TwistsClues(
        val lightTwistsClues: List<Puzzle.Clue>,
        val darkTwistsClues: List<Puzzle.Clue>,
        val twistsWords: List<Puzzle.Word>,
    )

    private fun generateTwistsClues(numberTwists: Boolean): TwistsClues {
        val lightTwistsCluesList = mutableListOf<Puzzle.Clue>()
        val darkTwistsCluesList = mutableListOf<Puzzle.Clue>()
        val twistsWordsList = mutableListOf<Puzzle.Word>()
        var twistNumber = 0
        for (j in 0 until (height / twistBoxSize)) {
            for (i in 0 until (width / twistBoxSize)) {
                val cells = mutableListOf<Puzzle.Coordinate>()
                for (y in (j * twistBoxSize + 1)..((j + 1) * twistBoxSize)) {
                    for (x in (i * twistBoxSize + 1)..((i + 1) * twistBoxSize)) {
                        cells.add(Puzzle.Coordinate(x = x - 1, y = y - 1))
                    }
                }
                val wordId = (1001 + (j * (width / twistBoxSize)) + i)
                val word = Puzzle.Word(wordId, cells)
                val clueNumber = if (numberTwists) "${twistNumber + 1}" else ""
                val clue = Puzzle.Clue(wordId, clueNumber, twistsClues[twistNumber])
                if (isLightTwist(i * twistBoxSize + 1, j * twistBoxSize + 1, twistBoxSize)) {
                    lightTwistsCluesList.add(clue)
                } else {
                    darkTwistsCluesList.add(clue)
                }
                twistsWordsList.add(word)
                twistNumber++
            }
        }
        return TwistsClues(lightTwistsCluesList, darkTwistsCluesList, twistsWordsList)
    }

    private fun isLightTwist(x: Int, y: Int, twistBoxSize: Int): Boolean =
        (((x - 1) / twistBoxSize) % 2) == (((y - 1) / twistBoxSize) % 2)

    private fun PdfDocument.drawArrow(x: Float, y: Float, width: Float, height: Float, leftSide: Boolean) {
        val midX = if (leftSide) x - width else x + width
        val endY = y - height
        addLine(x, y, midX, y)
        addLine(midX, y, midX, endY)
        addLine(midX, endY, x, endY)
        val arrowWidth = width / 4f
        val arrowEndX = if (leftSide) x - arrowWidth else x + arrowWidth
        addLine(x, endY, arrowEndX, endY - arrowWidth)
        addLine(x, endY, arrowEndX, endY + arrowWidth)
        stroke()
    }
}