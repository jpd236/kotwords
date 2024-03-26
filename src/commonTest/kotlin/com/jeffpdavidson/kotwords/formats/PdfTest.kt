package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.IgnoreNative
import com.jeffpdavidson.kotwords.formats.Pdf.splitTextToLines
import com.jeffpdavidson.kotwords.readBinaryResource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@IgnoreNative  // Depends on PDF support
class PdfTest {
    @Test
    fun asPdf() = runTest {
        ImageComparator.assertPdfEquals(
            readBinaryResource(ImageComparator::class, "pdf/test.pdf"),
            AcrossLite(readBinaryResource(PdfTest::class, "puz/test-simple.puz"))
                .asCrossword().asPdf(blackSquareLightnessAdjustment = 0.75f)
        )
    }

    @Test
    fun asPdf_customFonts() = runTest {
        ImageComparator.assertPdfEquals(
            readBinaryResource(ImageComparator::class, "pdf/test-customFonts.pdf"),
            AcrossLite(readBinaryResource(PdfTest::class, "puz/test.puz"))
                .asCrossword().asPdf(blackSquareLightnessAdjustment = 0.75f, fontFamily = getNotoSerifFontFamily())
        )
    }

    @Test
    fun asPdf_bgImages() = runTest {
        ImageComparator.assertPdfEquals(
            readBinaryResource(ImageComparator::class, "pdf/test-bgimage.pdf"),
            JpzFile(readBinaryResource(PdfTest::class, "jpz/test-bgimage.jpz"))
                .asPuzzle().asPdf(blackSquareLightnessAdjustment = 0.75f, fontFamily = getNotoSerifFontFamily())
        )
    }

    @Test
    fun asPdf_html() = runTest {
        ImageComparator.assertPdfEquals(
            readBinaryResource(ImageComparator::class, "pdf/test-html.pdf"),
            JpzFile(readBinaryResource(PdfTest::class, "jpz/test-html.jpz"))
                .asPuzzle().asPdf(blackSquareLightnessAdjustment = 0.75f, fontFamily = getNotoSerifFontFamily())
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
                10f,
                100f,
                isHtml = false,
            )
        )
    }

    @Test
    fun splitTextToLines_withHtmlFormatting() = withDocument { document ->
        fun format(fontName: BuiltInFontName, script: Pdf.Script): Pdf.Format =
            Pdf.Format(PdfFont.BuiltInFont(fontName), script)
        assertEquals(
            listOf(
                Pdf.RichTextElement.Text("a "),
                Pdf.RichTextElement.SetFormat(format(BuiltInFontName.COURIER_BOLD, Pdf.Script.REGULAR)),
                Pdf.RichTextElement.Text("b c"),
                Pdf.RichTextElement.SetFormat(format(BuiltInFontName.COURIER, Pdf.Script.REGULAR)),
                Pdf.RichTextElement.Text(" d e "),
                Pdf.RichTextElement.SetFormat(format(BuiltInFontName.COURIER_ITALIC, Pdf.Script.REGULAR)),
                Pdf.RichTextElement.Text("f g"),
                Pdf.RichTextElement.SetFormat(format(BuiltInFontName.COURIER, Pdf.Script.REGULAR)),
                Pdf.RichTextElement.Text(" h"),
                Pdf.RichTextElement.NewLine,

                Pdf.RichTextElement.SetFormat(format(BuiltInFontName.COURIER_BOLD_ITALIC, Pdf.Script.REGULAR)),
                Pdf.RichTextElement.Text("i"),
                Pdf.RichTextElement.SetFormat(format(BuiltInFontName.COURIER, Pdf.Script.REGULAR)),
                Pdf.RichTextElement.Text(" jj kk ll mm "),
                Pdf.RichTextElement.SetFormat(format(BuiltInFontName.COURIER_BOLD, Pdf.Script.REGULAR)),
                Pdf.RichTextElement.Text("nn"),
                Pdf.RichTextElement.NewLine,

                Pdf.RichTextElement.Text("oo"),
                Pdf.RichTextElement.SetFormat(format(BuiltInFontName.COURIER, Pdf.Script.REGULAR)),
                Pdf.RichTextElement.Text(" ppp qqq "),
                Pdf.RichTextElement.SetFormat(format(BuiltInFontName.COURIER_ITALIC, Pdf.Script.REGULAR)),
                Pdf.RichTextElement.Text("rr"),
                Pdf.RichTextElement.SetFormat(format(BuiltInFontName.COURIER_ITALIC, Pdf.Script.SUPERSCRIPT)),
                Pdf.RichTextElement.Text("r"),
                Pdf.RichTextElement.NewLine,

                Pdf.RichTextElement.SetFormat(format(BuiltInFontName.COURIER_BOLD_ITALIC, Pdf.Script.REGULAR)),
                Pdf.RichTextElement.Text("sss"),
                Pdf.RichTextElement.SetFormat(format(BuiltInFontName.COURIER_ITALIC, Pdf.Script.REGULAR)),
                Pdf.RichTextElement.Text(" tttt"),
                Pdf.RichTextElement.SetFormat(format(BuiltInFontName.COURIER_ITALIC, Pdf.Script.SUBSCRIPT)),
                Pdf.RichTextElement.Text("uuuu"),
                Pdf.RichTextElement.NewLine,

                Pdf.RichTextElement.SetFormat(format(BuiltInFontName.COURIER, Pdf.Script.REGULAR)),
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
                10f,
                100f,
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
                10f,
                100f,
                isHtml = false,
            )
        )
    }

    private fun withDocument(fn: suspend (PdfDocument) -> Unit) = runTest {
        val document = PdfDocument.create()
        try {
            fn(document)
        } finally {
            document.toByteArray()
        }
    }
}