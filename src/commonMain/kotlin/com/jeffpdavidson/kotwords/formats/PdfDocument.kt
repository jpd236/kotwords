package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.model.Puzzle

/**
 * A document canvas which can be rendered as a PDF.
 *
 * Implementations should assume one-page, letter-sized documents. All units are in points. Coordinates are measured as
 * distance from the bottom-left corner of the document.
 */
expect class PdfDocument private constructor() {
    val width: Float
    val height: Float

    /** Begin a text section from the bottom-left of the document. */
    fun beginText()

    /** End the current text section.*/
    fun endText()

    /** Start a new line offset by ([offsetX], [offsetY]) from the current line. */
    fun newLineAtOffset(offsetX: Float, offsetY: Float)

    /** Set the font to be used for text. */
    suspend fun setFont(font: PdfFont, size: Float)

    /** Get the width of the given [text] with font [font] and font size [size]. */
    suspend fun getTextWidth(text: String, font: PdfFont, size: Float): Float

    /** Draw and stroke the given [text]. */
    fun drawText(text: String)

    /** Set the line width. */
    fun setLineWidth(width: Float)

    /** Set the stroke color. */
    fun setStrokeColor(r: Float, g: Float, b: Float)

    /** Set the fill color. */
    fun setFillColor(r: Float, g: Float, b: Float)

    /** Draw a line from ([x1], [y1]) to ([x2], [y2]). */
    fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float)

    /** Draw a rectangle from bottom-left coordinates ([x], [y]). */
    fun drawRect(x: Float, y: Float, width: Float, height: Float, stroke: Boolean = false, fill: Boolean = false)

    /** Draw a circle of radius [radius] from bottom-left coordinates ([x], [y]). */
    fun drawCircle(x: Float, y: Float, radius: Float, stroke: Boolean = false, fill: Boolean = false)

    /** Draw the given image from bottom-left coordinates ([x], [y]). */
    suspend fun drawImage(x: Float, y: Float, width: Float, height: Float, image: Puzzle.Image.Data)

    /** Return this document as a PDF [ByteArray]. */
    suspend fun toByteArray(): ByteArray

    companion object {
        suspend fun create(): PdfDocument
    }
}