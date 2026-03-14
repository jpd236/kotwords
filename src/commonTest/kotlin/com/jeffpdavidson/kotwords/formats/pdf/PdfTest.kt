package com.jeffpdavidson.kotwords.formats.pdf

import com.jeffpdavidson.kotwords.formats.AcrossLite
import com.jeffpdavidson.kotwords.formats.JpzFile
import com.jeffpdavidson.kotwords.formats.pdf.Pdf.splitTextToLines
import com.jeffpdavidson.kotwords.readBinaryResource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class PdfTest {
    @Test
    fun asPdf() = runTest(timeout = 60.seconds) {
        assertContentEquals(
            readBinaryResource(PdfTest::class, "pdf/test.pdf"),
            AcrossLite(readBinaryResource(PdfTest::class, "puz/test-simple.puz"))
                .asCrossword().asPdf(blackSquareLightnessAdjustment = 0.75)
        )
    }

    @Test
    fun asPdf_customFonts() = runTest(timeout = 60.seconds) {
        assertContentEquals(
            readBinaryResource(PdfTest::class, "pdf/test-customFonts.pdf"),
            AcrossLite(readBinaryResource(PdfTest::class, "puz/test.puz"))
                .asCrossword().asPdf(blackSquareLightnessAdjustment = 0.75, fontFamily = getNotoSerifFontFamily())
        )
    }

    @Test
    fun asPdf_customFonts_sansSerif() = runTest(timeout = 60.seconds) {
        assertContentEquals(
            readBinaryResource(PdfTest::class, "pdf/test-customFonts-sansSerif.pdf"),
            AcrossLite(readBinaryResource(PdfTest::class, "puz/test.puz"))
                .asCrossword().asPdf(blackSquareLightnessAdjustment = 0.75, fontFamily = getNotoSansFontFamily())
        )
    }

    @Test
    fun asPdf_bgImages() = runTest(timeout = 60.seconds) {
        assertContentEquals(
            readBinaryResource(PdfTest::class, "pdf/test-bgimage.pdf"),
            JpzFile(readBinaryResource(PdfTest::class, "jpz/test-bgimage.jpz"))
                .asPuzzle().asPdf(blackSquareLightnessAdjustment = 0.75, fontFamily = getNotoSerifFontFamily())
        )
    }

    @Test
    fun asPdf_html() = runTest(timeout = 60.seconds) {
        assertContentEquals(
            readBinaryResource(PdfTest::class, "pdf/test-html.pdf"),
            JpzFile(readBinaryResource(PdfTest::class, "jpz/test-html.jpz"))
                .asPuzzle().asPdf(blackSquareLightnessAdjustment = 0.75, fontFamily = getNotoSerifFontFamily())
        )
    }

    // Note for splitTextToLines tests: a 100 pt line fits 16 10pt Courier characters.

    @Test
    fun splitTextToLines_standardText() = withDocument { document ->
        assertEquals(
            listOf(
                Pdf.RichTextElement.Text("a b c d e f g h"),
                Pdf.RichTextElement.NewLine,
                Pdf.RichTextElement.Text("i jj kk ll mm nn"),
                Pdf.RichTextElement.NewLine,
                Pdf.RichTextElement.Text("oo ppp qqq rrr"),
                Pdf.RichTextElement.NewLine,
                Pdf.RichTextElement.Text("sss tttt uuuu"),
                Pdf.RichTextElement.NewLine,
                Pdf.RichTextElement.Text("vvvv"),
                Pdf.RichTextElement.NewLine,
            ),
            splitTextToLines(
                document,
                "a b c d e f g h i jj kk ll mm nn oo ppp qqq rrr sss tttt uuuu vvvv",
                FONT_FAMILY_COURIER,
                10.0,
                100.0,
                isHtml = false,
            )
        )
    }

    @Test
    fun splitTextToLines_withHtmlFormatting() = withDocument { document ->
        fun format(style: Pdf.Style, script: Pdf.Script): Pdf.Format = Pdf.Format(style, script)
        assertEquals(
            listOf(
                Pdf.RichTextElement.Text("a "),
                Pdf.RichTextElement.SetFormat(format(Pdf.Style.BOLD, Pdf.Script.REGULAR)),
                Pdf.RichTextElement.Text("b c"),
                Pdf.RichTextElement.SetFormat(format(Pdf.Style.REGULAR, Pdf.Script.REGULAR)),
                Pdf.RichTextElement.Text(" d e "),
                Pdf.RichTextElement.SetFormat(format(Pdf.Style.ITALIC, Pdf.Script.REGULAR)),
                Pdf.RichTextElement.Text("f g"),
                Pdf.RichTextElement.SetFormat(format(Pdf.Style.REGULAR, Pdf.Script.REGULAR)),
                Pdf.RichTextElement.Text(" h"),
                Pdf.RichTextElement.NewLine,

                Pdf.RichTextElement.SetFormat(format(Pdf.Style.BOLD_ITALIC, Pdf.Script.REGULAR)),
                Pdf.RichTextElement.Text("i"),
                Pdf.RichTextElement.SetFormat(format(Pdf.Style.REGULAR, Pdf.Script.REGULAR)),
                Pdf.RichTextElement.Text(" jj kk ll mm "),
                Pdf.RichTextElement.SetFormat(format(Pdf.Style.BOLD, Pdf.Script.REGULAR)),
                Pdf.RichTextElement.Text("nn"),
                Pdf.RichTextElement.NewLine,

                Pdf.RichTextElement.Text("oo"),
                Pdf.RichTextElement.SetFormat(format(Pdf.Style.REGULAR, Pdf.Script.REGULAR)),
                Pdf.RichTextElement.Text(" ppp qqq "),
                Pdf.RichTextElement.SetFormat(format(Pdf.Style.ITALIC, Pdf.Script.REGULAR)),
                Pdf.RichTextElement.Text("rr"),
                Pdf.RichTextElement.SetFormat(format(Pdf.Style.ITALIC, Pdf.Script.SUPERSCRIPT)),
                Pdf.RichTextElement.Text("r"),
                Pdf.RichTextElement.NewLine,

                Pdf.RichTextElement.SetFormat(format(Pdf.Style.BOLD_ITALIC, Pdf.Script.REGULAR)),
                Pdf.RichTextElement.Text("sss"),
                Pdf.RichTextElement.SetFormat(format(Pdf.Style.ITALIC, Pdf.Script.REGULAR)),
                Pdf.RichTextElement.Text(" tttt"),
                Pdf.RichTextElement.SetFormat(format(Pdf.Style.ITALIC, Pdf.Script.SUBSCRIPT)),
                Pdf.RichTextElement.Text("uuuu"),
                Pdf.RichTextElement.NewLine,

                Pdf.RichTextElement.SetFormat(format(Pdf.Style.REGULAR, Pdf.Script.REGULAR)),
                Pdf.RichTextElement.Text("vvvvv"),
                Pdf.RichTextElement.NewLine,
            ),
            splitTextToLines(
                document,
                "a <b>b c</b> d e <i>f g</i> h " +
                        "<b><i>i</i></b> jj kk ll mm <b>nn " +
                        "oo</b> ppp qqq <i>rr<sup>r</sup> " +
                        "<b>sss</b> tttt<sub>uuuu</sub></i> " +
                        "vvvvv",
                FONT_FAMILY_COURIER,
                10.0,
                100.0,
                isHtml = true,
            )
        )
    }

    @Test
    fun splitTextToLines_longWords() = withDocument { document ->
        assertEquals(
            listOf(
                Pdf.RichTextElement.Text("1234567890123456"),
                Pdf.RichTextElement.NewLine,
                Pdf.RichTextElement.Text("7890 12345678901"),
                Pdf.RichTextElement.NewLine,
                Pdf.RichTextElement.Text("234567890 123456"),
                Pdf.RichTextElement.NewLine,
                Pdf.RichTextElement.Text("78901234567890"),
                Pdf.RichTextElement.NewLine,
            ),
            splitTextToLines(
                document,
                "12345678901234567890 12345678901234567890 12345678901234567890",
                FONT_FAMILY_COURIER,
                10.0,
                100.0,
                isHtml = false,
            )
        )
    }

    private fun withDocument(fn: suspend (PdfDocument) -> Unit) = runTest {
        val document = PdfDocument()
        try {
            fn(document)
        } finally {
            document.toByteArray()
        }
    }
}