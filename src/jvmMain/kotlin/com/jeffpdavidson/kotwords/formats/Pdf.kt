package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.model.Crossword
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.font.PDType1Font
import java.io.ByteArrayOutputStream

/** Extension functions to render crosswords as PDFs. */
object Pdf {
    // Constants/functions dictating the PDF style.

    /** Top/bottom and left/right margin size. */
    private const val MARGIN = 36f
    /** Size of the puzzle title. */
    private const val TITLE_SIZE = 16f
    /** Size of the puzzle author. */
    private const val AUTHOR_SIZE = 14f
    /** Size of the puzzle copyright. */
    private const val COPYRIGHT_SIZE = 9f
    /** Size of clue section headers ("ACROSS" and "DOWN"). */
    private const val CLUE_HEADER_SIZE = 12f
    /** Size of the space between adjacent clue columns. */
    private const val COLUMN_PADDING = 12f
    /** X offset of clue numbers in grid squares. (The Y offset is based on font size). */
    private const val GRID_NUMBER_X_OFFSET = 2f
    /** Color to use for "black" squares from 0 (black) to 1 (white). */
    private const val GRID_BLACK_COLOR = 0.75f

    /** Returns the number of columns to use for the clues. */
    private fun getClueColumns(gridRows: Int): Int = if (gridRows >= 15) { 4 } else { 3 }
    /** Returns the size of the grid number text. */
    private fun getGridNumberSize(gridRows: Int): Float = if (gridRows <= 17) { 8f } else { 6f }
    /** Returns the size of the clue text. */
    private fun getClueTextSize(gridRows: Int): Float = if (gridRows <= 17) { 11f } else { 8.5f }
    /** Returns the percentage of the content width to use for the grid. */
    private fun getGridWidthPercentage(gridRows: Int): Float =
            if (gridRows >= 15) { 0.7f } else { 0.6f }

    /**
     * Render this crossword as a PDF document.
     *
     * Inspired by [puz2pdf](https://sourceforge.net/projects/puz2pdf) and
     * [Crossword Nexus's PDF converter](https://crosswordnexus.com/js/puz_functions.js).
     */
    fun Crossword.asPdf(): ByteArray {
        // Speed up rendering on Java 8.
        System.setProperty("sun.java2d.cmm", "sun.java2d.cmm.kcms.KcmsServiceProvider")

        PDDocument().use { doc ->
            val page = PDPage()
            doc.addPage(page)

            PDPageContentStream(doc, page).use { content ->
                content.showCrossword(page, this)
            }

            ByteArrayOutputStream().use { stream ->
                doc.save(stream)
                return stream.toByteArray()
            }
        }
    }

