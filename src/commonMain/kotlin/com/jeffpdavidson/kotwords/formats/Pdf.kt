package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.model.Crossword

/** Extension functions to render crosswords as PDFs. */
object Pdf {
    // Constants/functions dictating the PDF style.

    /** Top/bottom and left/right margin size. */
    private const val MARGIN = 36f

    /** Size of the puzzle title. */
    private const val TITLE_SIZE = 16f

    /** Size of the puzzle author. */
    private const val AUTHOR_SIZE = 14f

    /** Size of the puzzle notes. */
    private const val NOTES_SIZE = 12f

    /** Size of the puzzle copyright. */
    private const val COPYRIGHT_SIZE = 9f

    /** Size of the space between adjacent clue columns. */
    private const val COLUMN_PADDING = 12f

    /** X offset of clue numbers in grid squares. (The Y offset is based on font size). */
    private const val GRID_NUMBER_X_OFFSET = 2f

    /** Color to use for "black" squares from 0 (black) to 1 (white). */
    private const val GRID_BLACK_COLOR = 0.75f

    /** Maximum size of clues. */
    private const val CLUE_TEXT_MAX_SIZE = 11f

    /** Minimum size of clues. */
    private const val CLUE_TEXT_MIN_SIZE = 5f

    /** Amount to shrink the font size for each render attempt if the clues do not fit on one page. */
    private const val CLUE_TEXT_SIZE_DELTA = 0.1f

    /** Returns the number of columns to use for the clues. */
    private fun getClueColumns(gridRows: Int): Int = if (gridRows >= 15) {
        4
    } else {
        3
    }

    /** Returns the size of the grid number text. */
    private fun getGridNumberSize(gridRows: Int): Float = if (gridRows <= 17) {
        8f
    } else {
        6f
    }

    /** Returns the percentage of the content width to use for the grid. */
    private fun getGridWidthPercentage(gridRows: Int): Float =
        if (gridRows >= 15) {
            0.7f
        } else {
            0.6f
        }

