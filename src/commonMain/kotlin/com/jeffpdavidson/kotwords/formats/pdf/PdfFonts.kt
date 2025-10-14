package com.jeffpdavidson.kotwords.formats.pdf

/** Identifier for a PDF font. */
sealed interface PdfFontId {
    /** Identifier for a built-in font. These fonts are supported by all PDF readers without embedding them. */
    enum class BuiltInFontId(internal val fontName: String) : PdfFontId {
        COURIER("Courier"),
        COURIER_BOLD("Courier-Bold"),
        COURIER_ITALIC("Courier-Oblique"),
        COURIER_BOLD_ITALIC("Courier-BoldOblique"),
        TIMES_ROMAN("Times-Roman"),
        TIMES_BOLD("Times-Bold"),
        TIMES_ITALIC("Times-Italic"),
        TIMES_BOLD_ITALIC("Times-BoldItalic"),
    }

    /** Identifier for a TTF font. The font data must be provided as a [PdfFont.TtfFont]. */
    data class TtfFontId(val fontName: String) : PdfFontId
}

/** A full PDF font containing all information needed to render it. */
sealed class PdfFont(open val id: PdfFontId) {
    /** A built-in font. */
    class BuiltInFont(override val id: PdfFontId.BuiltInFontId) : PdfFont(id)

    /**
     * A TTF font.
     * @param id Unique identifier for the font. Should be distinct for each distinct TTF file.
     * @param loadFn Function to load and return the font data.
     */
    class TtfFont(override val id: PdfFontId.TtfFontId, val loadFn: suspend () -> ByteArray) : PdfFont(id)
}

/**
 * A family of fonts with different style.
 *
 * This is the base unit of a font that can be used when generating PDFs. In addition to a base font, we expect bold,
 * italic, and bold + italic versions of the font in order to support those styles as they may appear in puzzle text.
 */
class PdfFontFamily(
    val baseFont: PdfFont,
    val boldFont: PdfFont,
    val italicFont: PdfFont,
    val boldItalicFont: PdfFont
)

/** [PdfFontFamily] for the built-in Courier fonts. */
val FONT_FAMILY_COURIER = PdfFontFamily(
    PdfFont.BuiltInFont(PdfFontId.BuiltInFontId.COURIER),
    PdfFont.BuiltInFont(PdfFontId.BuiltInFontId.COURIER_BOLD),
    PdfFont.BuiltInFont(PdfFontId.BuiltInFontId.COURIER_ITALIC),
    PdfFont.BuiltInFont(PdfFontId.BuiltInFontId.COURIER_BOLD_ITALIC),
)

/** [PdfFontFamily] for the built-in Times Roman fonts. */
val FONT_FAMILY_TIMES_ROMAN = PdfFontFamily(
    PdfFont.BuiltInFont(PdfFontId.BuiltInFontId.TIMES_ROMAN),
    PdfFont.BuiltInFont(PdfFontId.BuiltInFontId.TIMES_BOLD),
    PdfFont.BuiltInFont(PdfFontId.BuiltInFontId.TIMES_ITALIC),
    PdfFont.BuiltInFont(PdfFontId.BuiltInFontId.TIMES_BOLD_ITALIC),
)