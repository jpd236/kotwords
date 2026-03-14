package com.jeffpdavidson.kotwords.formats.pdf

import com.jeffpdavidson.kotwords.readBinaryResource

fun getNotoSerifFontFamily() = getFontFamily("NotoSerif")

fun getNotoSansFontFamily() = getFontFamily("NotoSans")

private fun getFontFamily(name: String): PdfFontFamily = PdfFontFamily(
    PdfFont.TtfFont(PdfFontId.TtfFontId("$name-Regular")) {
        readBinaryResource(PdfTest::class, "pdf/fonts/$name-Regular.ttf")
    },
    PdfFont.TtfFont(PdfFontId.TtfFontId("$name-Bold")) {
        readBinaryResource(PdfTest::class, "pdf/fonts/$name-Bold.ttf")
    },
    PdfFont.TtfFont(PdfFontId.TtfFontId("$name-Italic")) {
        readBinaryResource(PdfTest::class, "pdf/fonts/$name-Italic.ttf")
    },
    PdfFont.TtfFont(PdfFontId.TtfFontId("$name-BoldItalic")) {
        readBinaryResource(PdfTest::class, "pdf/fonts/$name-BoldItalic.ttf")
    },
)