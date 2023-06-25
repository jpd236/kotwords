@file:JsModule("pdf-lib")
@file:JsNonModule

package com.jeffpdavidson.kotwords.js

import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import kotlin.js.Promise

internal external class PDFDocument {
    fun addPage(size: PageSizes): PDFPage
    fun registerFontkit(fontkit: Fontkit)
    fun embedStandardFont(font: StandardFonts): PDFFont
    fun embedFont(font: ArrayBuffer, options: EmbedFontOptions): Promise<PDFFont>
    fun embedPng(png: ArrayBuffer): Promise<PDFImage>
    fun embedJpg(jpg: ArrayBuffer): Promise<PDFImage>
    fun save(): Promise<Uint8Array>

    companion object {
        fun create(): Promise<PDFDocument>
    }
}

internal external class PDFPage {
    fun getWidth(): Float
    fun getHeight(): Float
    fun moveTo(x: Float, y: Float)
    fun moveRight(x: Float)
    fun moveUp(y: Float)
    fun setFont(font: PDFFont)
    fun setFontSize(fontSize: Float)
    fun drawText(text: String)
    fun drawLine(options: PDFPageDrawLineOptions)
    fun drawRectangle(options: PDFPageDrawRectangleOptions)
    fun drawCircle(options: PDFPageDrawCircleOptions)
    fun drawImage(image: PDFImage, options: PDFPageDrawImageOptions)
}

internal external class PDFFont {
    fun widthOfTextAtSize(text: String, size: Float): Float
}

internal external class PDFImage

internal external enum class PageSizes {
    Letter
}

internal external enum class StandardFonts {
    Courier,
    CourierBold,
    CourierBoldOblique,
    CourierOblique,
    TimesRoman,
    TimesRomanBold,
    TimesRomanBoldItalic,
    TimesRomanItalic
}

internal external interface EmbedFontOptions {
    var subset: Boolean
}

internal external interface RGB

internal external fun rgb(red: Float, green: Float, blue: Float): RGB

internal external interface Point {
    var x: Float
    var y: Float
}

internal external interface PDFPageDrawLineOptions {
    var start: Point
    var end: Point
    var thickness: Float
    var color: RGB
}

internal external interface PDFPageDrawRectangleOptions {
    var x: Float
    var y: Float
    var width: Float
    var height: Float
    var borderWidth: Float?
    var borderColor: RGB?
    var color: RGB?
}

internal external interface PDFPageDrawCircleOptions {
    var x: Float
    var y: Float
    var size: Float
    var borderWidth: Float?
    var borderColor: RGB?
    var color: RGB?
}

internal external interface PDFPageDrawImageOptions {
    var x: Float
    var y: Float
    var width: Float
    var height: Float
}