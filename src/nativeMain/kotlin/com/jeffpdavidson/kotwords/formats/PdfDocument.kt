package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.model.Puzzle

actual class PdfDocument {
    actual val width: Float
        get() = TODO("Not yet implemented")
    actual val height: Float
        get() = TODO("Not yet implemented")

    actual fun beginText() {
        TODO("Not yet implemented")
    }

    actual fun endText() {
        TODO("Not yet implemented")
    }

    actual fun newLineAtOffset(offsetX: Float, offsetY: Float) {
        TODO("Not yet implemented")
    }

    actual suspend fun setFont(font: PdfFont, size: Float) {
        TODO("Not yet implemented")
    }

    actual suspend fun getTextWidth(
        text: String,
        font: PdfFont,
        size: Float
    ): Float {
        TODO("Not yet implemented")
    }

    actual suspend fun drawText(text: String) {
        TODO("Not yet implemented")
    }

    actual fun setLineWidth(width: Float) {
        TODO("Not yet implemented")
    }

    actual fun setStrokeColor(r: Float, g: Float, b: Float) {
        TODO("Not yet implemented")
    }

    actual fun setFillColor(r: Float, g: Float, b: Float) {
        TODO("Not yet implemented")
    }

    actual fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float) {
        TODO("Not yet implemented")
    }

    actual fun drawRect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        stroke: Boolean,
        fill: Boolean
    ) {
        TODO("Not yet implemented")
    }

    actual fun drawCircle(
        x: Float,
        y: Float,
        radius: Float,
        stroke: Boolean,
        fill: Boolean
    ) {
        TODO("Not yet implemented")
    }

    actual suspend fun drawImage(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        image: Puzzle.Image.Data
    ) {
        TODO("Not yet implemented")
    }

    actual suspend fun toByteArray(): ByteArray {
        TODO("Not yet implemented")
    }

    actual companion object {
        actual suspend fun create(): PdfDocument {
            TODO("Not yet implemented")
        }
    }
}