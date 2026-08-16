package com.jeffpdavidson.kotwords.formats

import korlibs.image.core.CoreImage
import korlibs.image.core.CoreImage32
import korlibs.image.core.CoreImage32Color
import korlibs.image.core.CoreImageFormat
import korlibs.image.core.decodeBytes
import korlibs.image.core.encodeBytes

internal class ParsedImage private constructor(private val coreImage: CoreImage32) {
    val width: Int = coreImage.width
    val height: Int = coreImage.height

    fun containsVisiblePixels(): Boolean {
        return coreImage.data.any {
            CoreImage32Color(it).alpha != 0
        }
    }

    fun crop(
        width: Int,
        height: Int,
        x: Int,
        y: Int
    ): ParsedImage {
        val croppedData = IntArray(width * height)
        (0 until height).forEach { row ->
            val sourceRowStart = (y + row) * this.width + x
            val sourceRowEnd = sourceRowStart + width
            val destinationRowOffset = row * width
            coreImage.data.copyInto(
                destination = croppedData,
                destinationOffset = destinationRowOffset,
                startIndex = sourceRowStart,
                endIndex = sourceRowEnd
            )
        }
        return ParsedImage(CoreImage32(width, height, croppedData).to32())
    }

    suspend fun toPngBytes(): ByteArray {
        return coreImage.encodeBytes(CoreImageFormat.PNG)
    }

    companion object {
        suspend fun parse(data: ByteArray): ParsedImage {
            return ParsedImage(CoreImage.decodeBytes(data).to32())
        }
    }
}