package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.readBinaryResource

suspend fun getNotoSerifFontFamily() = PdfFontFamily(
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