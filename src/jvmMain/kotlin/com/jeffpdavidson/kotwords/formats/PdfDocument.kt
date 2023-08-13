package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.model.Puzzle
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.font.PDType0Font
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

actual class PdfDocument {
    private val document = PDDocument()
    private val page = PDPage()
    private val content: PDPageContentStream

    actual val width: Float
    actual val height: Float

    private val loadedTtfFonts: MutableMap<Pair<String, String>, PDType0Font> = mutableMapOf()

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

    actual suspend fun setFont(font: PdfFont, size: Float) {
        content.setFont(font.toPdfFont(), size)
    }

    actual suspend fun getTextWidth(text: String, font: PdfFont, size: Float): Float {
        return font.toPdfFont().getStringWidth(text) * size / 1000
    }

    actual suspend fun drawText(text: String) {
        content.showText(text)
    }

    actual fun setStrokeColor(r: Float, g: Float, b: Float) {
        content.setStrokingColor(r, g, b)
    }

    actual fun setFillColor(r: Float, g: Float, b: Float) {
        content.setNonStrokingColor(r, g, b)
    }

    actual fun setLineWidth(width: Float) {
        content.setLineWidth(width)
    }

    actual fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float) {
        content.moveTo(x1, y1)
        content.lineTo(x2, y2)
        content.stroke()
    }

    actual fun drawRect(x: Float, y: Float, width: Float, height: Float, stroke: Boolean, fill: Boolean) {
        content.addRect(x, y, width, height)
        fillAndOrStroke(stroke, fill)
    }

    actual fun drawCircle(x: Float, y: Float, radius: Float, stroke: Boolean, fill: Boolean) {
        // PDFBox doesn't support circles natively, so draw the curves manually.
        val k = 0.552284749831f
        val cx = x + radius
        val cy = y + radius
        content.moveTo(cx - radius, cy)
        content.curveTo(cx - radius, cy + k * radius, cx - k * radius, cy + radius, cx, cy + radius)
        content.curveTo(cx + k * radius, cy + radius, cx + radius, cy + k * radius, cx + radius, cy)
        content.curveTo(cx + radius, cy - k * radius, cx + k * radius, cy - radius, cx, cy - radius)
        content.curveTo(cx - k * radius, cy - radius, cx - radius, cy - k * radius, cx - radius, cy)
        fillAndOrStroke(stroke, fill)
    }

    private fun fillAndOrStroke(stroke: Boolean, fill: Boolean) {
        when {
            stroke && fill -> content.fillAndStroke()
            stroke -> content.stroke()
            fill -> content.fill()
        }
    }

    actual suspend fun drawImage(x: Float, y: Float, width: Float, height: Float, image: Puzzle.Image.Data) {
        val pdImage = PDImageXObject.createFromByteArray(document, image.bytes.toByteArray(), /* name = */ null)
        content.drawImage(pdImage, x, y, width, height)
    }

    actual suspend fun toByteArray(): ByteArray {
        content.close()
        ByteArrayOutputStream().use { stream ->
            document.save(stream)
            document.close()
            return stream.toByteArray()
        }
    }

    private fun PdfFont.toPdfFont(): PDFont {
        return when (this) {
            is PdfFont.BuiltInFont -> {
                when (fontName) {
                    BuiltInFontName.COURIER -> PDType1Font.COURIER
                    BuiltInFontName.COURIER_BOLD -> PDType1Font.COURIER_BOLD
                    BuiltInFontName.COURIER_ITALIC -> PDType1Font.COURIER_OBLIQUE
                    BuiltInFontName.COURIER_BOLD_ITALIC -> PDType1Font.COURIER_BOLD_OBLIQUE
                    BuiltInFontName.TIMES_ROMAN -> PDType1Font.TIMES_ROMAN
                    BuiltInFontName.TIMES_BOLD -> PDType1Font.TIMES_BOLD
                    BuiltInFontName.TIMES_ITALIC -> PDType1Font.TIMES_ITALIC
                    BuiltInFontName.TIMES_BOLD_ITALIC -> PDType1Font.TIMES_BOLD_ITALIC
                }
            }
            is PdfFont.TtfFont -> {
                loadedTtfFonts.getOrPut(fontName to fontStyle) {
                    PDType0Font.load(document, ByteArrayInputStream(fontData))
                }
            }
        }
    }

    actual companion object {
        actual suspend fun create(): PdfDocument = PdfDocument()
    }
}