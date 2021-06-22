@file:JsModule("jspdf")
@file:JsNonModule

package com.jeffpdavidson.kotwords.js

@JsName("jsPDF")
internal external class JsPDF(options: JsPdfOptions = definedExternally) {
    val internal: Internal

    fun setFont(fontName: String, fontStyle: String)
    fun setFontSize(size: Float)
    fun getStringUnitWidth(text: String): Float
    fun text(text: String, x: Float, y: Float)

    fun setDrawColor(r: String, g: String, b: String)
    fun setFillColor(r: String, g: String, b: String)
    fun setLineWidth(width: Float)

    fun line(x1: Float, y1: Float, x2: Float, y2: Float, style: String? = definedExternally)
    fun rect(x: Float, y: Float, width: Float, height: Float, style: String? = definedExternally)
    fun circle(x: Float, y: Float, radius: Float, style: String? = definedExternally)
    fun stroke()
    fun fillStroke()

    fun output(type: String): Any
}

internal external interface Internal {
    val pageSize: PageSize
}

internal external interface PageSize {
    fun getWidth(): Float
    fun getHeight(): Float
}

internal external interface JsPdfOptions {
    var unit: String
    var format: String
}