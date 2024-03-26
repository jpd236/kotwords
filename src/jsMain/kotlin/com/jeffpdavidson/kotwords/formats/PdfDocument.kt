package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.js.FontkitModule
import com.jeffpdavidson.kotwords.js.Interop.toArrayBuffer
import com.jeffpdavidson.kotwords.js.Interop.toByteArray
import com.jeffpdavidson.kotwords.js.PDFDocument
import com.jeffpdavidson.kotwords.js.PDFFont
import com.jeffpdavidson.kotwords.js.PDFPage
import com.jeffpdavidson.kotwords.js.PageSizes
import com.jeffpdavidson.kotwords.js.RGB
import com.jeffpdavidson.kotwords.js.StandardFonts
import com.jeffpdavidson.kotwords.js.newEmbedFontOptions
import com.jeffpdavidson.kotwords.js.newPDFPageDrawCircleOptions
import com.jeffpdavidson.kotwords.js.newPDFPageDrawImageOptions
import com.jeffpdavidson.kotwords.js.newPDFPageDrawLineOptions
import com.jeffpdavidson.kotwords.js.newPDFPageDrawRectangleOptions
import com.jeffpdavidson.kotwords.js.newPoint
import com.jeffpdavidson.kotwords.js.rgb
import com.jeffpdavidson.kotwords.model.Puzzle
import kotlinx.coroutines.await

/** Javascript implementation of [PdfDocument], built atop pdf-lib. */
actual class PdfDocument {

    private lateinit var pdf: PDFDocument
    private lateinit var page: PDFPage

    private var currentFont: PDFFont? = null
    private var fontSize: Float = 0f
    private val loadedFonts = mutableMapOf<PdfFont, PDFFont>()

    private var lineWidth: Float = 1f
    private var strokeColor: RGB = rgb(0f, 0f, 0f)
    private var fillColor: RGB = rgb(1f, 1f, 1f)

    private var xOffset: Float = 0f

    private suspend fun init() {
        pdf = PDFDocument.create().await()
        pdf.registerFontkit(FontkitModule.default)
        page = pdf.addPage(PageSizes.Letter)
    }

    actual val width: Float get() = page.getWidth()
    actual val height: Float get() = page.getHeight()

    actual fun beginText() {
        xOffset = 0f
        page.moveTo(0f, 0f)
    }

    actual fun endText() {}

    actual fun newLineAtOffset(offsetX: Float, offsetY: Float) {
        page.moveRight(offsetX - xOffset)
        xOffset = 0f
        page.moveUp(offsetY)
    }

    actual suspend fun setFont(font: PdfFont, size: Float) {
        setFont(font)
        fontSize = size
        page.setFontSize(size)
    }

    private suspend fun setFont(font: PdfFont) {
        val newFont = loadFont(font)
        if (currentFont == newFont) {
            return
        }
        page.setFont(newFont)
        currentFont = newFont
    }

    private suspend fun loadFont(font: PdfFont): PDFFont = loadedFonts.getOrPut(font) {
        when (font) {
            is PdfFont.BuiltInFont -> {
                val standardFont = when (font.fontName) {
                    BuiltInFontName.COURIER -> StandardFonts.Courier
                    BuiltInFontName.COURIER_BOLD -> StandardFonts.CourierBold
                    BuiltInFontName.COURIER_ITALIC -> StandardFonts.CourierOblique
                    BuiltInFontName.COURIER_BOLD_ITALIC -> StandardFonts.CourierBoldOblique
                    BuiltInFontName.TIMES_ROMAN -> StandardFonts.TimesRoman
                    BuiltInFontName.TIMES_BOLD -> StandardFonts.TimesRomanBold
                    BuiltInFontName.TIMES_ITALIC -> StandardFonts.TimesRomanItalic
                    BuiltInFontName.TIMES_BOLD_ITALIC -> StandardFonts.TimesRomanBoldItalic
                }
                pdf.embedStandardFont(standardFont)
            }

            is PdfFont.TtfFont -> {
                pdf.embedFont(font.fontData.toArrayBuffer(), newEmbedFontOptions(subset = true)).await()
            }
        }
    }

    actual suspend fun getTextWidth(text: String, font: PdfFont, size: Float): Float {
        return loadFont(font).widthOfTextAtSize(text, size)
    }

    actual suspend fun drawText(text: String) {
        page.drawText(text)
        val width = currentFont!!.widthOfTextAtSize(text, fontSize)
        xOffset += width
        page.moveRight(width)
    }

    actual fun setLineWidth(width: Float) {
        lineWidth = width
    }

    actual fun setStrokeColor(r: Float, g: Float, b: Float) {
        strokeColor = rgb(r, g, b)
    }

    actual fun setFillColor(r: Float, g: Float, b: Float) {
        fillColor = rgb(r, g, b)
    }

    actual fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float) {
        page.drawLine(
            newPDFPageDrawLineOptions(
                start = newPoint(x1, y1),
                end = newPoint(x2, y2),
                thickness = lineWidth,
                color = strokeColor,
            )
        )
    }

    actual fun drawRect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        stroke: Boolean,
        fill: Boolean
    ) {
        page.drawRectangle(
            newPDFPageDrawRectangleOptions(
                x = x,
                y = y,
                width = width,
                height = height,
                borderWidth = if (stroke) lineWidth else undefined,
                borderColor = if (stroke) strokeColor else undefined,
                color = if (fill) fillColor else undefined,
            )
        )
    }

    actual fun drawCircle(
        x: Float,
        y: Float,
        radius: Float,
        stroke: Boolean,
        fill: Boolean
    ) {
        page.drawCircle(
            newPDFPageDrawCircleOptions(
                x = x + radius,
                y = y + radius,
                size = radius,
                borderWidth = if (stroke) lineWidth else undefined,
                borderColor = if (stroke) strokeColor else undefined,
                color = if (fill) fillColor else undefined,
            )
        )
    }

    actual suspend fun drawImage(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        image: Puzzle.Image.Data,
    ) {
        val pdfImage = when (image.format) {
            Puzzle.ImageFormat.PNG -> pdf.embedPng(image.bytes.toByteArray().toArrayBuffer())
            Puzzle.ImageFormat.JPG -> pdf.embedJpg(image.bytes.toByteArray().toArrayBuffer())
            else -> throw UnsupportedOperationException("Unsupported image format for PDFs: ${image.format}")
        }.await()
        page.drawImage(pdfImage, newPDFPageDrawImageOptions(x = x, y = y, width = width, height = height))
    }

    actual suspend fun toByteArray(): ByteArray {
        return pdf.save().await().buffer.toByteArray()
    }

    actual companion object {
        actual suspend fun create(): PdfDocument {
            return PdfDocument().also {
                it.init()
            }
        }
    }
}
