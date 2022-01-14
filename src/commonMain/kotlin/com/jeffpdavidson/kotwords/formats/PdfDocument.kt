package com.jeffpdavidson.kotwords.formats

/**
 * A document canvas which can be rendered as a PDF.
 *
 * Implementations should assume one-page, letter-sized documents. All units are in points. Coordinates are measured as
 * distance from the bottom-left corner of the document.
 */
expect class PdfDocument() {
    val width: Float
    val height: Float

    /** Begin a text section from the bottom-left of the document. */
    fun beginText()

    /** End the current text section.*/
    fun endText()

    /** Start a new line offset by ([offsetX], [offsetY]) from the current line. */
    fun newLineAtOffset(offsetX: Float, offsetY: Float)

    /** Set the font to be used for text. */
    fun setFont(font: PdfFont, size: Float)

    /** Get the width of the given [text] with font [font] and font size [size]. */
    fun getTextWidth(text: String, font: PdfFont, size: Float): Float

    /** Draw and stroke the given [text]. */
    fun drawText(text: String)

    /** Set the line width. */
    fun setLineWidth(width: Float)

    /** Set the stroke color. */
    fun setStrokeColor(r: Float, g: Float, b: Float)

    /** Set the fill color. */
    fun setFillColor(r: Float, g: Float, b: Float)

    /** Add a line path from ([x1], [y1]) to ([x2], [y2]). */
    fun addLine(x1: Float, y1: Float, x2: Float, y2: Float)

    /** Add a rectangular path from bottom-left coordinates ([x], [y]). */
    fun addRect(x: Float, y: Float, width: Float, height: Float)

    /** Add a circular path of radius [r] from bottom-left coordinates ([x], [y]). */
    fun addCircle(x: Float, y: Float, radius: Float)

    /** Draw a stroke around the current path. */
    fun stroke()

    /** Fill the current path. */
    fun fill()

    /** Draw a stroke around the current path and fill it. */
    fun fillAndStroke()

    /** Draw the given image from bottom-left coordinates ([x], [y]). */
    fun drawImage(x: Float, y: Float, width: Float, height: Float, imageData: ByteArray)

    /** Return this document as a PDF [ByteArray]. */
    fun toByteArray(): ByteArray
}