    /**
     * Render this crossword as a PDF document.
     *
     * Inspired by [puz2pdf](https://sourceforge.net/projects/puz2pdf) and
     * [Crossword Nexus's PDF converter](https://crosswordnexus.com/js/puz_functions.js).
     */
    fun Crossword.asPdf(): ByteArray = PdfDocument().run {
        val pageWidth = width
        val pageHeight = height
        val headerWidth = pageWidth - 2 * MARGIN
        val gridRows = grid.size
        val gridCols = grid[0].size
        val gridWidth = getGridWidthPercentage(gridRows) * headerWidth
        val gridHeight = gridWidth * gridRows / gridCols
        val gridSquareSize = gridHeight / gridRows
        val gridNumberSize = getGridNumberSize(gridRows)
        val gridX = pageWidth - MARGIN - gridWidth
        val gridY = MARGIN + COPYRIGHT_SIZE
        val columns = getClueColumns(gridRows)
        val columnWidth = (headerWidth - (columns - 1) * COLUMN_PADDING) / columns
        val titleX = MARGIN
        val titleY = pageHeight - MARGIN

        var positionY = titleY
        fun newLine(offsetY: Float) {
            newLineAtOffset(0f, -offsetY)
            positionY -= offsetY
        }

        beginText()
        newLineAtOffset(titleX, titleY)

        setFont(Font.TIMES_BOLD, TITLE_SIZE)
        drawMultiLineText(title, TITLE_SIZE, headerWidth, ::newLine)

        setFont(Font.TIMES_ROMAN, AUTHOR_SIZE)
        drawMultiLineText(author, AUTHOR_SIZE, headerWidth, ::newLine)

        if (notes.isNotBlank()) {
            setFont(Font.TIMES_ITALIC, NOTES_SIZE)
            drawMultiLineText(notes, NOTES_SIZE, headerWidth, ::newLine)
        }

        newLine(AUTHOR_SIZE)

        // Try progressively smaller clue sizes until we find one small enough to fit every clue on one page.
        setFont(Font.TIMES_ROMAN, CLUE_TEXT_MAX_SIZE)
        val clueTextSizes =
            generateSequence(CLUE_TEXT_MAX_SIZE) { it - CLUE_TEXT_SIZE_DELTA }.takeWhile { it >= CLUE_TEXT_MIN_SIZE }
        val bestTextSize = clueTextSizes.firstOrNull { clueTextSize ->
            showClueLists(
                crossword = this@asPdf,
                columnWidth = columnWidth,
                columns = columns,
                clueTopY = positionY,
                gridY = gridY,
                gridHeight = gridHeight,
                clueTextSize = clueTextSize,
                render = false
            )
        }
        require(bestTextSize != null) {
            "Clues do not fit on a single page"
        }
        showClueLists(
            crossword = this@asPdf,
            columnWidth = columnWidth,
            columns = columns,
            clueTopY = positionY,
            gridY = gridY,
            gridHeight = gridHeight,
            clueTextSize = bestTextSize,
            render = true
        )

        endText()

        setColor(GRID_BLACK_COLOR, GRID_BLACK_COLOR, GRID_BLACK_COLOR)
        Crossword.forEachSquare(grid) { x, y, clueNumber, _, _, square ->
            val squareX = gridX + x * gridSquareSize
            val squareY = gridY + gridHeight - (y + 1) * gridSquareSize
            addRect(squareX, squareY, gridSquareSize, gridSquareSize)
            if (square.isBlack) {
                fillAndStroke()
            } else {
                stroke()
                if (square.isCircled) {
                    addCircle(squareX, squareY, gridSquareSize / 2)
                    stroke()
                }
                if (clueNumber != null) {
                    setColor(0f, 0f, 0f)
                    beginText()
                    newLineAtOffset(
                        gridX + x * gridSquareSize + GRID_NUMBER_X_OFFSET,
                        gridY + gridHeight - y * gridSquareSize - gridNumberSize
                    )
                    setFont(Font.TIMES_ROMAN, gridNumberSize)
                    drawText(clueNumber.toString())
                    endText()
                    setColor(GRID_BLACK_COLOR, GRID_BLACK_COLOR, GRID_BLACK_COLOR)
                }
            }
        }

        setColor(0f, 0f, 0f)

        beginText()
        newLineAtOffset(gridX, MARGIN)
        setFont(Font.TIMES_ROMAN, COPYRIGHT_SIZE)
        drawText(copyright)
        endText()

        toByteArray()
    }

    private fun PdfDocument.drawMultiLineText(
        text: String, fontSize: Float, lineWidth: Float, newLineFn: (Float) -> Unit
    ) {
        splitTextToLines(this, text, fontSize, lineWidth).forEach { line ->
            drawText(line)
            newLineFn(fontSize)
        }
    }

    /** Split [text] into lines (using spaces as word separators) to fit the given [lineWidth]. */
    internal fun splitTextToLines(
        document: PdfDocument, text: String, fontSize: Float, lineWidth: Float
    ): List<String> {
        val lines = mutableListOf<StringBuilder>()
        var currentLine = StringBuilder()
        lines += currentLine
        var currentLineLength = 0f
        var currentSeparator = ""
        text.split(" ").forEach { word ->
            val separatorLength = document.getTextWidth(currentSeparator, fontSize)
            val wordLength = document.getTextWidth(word, fontSize)
            if (currentLineLength + separatorLength + wordLength > lineWidth) {
                // This word pushes us over the line length limit, so we'll need a new line.
                if (wordLength > lineWidth) {
                    // Word is too long to fit on a single line; have to chop by letter.
                    word.forEach { ch ->
                        val charLength = document.getTextWidth(ch.toString(), fontSize)
                        val wordSeparatorLengthPts = document.getTextWidth(currentSeparator, fontSize)
                        if (currentLineLength + wordSeparatorLengthPts + charLength > lineWidth) {
                            currentLine = StringBuilder(ch.toString())
                            lines += currentLine
                            currentLineLength = charLength
                        } else {
                            currentLine.append(currentSeparator).append(ch)
                            currentLineLength += wordSeparatorLengthPts + charLength
                        }
                        currentSeparator = ""
                    }
                } else {
                    // Start a new line with this word.
                    currentLine = StringBuilder(word)
                    lines += currentLine
                    currentLineLength = wordLength
                }
            } else {
                // This word fits, so continue the current line with it.
                currentLine.append(currentSeparator).append(word)
                currentLineLength += separatorLength + wordLength
            }
            currentSeparator = " "
        }
        return lines.map { it.toString() }
    }

