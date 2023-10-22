package com.jeffpdavidson.kotwords.formats

import org.apache.pdfbox.Loader
import org.apache.pdfbox.rendering.PDFRenderer
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import kotlin.test.assertEquals

actual object ImageComparator {
    actual suspend fun assertPdfEquals(expected: ByteArray, actual: ByteArray) {
        assertImageEquals(render(expected), render(actual))
    }

    actual suspend fun assertPngEquals(expected: ByteArray, actual: ByteArray) {
        assertImageEquals(ImageIO.read(ByteArrayInputStream(expected)), ImageIO.read(ByteArrayInputStream(actual)))
    }

    private fun assertImageEquals(expectedImage: BufferedImage, actualImage: BufferedImage) {
        assertEquals(expectedImage.width, actualImage.width)
        assertEquals(expectedImage.height, actualImage.height)
        for (y in 0 until expectedImage.height) {
            for (x in 0 until expectedImage.width) {
                assertEquals(expectedImage.getRGB(x, y), actualImage.getRGB(x, y))
            }
        }
    }

    private fun render(pdfBytes: ByteArray): BufferedImage {
        Loader.loadPDF(pdfBytes).use {
            assertEquals(1, it.numberOfPages)
            val renderer = PDFRenderer(it)
            return renderer.renderImage(0)
        }
    }
}