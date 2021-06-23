package com.jeffpdavidson.kotwords.formats

import com.github.ajalt.colormath.HSL
import com.github.ajalt.colormath.RGB
import com.jeffpdavidson.kotwords.model.Crossword
import com.jeffpdavidson.kotwords.model.Square
import kotlin.math.roundToInt

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
     *
     * @param fontFamily Font family to use for the PDF.
     * @param blackSquareLightnessAdjustment Percentage (from 0 to 1) indicating how much to brighten black/colored
     *                                       squares (i.e. to save ink). 0 indicates no adjustment; 1 would be fully
     *                                       white.
     */
    fun Crossword.asPdf(
        fontFamily: PdfFontFamily = FONT_FAMILY_TIMES_ROMAN,
        blackSquareLightnessAdjustment: Float = 0f
    ): ByteArray = PdfDocument().run {
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

        drawMultiLineText(title, fontFamily.boldFont, TITLE_SIZE, headerWidth, ::newLine)
        drawMultiLineText(author, fontFamily.baseFont, AUTHOR_SIZE, headerWidth, ::newLine)
        if (notes.isNotBlank()) {
            drawMultiLineText(notes, fontFamily.italicFont, NOTES_SIZE, headerWidth, ::newLine)
        }

        newLine(AUTHOR_SIZE)

        // Try progressively smaller clue sizes until we find one small enough to fit every clue on one page.
        setFont(fontFamily.baseFont, CLUE_TEXT_MAX_SIZE)
        val clueTextSizes =
            generateSequence(CLUE_TEXT_MAX_SIZE) { it - CLUE_TEXT_SIZE_DELTA }.takeWhile { it >= CLUE_TEXT_MIN_SIZE }
        val bestTextSize = clueTextSizes.firstOrNull { clueTextSize ->
            showClueLists(
                crossword = this@asPdf,
                fontFamily = fontFamily,
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
            fontFamily = fontFamily,
            columnWidth = columnWidth,
            columns = columns,
            clueTopY = positionY,
            gridY = gridY,
            gridHeight = gridHeight,
            clueTextSize = bestTextSize,
            render = true
        )

        endText()

        val gridBlackColor = getAdjustedColor("#000000", blackSquareLightnessAdjustment)
        setStrokeColor(gridBlackColor.r / 255f, gridBlackColor.g / 255f, gridBlackColor.b / 255f)
        setFillColor(gridBlackColor.r / 255f, gridBlackColor.g / 255f, gridBlackColor.b / 255f)
        Crossword.forEachSquare(grid) { x, y, clueNumber, _, _, square ->
            val squareX = gridX + x * gridSquareSize
            val squareY = gridY + gridHeight - (y + 1) * gridSquareSize
            addRect(squareX, squareY, gridSquareSize, gridSquareSize)
            if (square.isBlack) {
                if (square.backgroundColor?.isNotBlank() == true) {
                    val backgroundColor = getAdjustedColor(square.backgroundColor, blackSquareLightnessAdjustment)
                    setFillColor(backgroundColor.r / 255f, backgroundColor.g / 255f, backgroundColor.b / 255f)
                } else {
                    setFillColor(gridBlackColor.r / 255f, gridBlackColor.g / 255f, gridBlackColor.b / 255f)
                }
                fillAndStroke()
            } else {
                stroke()
                if (square.isCircled) {
                    addCircle(squareX, squareY, gridSquareSize / 2)
                    stroke()
                }
                // If custom words are provided, use only provided numbering. Otherwise, use generated numbering.
                val number = if (acrossWords.isNotEmpty() && downWords.isNotEmpty()) square.number else clueNumber
                if (number != null) {
                    setFillColor(0f, 0f, 0f)
                    beginText()
                    newLineAtOffset(
                        gridX + x * gridSquareSize + GRID_NUMBER_X_OFFSET,
                        gridY + gridHeight - y * gridSquareSize - gridNumberSize
                    )
                    setFont(fontFamily.baseFont, gridNumberSize)
                    drawText(number.toString())
                    endText()
                }
            }
            if (square.borderDirections.isNotEmpty()) {
                setLineWidth(3f)
                square.borderDirections.forEach { borderDirection ->
                    val squareXEnd = squareX + gridSquareSize
                    val squareYEnd = squareY + gridSquareSize
                    when (borderDirection) {
                        Square.BorderDirection.TOP -> addLine(squareX, squareYEnd, squareXEnd, squareYEnd)
                        Square.BorderDirection.BOTTOM -> addLine(squareX, squareY, squareXEnd, squareY)
                        Square.BorderDirection.LEFT -> addLine(squareX, squareY, squareX, squareYEnd)
                        Square.BorderDirection.RIGHT -> addLine(squareXEnd, squareY, squareXEnd, squareYEnd)
                    }
                    stroke()
                }
                setLineWidth(1f)
            }
        }

        setFillColor(0f, 0f, 0f)

        beginText()
        newLineAtOffset(gridX, MARGIN)
        setFont(fontFamily.baseFont, COPYRIGHT_SIZE)
        drawText(copyright)
        endText()

        toByteArray()
    }

    private fun getAdjustedColor(rgbString: String, lightnessAdjustment: Float): RGB {
        val hsl = RGB(rgbString).toHSL()
        return HSL(hsl.h, hsl.s, (hsl.l + (100 - hsl.l) * lightnessAdjustment).roundToInt()).toRGB()
    }

    private fun PdfDocument.drawMultiLineText(
        text: String, font: PdfFont, fontSize: Float, lineWidth: Float, newLineFn: (Float) -> Unit
    ) {
        setFont(font, fontSize)
        splitTextToLines(this, text, font, fontSize, lineWidth).lines.forEach { line ->
            drawText(line)
            newLineFn(fontSize)
        }
    }

    internal sealed class ClueTextElement {
        data class Text(val text: String) : ClueTextElement()
        object NewLine : ClueTextElement()
        data class SetFont(val font: PdfFont) : ClueTextElement()
    }

    internal data class NodeState(
        val node: Node,
        val boldTagLevel: Int,
        val italicTagLevel: Int,
    )

    private fun getFont(fontFamily: PdfFontFamily, boldTagLevel: Int, italicTagLevel: Int): PdfFont {
        return when {
            boldTagLevel > 0 && italicTagLevel > 0 -> fontFamily.boldItalicFont
            boldTagLevel > 0 -> fontFamily.boldFont
            italicTagLevel > 0 -> fontFamily.italicFont
            else -> fontFamily.baseFont
        }
    }

    // TODO: Handle sub/sup
    internal fun splitTextToLines(
        document: PdfDocument,
        rawText: String,
        fontFamily: PdfFontFamily,
        fontSize: Float,
        lineWidth: Float,
        isHtml: Boolean,
    ): List<ClueTextElement> {
        val elements = mutableListOf<ClueTextElement>()
        var boldTagLevel = 0
        var italicTagLevel = 0
        var currentLineLength = 0f
        val nodeStack = ArrayDeque<NodeState>()
        if (isHtml) {
            val node = Xml.parse(rawText, format = DocumentFormat.HTML)
            nodeStack.add(NodeState(node, boldTagLevel = 0, italicTagLevel = 0))
        } else {
            nodeStack.add(NodeState(TextNode(rawText), boldTagLevel = 0, italicTagLevel = 0))
        }
        while (nodeStack.isNotEmpty()) {
            val nodeState = nodeStack.removeFirst()

            // Handle any font changes from the previous state to the new one.
            val addBold = boldTagLevel == 0 && nodeState.boldTagLevel > 0
            val removeBold = boldTagLevel > 0 && nodeState.boldTagLevel == 0
            val addItalic = italicTagLevel == 0 && nodeState.italicTagLevel > 0
            val removeItalic = italicTagLevel > 0 && nodeState.italicTagLevel == 0
            if (addBold || removeBold || addItalic || removeItalic) {
                val newFont = getFont(fontFamily, nodeState.boldTagLevel, nodeState.italicTagLevel)
                elements.add(ClueTextElement.SetFont(newFont))
            }

            boldTagLevel = nodeState.boldTagLevel
            italicTagLevel = nodeState.italicTagLevel

            when (nodeState.node) {
                is Element -> {
                    nodeState.node.children.reversed().forEach { childNode ->
                        nodeStack.addFirst(
                            NodeState(
                                childNode,
                                boldTagLevel = if (nodeState.node.tag == "B") boldTagLevel + 1 else boldTagLevel,
                                italicTagLevel = if (nodeState.node.tag == "I") italicTagLevel + 1 else italicTagLevel
                            )
                        )
                    }
                }
                is TextNode -> {
                    val splitResult = splitTextToLines(
                        document = document,
                        text = nodeState.node.text,
                        font = getFont(fontFamily, boldTagLevel, italicTagLevel),
                        fontSize = fontSize,
                        lineWidth = lineWidth,
                        startingLineLength = currentLineLength
                    )
                    currentLineLength = splitResult.currentLineLength
                    splitResult.lines.forEachIndexed { i, line ->
                        if (i > 0) {
                            elements.add(ClueTextElement.NewLine)
                        }
                        if (line.isNotBlank()) {
                            elements.add(ClueTextElement.Text(line))
                        }
                    }
                }
            }
        }
        if (boldTagLevel > 0 || italicTagLevel > 0) {
            elements.add(ClueTextElement.SetFont(fontFamily.baseFont))
        }
        return elements
    }

    internal data class SplitTextToLinesResult(
        val lines: List<String>,
        val currentLineLength: Float,
    )

    /** Split [text] into lines (using spaces as word separators) to fit the given [lineWidth]. */
    internal fun splitTextToLines(
        document: PdfDocument,
        text: String,
        font: PdfFont,
        fontSize: Float,
        lineWidth: Float,
        startingLineLength: Float = 0f
    ): SplitTextToLinesResult {
        val lines = mutableListOf<StringBuilder>()
        var currentLine = StringBuilder()
        lines += currentLine
        var currentLineLength = startingLineLength
        var currentSeparator = ""
        text.split(" ").forEach { word ->
            val separatorLength = document.getTextWidth(currentSeparator, font, fontSize)
            val wordLength = document.getTextWidth(word, font, fontSize)
            if (currentLineLength + separatorLength + wordLength > lineWidth) {
                // This word pushes us over the line length limit, so we'll need a new line.
                if (wordLength > lineWidth) {
                    // Word is too long to fit on a single line; have to chop by letter.
                    word.forEach { ch ->
                        val charLength = document.getTextWidth(ch.toString(), font, fontSize)
                        val wordSeparatorLengthPts = document.getTextWidth(currentSeparator, font, fontSize)
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
        return SplitTextToLinesResult(lines.map { it.toString() }, currentLineLength)
    }

    private data class CluePosition(
        val positionY: Float,
        val column: Int,
        val columnBottomY: Float,
    )

    private fun PdfDocument.showClueLists(
        crossword: Crossword,
        fontFamily: PdfFontFamily,
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
            setFont(fontFamily.baseFont, clueTextSize)
        }
        val (success, cluePosition) =
            showClueList(
                clues = crossword.acrossClues,
                isHtml = crossword.hasHtmlClues,
                fontFamily = fontFamily,
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
            isHtml = crossword.hasHtmlClues,
            fontFamily = fontFamily,
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
        isHtml: Boolean,
        fontFamily: PdfFontFamily,
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
            val prefixWidth = getTextWidth(prefix, fontFamily.baseFont, clueTextSize)

            // Count the number of lines needed for the entire clue, plus the section header if
            // this is the first clue in a section, as we do not want to split a clue apart or
            // show a section header at the end of a column.
            val clueElements =
                splitTextToLines(this, clue, fontFamily, clueTextSize, columnWidth - prefixWidth, isHtml)
            val clueHeight = (clueElements.count { it == ClueTextElement.NewLine } + 1) * clueTextSize +
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
                    setFont(fontFamily.boldFont, clueHeaderSize)
                    drawText(header)
                    newLineAtOffset(0f, -clueHeaderSize)

                    setFont(fontFamily.baseFont, clueTextSize)
                }
                positionY -= clueHeaderSize
            }
            if (render) {
                drawText(prefix)
                newLineAtOffset(prefixWidth, 0f)
            }
            clueElements.forEach { clueElement ->
                when (clueElement) {
                    is ClueTextElement.Text -> {
                        if (render) {
                            drawText(clueElement.text)
                        }
                    }
                    is ClueTextElement.NewLine -> {
                        if (render) {
                            newLineAtOffset(0f, -clueTextSize)
                        }
                        positionY -= clueTextSize
                    }
                    is ClueTextElement.SetFont -> {
                        if (render) {
                            setFont(clueElement.font, clueTextSize)
                        }
                    }
                }
            }
            if (render) {
                newLineAtOffset(-prefixWidth, -clueTextSize)
            }
            positionY -= clueTextSize
        }
        return true to CluePosition(positionY = positionY, column = column, columnBottomY = columnBottomY)
    }
}