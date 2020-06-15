package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.Pdf.asPdf
import com.jeffpdavidson.kotwords.formats.Pdf.splitTextToLines
import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.runTest
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.rendering.PDFRenderer
import org.junit.Test
import java.awt.image.BufferedImage
import kotlin.test.assertEquals

class PdfTest {
    init {
        // Speed up rendering on Java 8.
        System.setProperty("sun.java2d.cmm", "sun.java2d.cmm.kcms.KcmsServiceProvider")
    }

    @Test
    fun asPdf() = runTest {
        assertPdfEquals(
                readBinaryResource(PdfTest::class, "pdf/test.pdf"),
                AcrossLite(readBinaryResource(PdfTest::class, "puz/test.puz"))
                        .asCrossword().asPdf()
        )
    }

    // Note for splitTextToLines tests: a 100 pt line fits 16 10pt Courier characters.

    @Test
    fun splitTextToLines_standardText() {
        assertEquals(
                listOf(
                        "a b c d e f g h",
                        "i jj kk ll mm nn",
                        "oo ppp qqq rrr",
                        "sss tttt uuuu",
                        "vvvv"
                ),
                splitTextToLines(
                        "a b c d e f g h i jj kk ll mm nn oo ppp qqq rrr sss tttt uuuu vvvv",
                        PDType1Font.COURIER,
                        10f,
                        100f
                )
        )
    }

    @Test
    fun splitTextToLines_longWords() {
        assertEquals(
                listOf(
                        "1234567890123456",
                        "7890 12345678901",
                        "234567890 123456",
                        "78901234567890"
                ),
                splitTextToLines(
                        "12345678901234567890 12345678901234567890 12345678901234567890",
                        PDType1Font.COURIER,
                        10f,
                        100f
                )
        )
    }

    private fun assertPdfEquals(expected: ByteArray, actual: ByteArray) {
        // Raw PDF contents are non-deterministic, so compare the rendered images instead.
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