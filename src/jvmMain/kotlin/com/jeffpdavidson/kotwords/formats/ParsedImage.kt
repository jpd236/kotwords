package com.jeffpdavidson.kotwords.formats

import okio.Closeable
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

internal actual class ParsedImage private constructor(private val bufferedImage: BufferedImage) : Closeable {
    actual val width = bufferedImage.width
    actual val height = bufferedImage.height

    actual fun containsVisiblePixels(): Boolean {
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (bufferedImage.getRGB(x, y) and 0xff000000.toInt() != 0) {
                    return true
                }
            }
        }
        return false
    }

    actual fun crop(width: Int, height: Int, x: Int, y: Int): ParsedImage {
        return ParsedImage(bufferedImage.getSubimage(x, y, width, height))
    }

    actual suspend fun toPngBytes(): ByteArray {
        val output = ByteArrayOutputStream()
        ImageIO.write(bufferedImage, "png", output)
        return output.toByteArray()
    }

    override fun close() {}

    actual companion object {
        actual suspend fun parse(format: ParsedImageFormat, data: ByteArray): ParsedImage {
            return ParsedImage(ImageIO.read(ByteArrayInputStream(data)))
        }
    }
}