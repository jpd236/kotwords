package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.js.Interop.toByteArray
import com.jeffpdavidson.kotwords.js.JsPDF
import com.jeffpdavidson.kotwords.js.newJsPdfOptions
import org.khronos.webgl.ArrayBuffer

/**
 * Javascript implementation of [PdfDocument], built atop jsPDF.
 *
 * Note that jsPDF uses a different coordinate system where (0,0) is the top-left instead of the bottom-left. When
 * rendering, we should always invert the provided y coordinate against the height of the document.
 */
internal actual class PdfDocument {
    private val pdf = JsPDF(newJsPdfOptions())

    init {
        pdf.setLineWidth(1.0f)
    }

    actual val width: Float = pdf.internal.pageSize.getWidth()
    actual val height: Float = pdf.internal.pageSize.getHeight()

    // Track the current offset, since jsPDF doesn't do this.
    private var textOffsetX = 0f
    private var textOffsetY = 0f

    actual fun beginText() {
        textOffsetX = 0f
        textOffsetY = 0f
    }

    actual fun endText() {}

    actual fun newLineAtOffset(offsetX: Float, offsetY: Float) {
        this.textOffsetX += offsetX
        this.textOffsetY += offsetY
    }

    actual fun setFont(font: Font, size: Float) {
        val (fontName, fontStyle) = when (font) {
            Font.COURIER -> "courier" to "normal"
            Font.TIMES_BOLD -> "times" to "bold"
            Font.TIMES_ROMAN -> "times" to "normal"
            Font.TIMES_ITALIC -> "times" to "italic"
        }
        pdf.setFont(fontName, fontStyle)
        pdf.setFontSize(size)
    }

    actual fun getTextWidth(text: String, size: Float): Float {
        return pdf.getStringUnitWidth(text) * size
    }

    actual fun drawText(text: String) {
        pdf.text(text, textOffsetX, height - textOffsetY)
    }

    actual fun setStrokeColor(r: Float, g: Float, b: Float) {
        pdf.setDrawColor(r.toString(), g.toString(), b.toString())
    }

    actual fun setFillColor(r: Float, g: Float, b: Float) {
        pdf.setFillColor(r.toString(), g.toString(), b.toString())
    }

    actual fun addRect(x: Float, y: Float, width: Float, height: Float) {
        pdf.rect(x, this.height - y - height, width, height, style = null)
    }

    actual fun addCircle(x: Float, y: Float, radius: Float) {
        pdf.circle(x + radius, height - y - radius, radius, style = null)
    }

    actual fun stroke() {
        pdf.stroke()
    }

    actual fun fillAndStroke() {
        pdf.fillStroke()
    }

    actual fun toByteArray(): ByteArray {
        return (pdf.output("arraybuffer") as ArrayBuffer).toByteArray()
    }
}