    private data class CluePosition(
        val positionY: Float,
        val column: Int,
        val columnBottomY: Float,
    )

    private fun PdfDocument.showClueLists(
        crossword: Crossword,
        columnWidth: Float,
        columns: Int,
        clueTopY: Float,
        gridY: Float,
        gridHeight: Float,
        clueTextSize: Float,
        render: Boolean
    ): Boolean {
        var positionY = clueTopY
        if (render) {
            setFont(Font.TIMES_ROMAN, clueTextSize)
        }
        val (success, cluePosition) =
            showClueList(
                clues = crossword.acrossClues,
                header = "ACROSS",
                columnWidth = columnWidth,
                columns = columns,
                clueTopY = clueTopY,
                gridY = gridY,
                gridHeight = gridHeight,
                clueTextSize = clueTextSize,
                cluePosition = CluePosition(positionY = positionY, column = 0, columnBottomY = gridY),
                render = render
            )
        if (!success) {
            return false
        }
        positionY = cluePosition.positionY
        if (render) {
            newLineAtOffset(0f, -clueTextSize)
        }
        positionY -= clueTextSize
        return showClueList(
            clues = crossword.downClues,
            header = "DOWN",
            columnWidth = columnWidth,
            columns = columns,
            clueTopY = clueTopY,
            gridY = gridY,
            gridHeight = gridHeight,
            clueTextSize = clueTextSize,
            cluePosition = CluePosition(
                positionY = positionY,
                column = cluePosition.column,
                columnBottomY = cluePosition.columnBottomY
            ),
            render = render
        ).first
    }

    private fun PdfDocument.showClueList(
        clues: Map<Int, String>,
        header: String,
        columnWidth: Float,
        columns: Int,
        clueTopY: Float,
        gridY: Float,
        gridHeight: Float,
        clueTextSize: Float,
        cluePosition: CluePosition,
        render: Boolean
    ): Pair<Boolean, CluePosition> {
        var positionY = cluePosition.positionY
        var columnBottomY = cluePosition.columnBottomY
        var column = cluePosition.column

        clues.entries.forEachIndexed { index, (clueNumber, clue) ->
            val clueHeaderSize = clueTextSize + 1.0f
            val prefix = "$clueNumber "
            val prefixWidth = getTextWidth(prefix, clueTextSize)

            // Count the number of lines needed for the entire clue, plus the section header if
            // this is the first clue in a section, as we do not want to split a clue apart or
            // show a section header at the end of a column.
            val lines = splitTextToLines(this, clue, clueTextSize, columnWidth - prefixWidth)
            val clueHeight = lines.size * clueTextSize +
                    if (index == 0) {
                        clueHeaderSize
                    } else {
                        0f
                    }

            if (positionY + clueTextSize - clueHeight < columnBottomY) {
                // This clue extends below the grid, so move to the next column.
                if (++column == columns) {
                    // Can't fit clues at this font size
                    return false to CluePosition(positionY = positionY, column = column, columnBottomY = columnBottomY)
                }
                if (render) {
                    newLineAtOffset(columnWidth + COLUMN_PADDING, clueTopY - positionY)
                }
                positionY = clueTopY
                columnBottomY = gridY + gridHeight + clueTextSize
            }

            if (index == 0) {
                if (render) {
                    setFont(Font.TIMES_BOLD, clueHeaderSize)
                    drawText(header)
                    newLineAtOffset(0f, -clueHeaderSize)

                    setFont(Font.TIMES_ROMAN, clueTextSize)
                }
                positionY -= clueHeaderSize
            }
            if (render) {
                drawText(prefix)
                newLineAtOffset(prefixWidth, 0f)
            }
            lines.forEach {
                if (render) {
                    drawText(it)
                    newLineAtOffset(0f, -clueTextSize)
                }
                positionY -= clueTextSize
            }
            if (render) {
                newLineAtOffset(-prefixWidth, 0f)
            }
        }
        return true to CluePosition(positionY = positionY, column = column, columnBottomY = columnBottomY)
    }
}