    // Note: PDFs use a coordinate system where (0, 0) is in the bottom left.
    private fun PDPageContentStream.showCrossword(page: PDPage, crossword: Crossword) {
        val pageWidth = page.mediaBox.width
        val pageHeight = page.mediaBox.height
        val gridRows = crossword.grid.size
        val gridCols = crossword.grid[0].size
        val gridWidth = getGridWidthPercentage(gridRows) * (pageWidth - 2 * MARGIN)
        val gridHeight = gridWidth * gridRows / gridCols
        val gridSquareSize = gridHeight / gridRows
        val gridNumberSize = getGridNumberSize(gridRows)
        val gridX = pageWidth - MARGIN - gridWidth
        val gridY = MARGIN + COPYRIGHT_SIZE
        val clueSize = getClueTextSize(gridRows)
        val columns = getClueColumns(gridRows)
        val columnWidth = (pageWidth - 2 * MARGIN - (columns - 1) * COLUMN_PADDING) / columns
        val titleX = MARGIN
        val titleY = pageHeight - MARGIN

        var positionY = titleY
        fun newLine(offsetY: Float) {
            newLineAtOffset(0f, -offsetY)
            positionY -= offsetY
        }

        beginText()
        newLineAtOffset(titleX, titleY)

        setFont(PDType1Font.TIMES_BOLD, TITLE_SIZE)
        showText(crossword.title)
        newLine(TITLE_SIZE)

        setFont(PDType1Font.TIMES_ROMAN, AUTHOR_SIZE)
        showText(crossword.author)
        newLine(2 * AUTHOR_SIZE)

        val clueTopY = positionY
        // For the first column, the clues descend to the bottom of the grid.
        var clueBottomY = gridY
        var column = 0

        fun showClueList(clues: Map<Int, String>, header: String) {
            clues.entries.forEachIndexed { index, (clueNumber, clue) ->
                val prefix = "$clueNumber "
                val prefixWidth = prefix.getWidth(PDType1Font.TIMES_ROMAN, clueSize)

                // Count the number of lines needed for the entire clue, plus the section header if
                // this is the first clue in a section, as we do not want to split a clue apart or
                // show a section header at the end of a column.
                val lines = splitTextToLines(
                        clue, PDType1Font.TIMES_ROMAN, clueSize, columnWidth - prefixWidth)
                val clueHeight = lines.size * clueSize +
                        if (index == 0) { CLUE_HEADER_SIZE } else { 0f }

                if (positionY + clueSize - clueHeight < clueBottomY) {
                    // This clue extends below the grid, so move to the next column.
                    if (++column == columns) {
                        throw UnsupportedOperationException("Clues do not fit on a single page")
                    }
                    newLineAtOffset(columnWidth + COLUMN_PADDING, clueTopY - positionY)
                    positionY = clueTopY
                    clueBottomY = gridY + gridHeight + clueSize
                }

                if (index == 0) {
                    setFont(PDType1Font.TIMES_BOLD, CLUE_HEADER_SIZE)
                    showText(header)
                    newLine(CLUE_HEADER_SIZE)

                    setFont(PDType1Font.TIMES_ROMAN, clueSize)
                }
                showText(prefix)
                newLineAtOffset(prefixWidth, 0f)
                lines.forEach {
                    showText(it)
                    newLine(clueSize)
                }
                newLineAtOffset(-prefixWidth, 0f)
            }
        }

        showClueList(crossword.acrossClues, "ACROSS")
        newLine(clueSize)
        showClueList(crossword.downClues, "DOWN")

        endText()

        setStrokingColor(GRID_BLACK_COLOR)
        setNonStrokingColor(GRID_BLACK_COLOR)
        Crossword.forEachSquare(crossword.grid) { x, y, clueNumber, _, _, square ->
            val squareX = gridX + x * gridSquareSize
            val squareY = gridY + gridHeight - (y + 1) * gridSquareSize
            addRect(squareX, squareY, gridSquareSize, gridSquareSize)
            if (square.isBlack) {
                fillAndStroke()
            } else {
                stroke()
                if (square.isCircled) {
                    showCircle(gridSquareSize / 2, squareX, squareY)
                }
                if (clueNumber != null) {
                    setStrokingColor(0f)
                    setNonStrokingColor(0f)
                    beginText()
                    newLineAtOffset(
                            gridX + x * gridSquareSize + GRID_NUMBER_X_OFFSET,
                            gridY + gridHeight - y * gridSquareSize - gridNumberSize)
                    setFont(PDType1Font.TIMES_ROMAN, gridNumberSize)
                    showText(clueNumber.toString())
                    endText()
                    setStrokingColor(GRID_BLACK_COLOR)
                    setNonStrokingColor(GRID_BLACK_COLOR)
                }
            }
        }

        setStrokingColor(0f)
        setNonStrokingColor(0f)

        beginText()
        newLineAtOffset(gridX, MARGIN)
        setFont(PDType1Font.TIMES_ROMAN, COPYRIGHT_SIZE)
        showText(crossword.copyright)
        endText()
    }

    /** Split [text] into lines (using spaces as word separators) to fit the given [lineWidth]. */
    internal fun splitTextToLines(
            text: String, font: PDFont, fontSize: Float, lineWidth: Float): List<String> {
        val lines = mutableListOf<StringBuilder>()
        var currentLine = StringBuilder()
        lines += currentLine
        var currentLineLength = 0f
        var currentSeparator = ""
        text.split(" ").forEach { word ->
            val separatorLength = currentSeparator.getWidth(font, fontSize)
            val wordLength = word.getWidth(font, fontSize)
            if (currentLineLength + separatorLength + wordLength > lineWidth) {
                // This word pushes us over the line length limit, so we'll need a new line.
                if (wordLength > lineWidth) {
                    // Word is too long to fit on a single line; have to chop by letter.
                    word.forEach { ch ->
                        val charLength = ch.toString().getWidth(font, fontSize)
                        val wordSeparatorLengthPts = currentSeparator.getWidth(font, fontSize)
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

    /** Show a circle of radius [r] from bottom-left coordinates ([x], [y]). */
    private fun PDPageContentStream.showCircle(r: Float, x: Float, y: Float) {
        val k = 0.552284749831f
        val cx = x + r
        val cy = y + r
        moveTo(cx - r, cy)
        curveTo(cx - r, cy + k * r, cx - k * r, cy + r, cx, cy + r)
        curveTo(cx + k * r, cy + r, cx + r, cy + k * r, cx + r, cy)
        curveTo(cx + r, cy - k * r, cx + k * r, cy - r, cx, cy - r)
        curveTo(cx - k * r, cy - r, cx - r, cy - k * r, cx - r, cy)
        stroke()
    }

    private fun String.getWidth(font: PDFont, size: Float): Float =
            font.getStringWidth(this) * size / 1000
}