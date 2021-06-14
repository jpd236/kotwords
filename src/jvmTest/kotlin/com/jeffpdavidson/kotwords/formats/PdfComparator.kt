package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.readBinaryResource
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import java.awt.image.BufferedImage
import kotlin.test.assertEquals

actual object PdfComparator {
    actual suspend fun assertPdfEquals(expected: ByteArray, actual: ByteArray) {
        val expectedImage = render(expected)
        val actualImage = render(actual)
        assertEquals(expectedImage.width, actualImage.width)
        assertEquals(expectedImage.height, actualImage.height)

        for (y in 0 until expectedImage.height) {
            for (x in 0 until expectedImage.width) {
                assertEquals(expectedImage.getRGB(x, y), actualImage.getRGB(x, y))
            }
        }
    }

    private fun render(pdfBytes: ByteArray): BufferedImage {
        PDDocument.load(pdfBytes).use {
            assertEquals(1, it.numberOfPages)
            val renderer = PDFRenderer(it)
            return renderer.renderImage(0)
        }
    }
}