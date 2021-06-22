package com.jeffpdavidson.kotwords.formats

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType1Font
import java.io.ByteArrayOutputStream

internal actual class PdfDocument {
    private val document = PDDocument()
    private val page = PDPage()
    private val content: PDPageContentStream

    actual val width: Float
    actual val height: Float

    init {
        document.addPage(page)
        width = page.mediaBox.width
        height = page.mediaBox.height
        content = PDPageContentStream(document, page)
    }

    actual fun beginText() {
        content.beginText()
    }

    actual fun endText() {
        content.endText()
    }

    actual fun newLineAtOffset(offsetX: Float, offsetY: Float) {
        content.newLineAtOffset(offsetX, offsetY)
    }

    actual fun setFont(font: Font, size: Float) {
        content.setFont(font.toPdfFont(), size)
    }

    actual fun getTextWidth(text: String, font: Font, size: Float): Float {
        return font.toPdfFont().getStringWidth(text) * size / 1000
    }

    actual fun drawText(text: String) {
        content.showText(text)
    }

    actual fun setStrokeColor(r: Float, g: Float, b: Float) {
        content.setStrokingColor(r, g, b)
    }

    actual fun setFillColor(r: Float, g: Float, b: Float) {
        content.setNonStrokingColor(r, g, b)
    }

    actual fun addRect(x: Float, y: Float, width: Float, height: Float) {
        content.addRect(x, y, width, height)
    }

    actual fun addCircle(x: Float, y: Float, radius: Float) {
        // PDFBox doesn't support circles natively, so draw the curves manually.
        val k = 0.552284749831f
        val cx = x + radius
        val cy = y + radius
        content.moveTo(cx - radius, cy)
        content.curveTo(cx - radius, cy + k * radius, cx - k * radius, cy + radius, cx, cy + radius)
        content.curveTo(cx + k * radius, cy + radius, cx + radius, cy + k * radius, cx + radius, cy)
        content.curveTo(cx + radius, cy - k * radius, cx + k * radius, cy - radius, cx, cy - radius)
        content.curveTo(cx - k * radius, cy - radius, cx - radius, cy - k * radius, cx - radius, cy)
    }

    actual fun stroke() {
        content.stroke()
    }

    actual fun fillAndStroke() {
        content.fillAndStroke()
    }

    actual fun toByteArray(): ByteArray {
        content.close()
        ByteArrayOutputStream().use { stream ->
            document.save(stream)
            return stream.toByteArray()
        }
    }

    private fun Font.toPdfFont(): PDType1Font {
        return when (this) {
            Font.COURIER -> PDType1Font.COURIER
            Font.COURIER_BOLD -> PDType1Font.COURIER_BOLD
            Font.COURIER_ITALIC -> PDType1Font.COURIER_OBLIQUE
            Font.COURIER_BOLD_ITALIC -> PDType1Font.COURIER_BOLD_OBLIQUE
            Font.TIMES_ROMAN -> PDType1Font.TIMES_ROMAN
            Font.TIMES_BOLD -> PDType1Font.TIMES_BOLD
            Font.TIMES_ITALIC -> PDType1Font.TIMES_ITALIC
            Font.TIMES_BOLD_ITALIC -> PDType1Font.TIMES_BOLD_ITALIC
        }
    }
}