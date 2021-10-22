package com.jeffpdavidson.kotwords.formats

import com.github.ajalt.colormath.HSL
import com.github.ajalt.colormath.RGB
import com.jeffpdavidson.kotwords.model.Puzzle
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

    /** Space between the header text and the start of the clues. */
    private const val HEADER_CLUES_SPACING = 28f

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

    /** Maximum size of answer text. */
    private const val SOLUTION_TEXT_MAX_SIZE = 16f

    /** Minimum size of answer text. */
    private const val SOLUTION_TEXT_MIN_SIZE = 3f

    /** Amount to shrink the font size for each render attempt if text does not fit in the given bounds. */
    private const val TEXT_SIZE_DELTA = 0.1f

    /** Line spacing for text. */
    private const val LINE_SPACING = 1.15f

    /** Percentage of the current font size to use for sub/superscripts. */
    private const val SUB_SUPER_SCRIPT_FONT_SIZE_PERCENTAGE = 0.75f

    /** Percentage of the current font size to offset superscripts upwards. */
    private const val SUPER_SCRIPT_OFFSET_PERCENTAGE = 0.2f

    /** Percentage of the current font size to offset subscripts downwards. */
    private const val SUB_SCRIPT_OFFSET_PERCENTAGE = 0.2f

    /** Returns the number of columns to use for the clues. */
    private fun getClueColumns(clueCount: Int): Int = if (clueCount >= 50) {
        4
    } else {
        3
    }

    /** Returns the percentage of the content width to use for the grid. */
    private fun getGridWidthPercentage(clueCount: Int): Float =
        if (clueCount >= 50) {
            0.7f
        } else {
            0.6f
        }

    /** Result metrics from drawing the grid. */
    data class DrawGridResult(
        /** The maximum height of the rendered grid. */
        val gridHeight: Float,
        /** The start (x) offset of the bottom row of the rendered grid; used to align the copyright text. */
        val bottomRowStartOffset: Float,
    )

    /**
     * Render this puzzle as a PDF document.
     *
     * Inspired by [puz2pdf](https://sourceforge.net/projects/puz2pdf) and
     * [Crossword Nexus's PDF converter](https://crosswordnexus.com/js/puz_functions.js).
     *
     * @param fontFamily Font family to use for the PDF.
     * @param blackSquareLightnessAdjustment Percentage (from 0 to 1) indicating how much to brighten black/colored
     *                                       squares (i.e. to save ink). 0 indicates no adjustment; 1 would be fully
     *                                       white.
     * @param gridRenderer Optional function to render the grid, if custom rendering is desired. The default rendering
     *                     draws a rectangular grid with square cells. Custom functions should fill a maximum width of
     *                     gridWidth and return the resulting maximum height of the grid.
     */
    fun Puzzle.asPdf(
        fontFamily: PdfFontFamily = FONT_FAMILY_TIMES_ROMAN,
        blackSquareLightnessAdjustment: Float = 0f,
        gridRenderer: (
            document: PdfDocument,
            grid: List<List<Puzzle.Cell>>,
            blackSquareLightnessAdjustment: Float,
            gridWidth: Float,
            gridX: Float,
            gridY: Float,
            fontFamily: PdfFontFamily,
        ) -> DrawGridResult = ::drawGrid,
    ): ByteArray = PdfDocument().run {
        val pageWidth = width
        val pageHeight = height
        val headerWidth = pageWidth - 2 * MARGIN
        val clueCount = clues.sumOf { it.clues.size }
        // TODO: Need to cap width such that height doesn't exceed some reasonable threshold
        val gridWidth = getGridWidthPercentage(clueCount) * headerWidth
        val gridX = pageWidth - MARGIN - gridWidth
        val gridY = MARGIN + COPYRIGHT_SIZE
        val columns = getClueColumns(clueCount)
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

        // Header - title, creator, description.
        drawMultiLineText(title, fontFamily.boldFont, TITLE_SIZE, headerWidth, ::newLine)
        newLine(AUTHOR_SIZE * LINE_SPACING)
        drawMultiLineText(creator, fontFamily.baseFont, AUTHOR_SIZE, headerWidth, ::newLine)
        if (description.isNotBlank()) {
            newLine(NOTES_SIZE * LINE_SPACING)
            drawMultiLineText(description, fontFamily.italicFont, NOTES_SIZE, headerWidth, ::newLine)
        }
        endText()

        // Grid
        val drawGridResult = gridRenderer(
            this,
            grid,
            blackSquareLightnessAdjustment,
            gridWidth,
            gridX,
            gridY,
            fontFamily,
        )

        // Clues
        setFillColor(0f, 0f, 0f)
        beginText()
        positionY -= HEADER_CLUES_SPACING
        newLineAtOffset(titleX, positionY)

        // Try progressively smaller clue sizes until we find one small enough to fit every clue on one page.
        setFont(fontFamily.baseFont, CLUE_TEXT_MAX_SIZE)
        val bestTextSize = findBestFontSize(CLUE_TEXT_MIN_SIZE, CLUE_TEXT_MAX_SIZE) {
            showClueLists(
                puzzle = this@asPdf,
                fontFamily = fontFamily,
                columnWidth = columnWidth,
                columns = columns,
                clueTopY = positionY,
                gridY = gridY,
                gridHeight = drawGridResult.gridHeight,
                clueTextSize = it,
                render = false
            )
        }
        require(bestTextSize != null) {
            "Clues do not fit on a single page"
        }
        showClueLists(
            puzzle = this@asPdf,
            fontFamily = fontFamily,
            columnWidth = columnWidth,
            columns = columns,
            clueTopY = positionY,
            gridY = gridY,
            gridHeight = drawGridResult.gridHeight,
            clueTextSize = bestTextSize,
            render = true
        )
        endText()

        // Copyright
        beginText()
        newLineAtOffset(gridX + drawGridResult.bottomRowStartOffset, MARGIN)
        setFont(fontFamily.baseFont, COPYRIGHT_SIZE)
        drawText(copyright)
        endText()

        toByteArray()
    }

    /** Default grid drawing function for [asPdf]. */
    fun drawGrid(
        document: PdfDocument,
        grid: List<List<Puzzle.Cell>>,
        blackSquareLightnessAdjustment: Float,
        gridWidth: Float,
        gridX: Float,
        gridY: Float,
        fontFamily: PdfFontFamily,
    ): DrawGridResult = document.run {
        val gridRows = grid.size
        val gridCols = grid.maxOf { it.size }
        val gridSquareSize = gridWidth / gridCols
        val gridHeight = gridSquareSize * gridRows
        val gridNumberSize = gridSquareSize / 3

        val gridBlackColor = getAdjustedColor(RGB("#000000"), blackSquareLightnessAdjustment)

        grid.forEachIndexed { y, row ->
            row.forEachIndexed eachSquare@{ x, square ->
                val squareX = gridX + x * gridSquareSize
                val squareY = gridY + gridHeight - (y + 1) * gridSquareSize

                // Fill in the square background if a background color is specified, or if this is a black square.
                // Otherwise, use a white background.
                addRect(squareX, squareY, gridSquareSize, gridSquareSize)
                val backgroundColor = when {
                    square.backgroundColor.isNotBlank() ->
                        getAdjustedColor(RGB(square.backgroundColor), blackSquareLightnessAdjustment)
                    square.cellType == Puzzle.CellType.BLOCK -> gridBlackColor
                    else -> RGB("#ffffff")
                }
                setFillColor(backgroundColor.r / 255f, backgroundColor.g / 255f, backgroundColor.b / 255f)
                if (square.cellType == Puzzle.CellType.VOID) {
                    fill()
                } else {
                    setStrokeColor(gridBlackColor.r / 255f, gridBlackColor.g / 255f, gridBlackColor.b / 255f)
                    fillAndStroke()
                }

                if (!square.cellType.isBlack()) {
                    if (square.backgroundShape == Puzzle.BackgroundShape.CIRCLE) {
                        addCircle(squareX, squareY, gridSquareSize / 2)
                        stroke()
                    }

                    if (square.number.isNotBlank()) {
                        drawSquareNumber(
                            x = gridX + x * gridSquareSize + GRID_NUMBER_X_OFFSET,
                            y = gridY + gridHeight - y * gridSquareSize - gridNumberSize,
                            text = square.number,
                            textWidth = getTextWidth(square.number, fontFamily.baseFont, gridNumberSize),
                            gridNumberSize = gridNumberSize,
                            font = fontFamily.baseFont,
                        )
                    }
                    if (square.topRightNumber.isNotBlank()) {
                        val textWidth = getTextWidth(square.topRightNumber, fontFamily.baseFont, gridNumberSize)
                        drawSquareNumber(
                            x = gridX + (x + 1) * gridSquareSize - GRID_NUMBER_X_OFFSET - textWidth,
                            y = gridY + gridHeight - y * gridSquareSize - gridNumberSize,
                            text = square.topRightNumber,
                            textWidth = getTextWidth(square.number, fontFamily.baseFont, gridNumberSize),
                            gridNumberSize = gridNumberSize,
                            font = fontFamily.baseFont,
                        )
                    }
                    if (square.cellType == Puzzle.CellType.CLUE) {
                        // Render the square's solution.
                        // Truncate the solution if it's greater than eight characters, and split it into two lines if it's
                        // more than four characters.
                        var solutionString = square.solution
                        if (solutionString.length > 8) {
                            solutionString = solutionString.substring(0, 5) + "..."
                        }
                        val solutionLines = solutionString.chunked(4)
                        // Find the maximum font size for the solution that can fit the solution text into 80% of the
                        // square size.
                        val maxSize = 0.8 * gridSquareSize
                        val solutionTextSize =
                            findBestFontSize(SOLUTION_TEXT_MIN_SIZE, SOLUTION_TEXT_MAX_SIZE) { textSize ->
                                val maxWidth = solutionLines.maxOf { line ->
                                    getTextWidth(line, fontFamily.baseFont, textSize)
                                }
                                val height = solutionLines.size * textSize
                                maxWidth < maxSize && height < maxSize
                            }
                        require(solutionTextSize != null) {
                            "Solution text does not fit into square"
                        }
                        // Center align each line and draw the text.
                        setFillColor(0f, 0f, 0f)
                        val textHeight = solutionLines.size * solutionTextSize
                        solutionLines.forEachIndexed { i, line ->
                            beginText()
                            setFont(fontFamily.baseFont, solutionTextSize)
                            val lineWidth = getTextWidth(line, fontFamily.baseFont, solutionTextSize)
                            val solutionX = squareX + (gridSquareSize - lineWidth) / 2
                            val baseYOffset = (gridSquareSize - textHeight) / 2
                            val lineOffset = (solutionLines.size - i - 1) * solutionTextSize
                            val solutionY = squareY + baseYOffset + lineOffset
                            newLineAtOffset(solutionX, solutionY)
                            drawText(line)
                            endText()
                        }
                    }
                }
                if (square.borderDirections.isNotEmpty()) {
                    setLineWidth(3f)
                    square.borderDirections.forEach { borderDirection ->
                        val squareXEnd = squareX + gridSquareSize
                        val squareYEnd = squareY + gridSquareSize
                        when (borderDirection) {
                            Puzzle.BorderDirection.TOP -> addLine(squareX, squareYEnd, squareXEnd, squareYEnd)
                            Puzzle.BorderDirection.BOTTOM -> addLine(squareX, squareY, squareXEnd, squareY)
                            Puzzle.BorderDirection.LEFT -> addLine(squareX, squareY, squareX, squareYEnd)
                            Puzzle.BorderDirection.RIGHT -> addLine(squareXEnd, squareY, squareXEnd, squareYEnd)
                        }
                        stroke()
                    }
                    setLineWidth(1f)
                }
            }
        }
        return DrawGridResult(gridHeight = gridHeight, bottomRowStartOffset = 0f)
    }

    /** Return the adjusted color as a result of applying [lightnessAdjustment] to the given [rgb] color. */
    fun getAdjustedColor(rgb: RGB, lightnessAdjustment: Float): RGB {
        val hsl = rgb.toHSL()
        return HSL(hsl.h, hsl.s, (hsl.l + (100 - hsl.l) * lightnessAdjustment).roundToInt()).toRGB()
    }

    private fun PdfDocument.drawSquareNumber(
        x: Float,
        y: Float,
        text: String,
        textWidth: Float,
        gridNumberSize: Float,
        font: PdfFont
    ) {
        // Erase a rectangle around the number to make sure it stands out if there is a circle.
        addRect(
            x,
            y,
            textWidth,
            gridNumberSize - 2f
        )
        fill()

        setFillColor(0f, 0f, 0f)
        beginText()
        newLineAtOffset(x, y)
        setFont(font, gridNumberSize)
        drawText(text)
        endText()
    }

    private fun PdfDocument.drawMultiLineText(
        text: String, font: PdfFont, fontSize: Float, lineWidth: Float, newLineFn: (Float) -> Unit
    ) {
        setFont(font, fontSize)
        splitTextToLines(this, text, font, fontSize, lineWidth).forEachIndexed { i, line ->
            if (i > 0) {
                newLineFn(fontSize * LINE_SPACING)
            }
            drawText(line)
        }
    }

    internal enum class Script {
        REGULAR,
        SUBSCRIPT,
        SUPERSCRIPT;

        fun getScaledFontSize(fontSize: Float): Float {
            return when (this) {
                REGULAR -> fontSize
                else -> fontSize * SUB_SUPER_SCRIPT_FONT_SIZE_PERCENTAGE
            }
        }
    }

    internal data class Format(val font: PdfFont, val script: Script)

    internal sealed class ClueTextElement {
        data class Text(val text: String) : ClueTextElement()
        object NewLine : ClueTextElement()
        data class SetFormat(val format: Format) : ClueTextElement()
    }

    private data class FormattedChar(val char: Char, val format: Format)

    internal fun splitTextToLines(
        document: PdfDocument,
        rawText: String,
        fontFamily: PdfFontFamily,
        fontSize: Float,
        lineWidth: Float,
        isHtml: Boolean,
    ): List<ClueTextElement> {
        // Parse the HTML to create a list of character + font pairs.
        data class NodeState(
            val node: Node,
            val boldTagLevel: Int,
            val italicTagLevel: Int,
            val subTagLevel: Int,
            val supTagLevel: Int,
        )

        val nodeStack = ArrayDeque<NodeState>()
        if (isHtml) {
            val node = Xml.parse(rawText, format = DocumentFormat.HTML)
            nodeStack.add(
                NodeState(node, boldTagLevel = 0, italicTagLevel = 0, subTagLevel = 0, supTagLevel = 0)
            )
        } else {
            nodeStack.add(
                NodeState(TextNode(rawText), boldTagLevel = 0, italicTagLevel = 0, subTagLevel = 0, supTagLevel = 0)
            )
        }
        val formattedChars = mutableListOf<FormattedChar>()
        while (nodeStack.isNotEmpty()) {
            val nodeState = nodeStack.removeFirst()
            when (nodeState.node) {
                is Element -> {
                    nodeState.node.children.reversed().forEach { childNode ->
                        nodeStack.addFirst(
                            NodeState(
                                childNode,
                                boldTagLevel = nodeState.boldTagLevel + if (nodeState.node.tag == "B") 1 else 0,
                                italicTagLevel = nodeState.italicTagLevel + if (nodeState.node.tag == "I") 1 else 0,
                                subTagLevel = nodeState.subTagLevel + if (nodeState.node.tag == "SUB") 1 else 0,
                                supTagLevel = nodeState.supTagLevel + if (nodeState.node.tag == "SUP") 1 else 0,
                            )
                        )
                    }
                }
                is TextNode -> {
                    val currentFont = getFont(fontFamily, nodeState.boldTagLevel, nodeState.italicTagLevel)
                    val currentScript = getScript(nodeState.subTagLevel, nodeState.supTagLevel)
                    val text = nodeState.node.text.map { FormattedChar(it, Format(currentFont, currentScript)) }
                    formattedChars.addAll(text)
                }
            }
        }

        // Split the formatted text into lines, and convert into a stream of text, font changes, and new lines.
        val lines = splitTextToLines(document, formattedChars, fontFamily.baseFont, fontSize, lineWidth)
        val clueTextElements = mutableListOf<ClueTextElement>()
        val baseFormat = Format(fontFamily.baseFont, Script.REGULAR)
        var currentFormat = baseFormat
        lines.forEach { line ->
            forEachFormat(line) { text, format ->
                if (format != currentFormat) {
                    clueTextElements.add(ClueTextElement.SetFormat(format))
                    currentFormat = format
                }
                clueTextElements.add(ClueTextElement.Text(text))
            }
            clueTextElements.add(ClueTextElement.NewLine)
        }
        if (currentFormat != baseFormat) {
            clueTextElements.add(ClueTextElement.SetFormat(baseFormat))
        }
        return clueTextElements
    }

    private fun getFont(fontFamily: PdfFontFamily, boldTagLevel: Int, italicTagLevel: Int): PdfFont {
        return when {
            boldTagLevel > 0 && italicTagLevel > 0 -> fontFamily.boldItalicFont
            boldTagLevel > 0 -> fontFamily.boldFont
            italicTagLevel > 0 -> fontFamily.italicFont
            else -> fontFamily.baseFont
        }
    }

    private fun getScript(subTagLevel: Int, supTagLevel: Int): Script {
        return when {
            subTagLevel > 0 -> Script.SUBSCRIPT
            supTagLevel > 0 -> Script.SUPERSCRIPT
            else -> Script.REGULAR
        }
    }

    /** Run the given function on each word, using space as a separator. */
    private fun forEachWord(
        text: List<FormattedChar>,
        fn: (word: List<FormattedChar>, nextSeparator: FormattedChar?) -> Unit
    ) {
        val currentWord = mutableListOf<FormattedChar>()
        text.forEach { formattedChar ->
            if (formattedChar.char == ' ') {
                fn(currentWord, formattedChar)
                currentWord.clear()
            } else {
                currentWord.add(formattedChar)
            }
        }
        if (currentWord.isNotEmpty()) {
            fn(currentWord, null)
        }
    }

    /** Run the given function for each chunk of the given string which has the same format. */
    private fun forEachFormat(text: List<FormattedChar>, fn: (text: String, format: Format) -> Unit) {
        val currentString = StringBuilder()
        var currentFormat: Format? = null
        text.forEach { formattedChar ->
            if (currentFormat != null && currentFormat != formattedChar.format) {
                fn(currentString.toString(), currentFormat!!)
                currentString.clear()
            }
            currentString.append(formattedChar.char)
            currentFormat = formattedChar.format
        }
        if (currentString.isNotEmpty()) {
            fn(currentString.toString(), currentFormat!!)
        }
    }

    /** Split [text] into lines (using spaces as word separators) to fit the given [lineWidth]. */
    internal fun splitTextToLines(
        document: PdfDocument,
        text: String,
        font: PdfFont,
        fontSize: Float,
        lineWidth: Float,
    ): List<String> {
        val formattedText = text.map { FormattedChar(it, Format(font, Script.REGULAR)) }
        return splitTextToLines(document, formattedText, font, fontSize, lineWidth).map { line ->
            line.map { it.char }.toCharArray().concatToString()
        }
    }

    /** Split formatted [text] into lines (using spaces as word separators) to fit the given [lineWidth]. */
    private fun splitTextToLines(
        document: PdfDocument,
        text: List<FormattedChar>,
        baseFont: PdfFont,
        fontSize: Float,
        lineWidth: Float,
    ): List<List<FormattedChar>> {
        val lines = mutableListOf<List<FormattedChar>>()
        var currentLine = mutableListOf<FormattedChar>()
        lines += currentLine
        var currentLineLength = 0f
        var currentSeparator = ""
        var currentSeparatorFormat = Format(baseFont, Script.REGULAR)
        forEachWord(text) { formattedWord, nextSeparator ->
            val separatorLength = document.getTextWidth(currentSeparator, currentSeparatorFormat, fontSize)
            var wordLength = 0f
            forEachFormat(formattedWord) { word, format ->
                wordLength += document.getTextWidth(word, format, fontSize)
            }
            if (currentLineLength + separatorLength + wordLength > lineWidth) {
                // This word pushes us over the line length limit, so we'll need a new line.
                if (wordLength > lineWidth) {
                    // Word is too long to fit on a single line; have to chop by letter.
                    formattedWord.forEach { formattedChar ->
                        val charLength =
                            document.getTextWidth(formattedChar.char.toString(), formattedChar.format, fontSize)
                        val wordSeparatorLengthPts =
                            document.getTextWidth(currentSeparator, currentSeparatorFormat, fontSize)
                        if (currentLineLength + wordSeparatorLengthPts + charLength > lineWidth) {
                            currentLine = mutableListOf(formattedChar)
                            lines += currentLine
                            currentLineLength = charLength
                        } else {
                            currentLine.addAll(currentSeparator.map { FormattedChar(it, currentSeparatorFormat) })
                            currentLine.add(formattedChar)
                            currentLineLength += wordSeparatorLengthPts + charLength
                        }
                        currentSeparator = ""
                    }
                } else {
                    // Start a new line with this word.
                    currentLine = formattedWord.toMutableList()
                    lines += currentLine
                    currentLineLength = wordLength
                }
            } else {
                // This word fits, so continue the current line with it.
                currentLine.addAll(currentSeparator.map { FormattedChar(it, currentSeparatorFormat) })
                currentLine.addAll(formattedWord)
                currentLineLength += separatorLength + wordLength
            }
            currentSeparator = nextSeparator?.char?.toString() ?: ""
            currentSeparatorFormat = nextSeparator?.format ?: currentSeparatorFormat
        }
        return lines
    }

    private data class CluePosition(
        val positionY: Float,
        val column: Int,
        val columnBottomY: Float,
    )

    private fun PdfDocument.showClueLists(
        puzzle: Puzzle,
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
        var column = 0
        var columnBottomY = gridY
        if (render) {
            setFont(fontFamily.baseFont, clueTextSize)
        }
        var lastSuccess = true
        puzzle.clues.forEachIndexed { i, clueList ->
            val (success, cluePosition) =
                showClueList(
                    puzzle = puzzle,
                    clues = clueList,
                    isHtml = puzzle.hasHtmlClues,
                    fontFamily = fontFamily,
                    columnWidth = columnWidth,
                    columns = columns,
                    clueTopY = clueTopY,
                    gridY = gridY,
                    gridHeight = gridHeight,
                    clueTextSize = clueTextSize,
                    cluePosition = CluePosition(positionY = positionY, column = column, columnBottomY = columnBottomY),
                    render = render
                )

            if (!success) {
                return false
            }

            positionY = cluePosition.positionY
            column = cluePosition.column
            columnBottomY = cluePosition.columnBottomY

            if (i != puzzle.clues.lastIndex) {
                if (render) {
                    newLineAtOffset(0f, -clueTextSize)
                }
                positionY -= clueTextSize
            }

            lastSuccess = success
        }
        return lastSuccess
    }

    private fun PdfDocument.showClueList(
        puzzle: Puzzle,
        clues: Puzzle.ClueList,
        isHtml: Boolean,
        fontFamily: PdfFontFamily,
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

        val maxPrefixWidth = puzzle.clues.flatMap { it.clues }.maxOf {
            getTextWidth("${it.number.ifBlank { "•" }} ", fontFamily.baseFont, clueTextSize)
        }

        val clueHeaderSize = clueTextSize + 1.0f
        val title = (if (isHtml) clues.title else "<b>${clues.title}</b>").uppercase()
        val titleElements =
            splitTextToLines(this, title, fontFamily, clueHeaderSize, columnWidth - maxPrefixWidth, isHtml = true)
        val titleLineCount = titleElements.count { it == ClueTextElement.NewLine }

        clues.clues.forEachIndexed { index, clue ->
            // Count the number of lines needed for the entire clue, plus the section header if
            // this is the first clue in a section, as we do not want to split a clue apart or
            // show a section header at the end of a column.
            val clueElements =
                splitTextToLines(this, clue.text, fontFamily, clueTextSize, columnWidth - maxPrefixWidth, isHtml)
            val lineCount = clueElements.count { it == ClueTextElement.NewLine }
            val clueHeight = clueTextSize * (1 + LINE_SPACING * (lineCount - 1)) +
                    if (index == 0) {
                        clueHeaderSize * (1 + LINE_SPACING * (titleLineCount - 1)) + (LINE_SPACING - 1) * clueTextSize
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

            fun drawClueElements(clueElements: List<ClueTextElement>, textSize: Float, nextTextSize: Float = textSize) {
                var currentFont = fontFamily.baseFont
                var currentFontSize = textSize
                var currentScript = Script.REGULAR
                var currentLinePosition = 0f
                var lastLineXOffset = 0f
                var lastLineYOffset = 0f

                val lastNewLineIndex = clueElements.lastIndexOf(ClueTextElement.NewLine)

                clueElements.forEachIndexed { i, clueElement ->
                    when (clueElement) {
                        is ClueTextElement.Text -> {
                            if (render) {
                                drawText(clueElement.text)
                                currentLinePosition += getTextWidth(clueElement.text, currentFont, currentFontSize)
                            }
                        }
                        is ClueTextElement.NewLine -> {
                            val offset = (if (i == lastNewLineIndex) nextTextSize else textSize) * LINE_SPACING
                            if (render) {
                                newLineAtOffset(-lastLineXOffset, -offset)
                                currentLinePosition = 0f
                                lastLineXOffset = 0f
                            }
                            positionY -= offset
                        }
                        is ClueTextElement.SetFormat -> {
                            if (render) {
                                val newFont = clueElement.format.font
                                val newFontSize = clueElement.format.script.getScaledFontSize(textSize)
                                if (newFont != currentFont || newFontSize != currentFontSize) {
                                    setFont(newFont, newFontSize)
                                    currentFont = newFont
                                    currentFontSize = newFontSize
                                }
                                if (currentScript != clueElement.format.script) {
                                    val yOffset =
                                        when (clueElement.format.script) {
                                            Script.SUPERSCRIPT -> clueTextSize * SUPER_SCRIPT_OFFSET_PERCENTAGE
                                            Script.SUBSCRIPT -> -clueTextSize * SUB_SCRIPT_OFFSET_PERCENTAGE
                                            Script.REGULAR -> 0f
                                        }
                                    newLineAtOffset(currentLinePosition - lastLineXOffset, yOffset - lastLineYOffset)
                                    lastLineXOffset = currentLinePosition
                                    lastLineYOffset = yOffset
                                    currentScript = clueElement.format.script
                                }
                            }
                        }
                    }
                }
            }

            if (index == 0) {
                setFont(fontFamily.baseFont, clueHeaderSize)
                newLineAtOffset(maxPrefixWidth, 0f)
                drawClueElements(titleElements, textSize = clueHeaderSize, nextTextSize = clueTextSize)
                newLineAtOffset(-maxPrefixWidth, 0f)

                setFont(fontFamily.baseFont, clueTextSize)
            }

            if (render) {
                val prefix = "${clue.number.ifBlank { "•" }} "
                val prefixWidth = getTextWidth(prefix, fontFamily.baseFont, clueTextSize)
                newLineAtOffset(maxPrefixWidth - prefixWidth, 0f)
                drawText(prefix)
                newLineAtOffset(prefixWidth, 0f)
            }

            drawClueElements(clueElements, textSize = clueTextSize)

            if (render) {
                newLineAtOffset(-maxPrefixWidth, 0f)
            }
        }
        return true to CluePosition(positionY = positionY, column = column, columnBottomY = columnBottomY)
    }

    private fun findBestFontSize(minSize: Float, maxSize: Float, testFn: (Float) -> Boolean): Float? {
        val textSizes = generateSequence(maxSize) { it - TEXT_SIZE_DELTA }.takeWhile { it >= minSize }.toList()
        val insertionIndex = -textSizes.binarySearch { size -> if (testFn(size)) 1 else -1 } - 1
        return if (insertionIndex > textSizes.lastIndex) null else textSizes[insertionIndex]
    }

    private fun PdfDocument.getTextWidth(text: String, format: Format, fontSize: Float): Float {
        return getTextWidth(text, format.font, format.script.getScaledFontSize(fontSize))
    }
}