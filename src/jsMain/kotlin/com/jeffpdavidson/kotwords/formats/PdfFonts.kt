package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.pdf.PdfFont
import com.jeffpdavidson.kotwords.formats.pdf.PdfFontFamily
import com.jeffpdavidson.kotwords.formats.pdf.PdfFontId
import com.jeffpdavidson.kotwords.js.Http

internal object PdfFonts {
    val NOTO_FONT_FAMILY: PdfFontFamily = PdfFontFamily(
        baseFont = PdfFont.TtfFont(PdfFontId.TtfFontId("NotoSerif-Regular")) {
            Http.getBinary("fonts/NotoSerif-Regular.ttf")
        },
        boldFont = PdfFont.TtfFont(PdfFontId.TtfFontId("NotoSerif-Bold")) {
            Http.getBinary("fonts/NotoSerif-Bold.ttf")
        },
        italicFont = PdfFont.TtfFont(PdfFontId.TtfFontId("NotoSerif-Italic")) {
            Http.getBinary("fonts/NotoSerif-Italic.ttf")
        },
        boldItalicFont = PdfFont.TtfFont(PdfFontId.TtfFontId("NotoSerif-BoldItalic")) {
            Http.getBinary("fonts/NotoSerif-BoldItalic.ttf")
        }
    )
}