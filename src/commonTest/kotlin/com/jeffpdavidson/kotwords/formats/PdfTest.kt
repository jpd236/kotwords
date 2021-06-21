package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.Pdf.asPdf
import com.jeffpdavidson.kotwords.formats.Pdf.splitTextToLines
import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PdfTest {
    @Test
    fun asPdf() = runTest {
        PdfComparator.assertPdfEquals(
            readBinaryResource(PdfComparator::class, "pdf/test.pdf"),
            AcrossLite(readBinaryResource(PdfTest::class, "puz/test.puz"))
                .asCrossword().asPdf(blackSquareLightnessAdjustment = 0.75f)
        )
    }

    // Note for splitTextToLines tests: a 100 pt line fits 16 10pt Courier characters.

    @Test
    fun splitTextToLines_standardText() {
        val document = PdfDocument()
        document.setFont(Font.COURIER, 10f)
        assertEquals(
            listOf(
                "a b c d e f g h",
                "i jj kk ll mm nn",
                "oo ppp qqq rrr",
                "sss tttt uuuu",
                "vvvv"
            ),
            splitTextToLines(
                document,
                "a b c d e f g h i jj kk ll mm nn oo ppp qqq rrr sss tttt uuuu vvvv",
                10f,
                100f
            )
        )
    }

    @Test
    fun splitTextToLines_longWords() {
        val document = PdfDocument()
        document.setFont(Font.COURIER, 10f)
        assertEquals(
            listOf(
                "1234567890123456",
                "7890 12345678901",
                "234567890 123456",
                "78901234567890"
            ),
            splitTextToLines(
                document,
                "12345678901234567890 12345678901234567890 12345678901234567890",
                10f,
                100f
            )
        )
    }
}

/**
 * Comparator to use to evaluate whether two PDFs are equal.
 *
 * Since PDFs are generally non-deterministic due to timestamps and unique identifiers, and also have many different
 * ways to render the same content, we generally want to compare PDFs by rendering them as images and comparing the
 * image contents.
 */
expect object PdfComparator {
    suspend fun assertPdfEquals(expected: ByteArray, actual: ByteArray)
}