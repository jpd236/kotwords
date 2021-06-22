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
                Font.COURIER,
                10f,
                100f
            ).lines
        )
    }

    @Test
    fun splitTextToLines_withHtmlFormatting() {
        val document = PdfDocument()
        assertEquals(
            listOf(
                Pdf.ClueTextElement.Text("a "),
                Pdf.ClueTextElement.SetFont(Font.COURIER_BOLD),
                Pdf.ClueTextElement.Text("b c"),
                Pdf.ClueTextElement.SetFont(Font.COURIER),
                Pdf.ClueTextElement.Text(" d e "),
                Pdf.ClueTextElement.SetFont(Font.COURIER_ITALIC),
                Pdf.ClueTextElement.Text("f g"),
                Pdf.ClueTextElement.SetFont(Font.COURIER),
                Pdf.ClueTextElement.Text(" h "),
                Pdf.ClueTextElement.SetFont(Font.COURIER_BOLD),
                Pdf.ClueTextElement.SetFont(Font.COURIER_BOLD_ITALIC),
                Pdf.ClueTextElement.NewLine,

                Pdf.ClueTextElement.Text("i"),
                Pdf.ClueTextElement.SetFont(Font.COURIER),
                Pdf.ClueTextElement.Text(" jj kk ll mm "),
                Pdf.ClueTextElement.SetFont(Font.COURIER_BOLD),
                Pdf.ClueTextElement.Text("nn"),
                Pdf.ClueTextElement.NewLine,

                Pdf.ClueTextElement.Text("oo"),
                Pdf.ClueTextElement.SetFont(Font.COURIER),
                Pdf.ClueTextElement.Text(" ppp qqq "),
                Pdf.ClueTextElement.SetFont(Font.COURIER_ITALIC),
                Pdf.ClueTextElement.Text("rrr "),
                Pdf.ClueTextElement.SetFont(Font.COURIER_BOLD_ITALIC),
                Pdf.ClueTextElement.NewLine,

                Pdf.ClueTextElement.Text("sss"),
                Pdf.ClueTextElement.SetFont(Font.COURIER_ITALIC),
                Pdf.ClueTextElement.Text(" tttt uuuu"),
                Pdf.ClueTextElement.SetFont(Font.COURIER),
                Pdf.ClueTextElement.NewLine,

                Pdf.ClueTextElement.Text("vvvv")
            ),
            splitTextToLines(
                document,
                "a <b>b c</b> d e <i>f g</i> h <b><i>i</i></b> jj kk ll mm <b>nn oo</b> ppp qqq <i>rrr <b>sss</b> tttt uuuu</i> vvvv",
                Pdf.FontFamily.COURIER,
                10f,
                100f,
                isHtml = true,
            )
        )
    }

    @Test
    fun splitTextToLines_longWords() {
        val document = PdfDocument()
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
                Font.COURIER,
                10f,
                100f
            ).lines
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