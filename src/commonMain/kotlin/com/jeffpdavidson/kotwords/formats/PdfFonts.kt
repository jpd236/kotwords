package com.jeffpdavidson.kotwords.formats

enum class BuiltInFontName {
    COURIER,
    COURIER_BOLD,
    COURIER_ITALIC,
    COURIER_BOLD_ITALIC,
    TIMES_ROMAN,
    TIMES_BOLD,
    TIMES_ITALIC,
    TIMES_BOLD_ITALIC,
}

sealed class PdfFont {
    data class BuiltInFont(val fontName: BuiltInFontName) : PdfFont()
    data class TtfFont(val fontName: String, val fontStyle: String, val fontData: ByteArray) : PdfFont()
}

data class PdfFontFamily(
    val baseFont: PdfFont,
    val boldFont: PdfFont,
    val italicFont: PdfFont,
    val boldItalicFont: PdfFont
)

val FONT_FAMILY_COURIER = PdfFontFamily(
    PdfFont.BuiltInFont(BuiltInFontName.COURIER),
    PdfFont.BuiltInFont(BuiltInFontName.COURIER_BOLD),
    PdfFont.BuiltInFont(BuiltInFontName.COURIER_ITALIC),
    PdfFont.BuiltInFont(BuiltInFontName.COURIER_BOLD_ITALIC),
)
val FONT_FAMILY_TIMES_ROMAN = PdfFontFamily(
    PdfFont.BuiltInFont(BuiltInFontName.TIMES_ROMAN),
    PdfFont.BuiltInFont(BuiltInFontName.TIMES_BOLD),
    PdfFont.BuiltInFont(BuiltInFontName.TIMES_ITALIC),
    PdfFont.BuiltInFont(BuiltInFontName.TIMES_BOLD_ITALIC),
)