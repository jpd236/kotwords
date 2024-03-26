package com.jeffpdavidson.kotwords.formats

import okio.Closeable

internal actual class ParsedImage : Closeable {
    actual val width: Int
        get() = TODO("Not yet implemented")
    actual val height: Int
        get() = TODO("Not yet implemented")

    actual fun containsVisiblePixels(): Boolean {
        TODO("Not yet implemented")
    }

    actual fun crop(
        width: Int,
        height: Int,
        x: Int,
        y: Int
    ): ParsedImage {
        TODO("Not yet implemented")
    }

    actual suspend fun toPngBytes(): ByteArray {
        TODO("Not yet implemented")
    }

    actual companion object {
        actual suspend fun parse(
            format: ParsedImageFormat,
            data: ByteArray
        ): ParsedImage {
            TODO("Not yet implemented")
        }
    }

    override fun close() {
        TODO("Not yet implemented")
    }
}