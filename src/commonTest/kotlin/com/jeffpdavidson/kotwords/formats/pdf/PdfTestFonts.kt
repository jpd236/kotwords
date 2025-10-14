package com.jeffpdavidson.kotwords.formats.pdf

import com.jeffpdavidson.kotwords.readBinaryResource

suspend fun getNotoSerifFontFamily() = PdfFontFamily(
    PdfFont.TtfFont(PdfFontId.TtfFontId("NotoSerif-Regular")) {
        readBinaryResource(PdfTest::class, "pdf/fonts/NotoSerif-Regular.ttf")
    },
    PdfFont.TtfFont(PdfFontId.TtfFontId("NotoSerif-Bold")) {
        readBinaryResource(PdfTest::class, "pdf/fonts/NotoSerif-Bold.ttf")
    },
    PdfFont.TtfFont(PdfFontId.TtfFontId("NotoSerif-Italic")) {
        readBinaryResource(PdfTest::class, "pdf/fonts/NotoSerif-Italic.ttf")
    },
    PdfFont.TtfFont(PdfFontId.TtfFontId("NotoSerif-BoldItalic")) {
        readBinaryResource(PdfTest::class, "pdf/fonts/NotoSerif-BoldItalic.ttf")
    },
)