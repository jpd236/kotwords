package com.jeffpdavidson.kotwords.formats

import okio.Closeable

internal enum class ParsedImageFormat {
    GIF,
    JPG,
    PNG;

    companion object {
        fun fromExtension(extension: String): ParsedImageFormat {
            return when (extension.lowercase()) {
                "gif" -> GIF
                "jpg", "jpeg" -> JPG
                "png" -> PNG
                else -> throw UnsupportedOperationException("Unknown image type $extension")
            }
        }
    }
}

internal expect class ParsedImage : Closeable {
    val width: Int
    val height: Int

    fun containsVisiblePixels(): Boolean

    fun crop(width: Int, height: Int, x: Int, y: Int): ParsedImage

    suspend fun toPngBytes(): ByteArray

    companion object {
        suspend fun parse(format: ParsedImageFormat, data: ByteArray): ParsedImage
    }
}