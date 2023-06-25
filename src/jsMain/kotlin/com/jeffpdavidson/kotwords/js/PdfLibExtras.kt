package com.jeffpdavidson.kotwords.js

internal fun newEmbedFontOptions(subset: Boolean): EmbedFontOptions {
    return js("{}").unsafeCast<EmbedFontOptions>().also {
        it.subset = subset
    }
}

internal fun newPDFPageDrawLineOptions(
    start: Point,
    end: Point,
    thickness: Float,
    color: RGB,
): PDFPageDrawLineOptions {
    return js("{}").unsafeCast<PDFPageDrawLineOptions>().also {
        it.start = start
        it.end = end
        it.thickness = thickness
        it.color = color
    }
}

internal fun newPoint(x: Float, y: Float): Point {
    return js("{}").unsafeCast<Point>().also {
        it.x = x
        it.y = y
    }
}

internal fun newPDFPageDrawRectangleOptions(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    borderWidth: Float?,
    borderColor: RGB?,
    color: RGB?,
): PDFPageDrawRectangleOptions {
    return js("{}").unsafeCast<PDFPageDrawRectangleOptions>().also {
        it.x = x
        it.y = y
        it.width = width
        it.height = height
        it.borderWidth = borderWidth
        it.borderColor = borderColor
        it.color = color
    }
}

internal fun newPDFPageDrawCircleOptions(
    x: Float,
    y: Float,
    size: Float,
    borderWidth: Float?,
    borderColor: RGB?,
    color: RGB?,
): PDFPageDrawCircleOptions {
    return js("{}").unsafeCast<PDFPageDrawCircleOptions>().also {
        it.x = x
        it.y = y
        it.size = size
        it.borderWidth = borderWidth
        it.borderColor = borderColor
        it.color = color
    }
}

internal fun newPDFPageDrawImageOptions(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
): PDFPageDrawImageOptions {
    return js("{}").unsafeCast<PDFPageDrawImageOptions>().also {
        it.x = x
        it.y = y
        it.width = width
        it.height = height
    }
}