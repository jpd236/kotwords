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

    @Test
    fun asPdf_customFonts() = runTest {
        val notoSerifFontFamily = PdfFontFamily(
            PdfFont.TtfFont(
                "NotoSerif", "normal", readBinaryResource(PdfTest::class, "pdf/fonts/NotoSerif-Regular.ttf")
            ),
            PdfFont.TtfFont(
                "NotoSerif", "bold", readBinaryResource(PdfTest::class, "pdf/fonts/NotoSerif-Bold.ttf")
            ),
            PdfFont.TtfFont(
                "NotoSerif", "italic", readBinaryResource(PdfTest::class, "pdf/fonts/NotoSerif-Italic.ttf")
            ),
            PdfFont.TtfFont(
                "NotoSerif", "bolditalic", readBinaryResource(PdfTest::class, "pdf/fonts/NotoSerif-BoldItalic.ttf")
            ),
        )
        PdfComparator.assertPdfEquals(
            readBinaryResource(PdfComparator::class, "pdf/test-customFonts.pdf"),
            AcrossLite(readBinaryResource(PdfTest::class, "puz/test.puz"))
                .asCrossword().asPdf(blackSquareLightnessAdjustment = 0.75f, fontFamily = notoSerifFontFamily)
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
                PdfFont.BuiltInFont(BuiltInFontName.COURIER),
                10f,
                100f
            )
        )
    }

    @Test
    fun splitTextToLines_withHtmlFormatting() {
        val document = PdfDocument()
        assertEquals(
            listOf(
                Pdf.ClueTextElement.Text("a "),
                Pdf.ClueTextElement.SetFont(PdfFont.BuiltInFont(BuiltInFontName.COURIER_BOLD)),
                Pdf.ClueTextElement.Text("b c"),
                Pdf.ClueTextElement.SetFont(PdfFont.BuiltInFont(BuiltInFontName.COURIER)),
                Pdf.ClueTextElement.Text(" d e "),
                Pdf.ClueTextElement.SetFont(PdfFont.BuiltInFont(BuiltInFontName.COURIER_ITALIC)),
                Pdf.ClueTextElement.Text("f g"),
                Pdf.ClueTextElement.SetFont(PdfFont.BuiltInFont(BuiltInFontName.COURIER)),
                Pdf.ClueTextElement.Text(" h"),
                Pdf.ClueTextElement.NewLine,

                Pdf.ClueTextElement.SetFont(PdfFont.BuiltInFont(BuiltInFontName.COURIER_BOLD_ITALIC)),
                Pdf.ClueTextElement.Text("i"),
                Pdf.ClueTextElement.SetFont(PdfFont.BuiltInFont(BuiltInFontName.COURIER)),
                Pdf.ClueTextElement.Text(" jj kk ll mm "),
                Pdf.ClueTextElement.SetFont(PdfFont.BuiltInFont(BuiltInFontName.COURIER_BOLD)),
                Pdf.ClueTextElement.Text("nn"),
                Pdf.ClueTextElement.NewLine,

                Pdf.ClueTextElement.Text("oo"),
                Pdf.ClueTextElement.SetFont(PdfFont.BuiltInFont(BuiltInFontName.COURIER)),
                Pdf.ClueTextElement.Text(" ppp qqq "),
                Pdf.ClueTextElement.SetFont(PdfFont.BuiltInFont(BuiltInFontName.COURIER_ITALIC)),
                Pdf.ClueTextElement.Text("rrr"),
                Pdf.ClueTextElement.NewLine,

                Pdf.ClueTextElement.SetFont(PdfFont.BuiltInFont(BuiltInFontName.COURIER_BOLD_ITALIC)),
                Pdf.ClueTextElement.Text("sss"),
                Pdf.ClueTextElement.SetFont(PdfFont.BuiltInFont(BuiltInFontName.COURIER_ITALIC)),
                Pdf.ClueTextElement.Text(" tttt uuuu"),
                Pdf.ClueTextElement.NewLine,

                Pdf.ClueTextElement.SetFont(PdfFont.BuiltInFont(BuiltInFontName.COURIER)),
                Pdf.ClueTextElement.Text("vvvv"),
                Pdf.ClueTextElement.NewLine,
            ),
            splitTextToLines(
                document,
                "a <b>b c</b> d e <i>f g</i> h " +
                        "<b><i>i</i></b> jj kk ll mm <b>nn " +
                        "oo</b> ppp qqq <i>rrr " +
                        "<b>sss</b> tttt uuuu</i> " +
                        "vvvv",
                FONT_FAMILY_COURIER,
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
                PdfFont.BuiltInFont(BuiltInFontName.COURIER),
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