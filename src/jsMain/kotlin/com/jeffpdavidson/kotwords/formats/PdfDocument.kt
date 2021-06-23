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
        setLineWidth(1.0f)
    }

    actual val width: Float = pdf.internal.pageSize.getWidth()
    actual val height: Float = pdf.internal.pageSize.getHeight()

    // Track the current offset, since jsPDF doesn't do this.
    private var textOffsetX = 0f
    private var lineOffsetX = 0f
    private var textOffsetY = 0f

    private val loadedTtfFonts: MutableSet<Pair<String, String>> = mutableSetOf()
    private var currentFont: PdfFont? = null
    private var currentFontSize: Float? = null

    actual fun beginText() {
        textOffsetX = 0f
        textOffsetY = 0f
        lineOffsetX = 0f
    }

    actual fun endText() {}

    actual fun newLineAtOffset(offsetX: Float, offsetY: Float) {
        this.textOffsetX += offsetX
        this.textOffsetY += offsetY
        this.lineOffsetX = 0f
    }

    actual fun setFont(font: PdfFont, size: Float) {
        setFont(font)
        pdf.setFontSize(size)
        currentFont = font
        currentFontSize = size
    }

    private fun setFont(font: PdfFont) {
        val (fontName, fontStyle) = when (font) {
            is PdfFont.BuiltInFont -> {
                when (font.fontName) {
                    BuiltInFontName.COURIER -> "courier" to "normal"
                    BuiltInFontName.COURIER_BOLD -> "courier" to "bold"
                    BuiltInFontName.COURIER_ITALIC -> "courier" to "italic"
                    BuiltInFontName.COURIER_BOLD_ITALIC -> "courier" to "bolditalic"
                    BuiltInFontName.TIMES_ROMAN -> "times" to "normal"
                    BuiltInFontName.TIMES_BOLD -> "times" to "bold"
                    BuiltInFontName.TIMES_ITALIC -> "times" to "italic"
                    BuiltInFontName.TIMES_BOLD_ITALIC -> "times" to "bolditalic"
                }
            }
            is PdfFont.TtfFont -> {
                if (!loadedTtfFonts.contains(font.fontName to font.fontStyle)) {
                    val fileName = "${font.fontName}-${font.fontStyle}.ttf"
                    pdf.addFileToVFS(fileName, Encodings.encodeBase64(font.fontData))
                    pdf.addFont(fileName, font.fontName, font.fontStyle)
                    loadedTtfFonts.add(font.fontName to font.fontStyle)
                }
                font.fontName to font.fontStyle
            }
        }
        pdf.setFont(fontName, fontStyle)
    }

    actual fun getTextWidth(text: String, font: PdfFont, size: Float): Float {
        // getStringUnitWidth uses the current font, so temporarily set it to the desired value.
        setFont(font)
        val result = pdf.getStringUnitWidth(text) * size
        currentFont?.let { setFont(it) }
        return result
    }

    actual fun drawText(text: String) {
        pdf.text(text, textOffsetX + lineOffsetX, height - textOffsetY)
        lineOffsetX += getTextWidth(text, currentFont!!, currentFontSize!!)
    }

    actual fun setStrokeColor(r: Float, g: Float, b: Float) {
        pdf.setDrawColor(r.toString(), g.toString(), b.toString())
    }

    actual fun setFillColor(r: Float, g: Float, b: Float) {
        pdf.setFillColor(r.toString(), g.toString(), b.toString())
    }

    actual fun setLineWidth(width: Float) {
        pdf.setLineWidth(width)
    }

    actual fun addLine(x1: Float, y1: Float, x2: Float, y2: Float) {
        pdf.line(x1, y1, x2, y2, style = null)
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
