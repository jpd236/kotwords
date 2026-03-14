package com.jeffpdavidson.kotwords.formats.pdf

import com.jeffpdavidson.kotwords.formats.Encodings
import com.jeffpdavidson.kotwords.model.Puzzle
import korlibs.image.core.CoreImage
import korlibs.image.core.CoreImage32Color
import korlibs.image.core.decodeBytes
import korlibs.image.core.info
import okio.Buffer
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * A document canvas which can be rendered as a PDF.
 *
 * Assumes one-page, letter-sized documents. All units are in points. Coordinates are measured as distance from the
 * bottom-left corner of the document.
 */
internal class PdfDocument {
    /** Width of the page, in points. */
    public val width: Double = 612.0

    /** Height of the page, in points. */
    public val height: Double = 792.0

    private var nextObjectId = 1
    private fun allocateId() = nextObjectId++

    private val catalogId = allocateId()
    private val pagesId = allocateId()
    private val pageId = allocateId()
    private val contentId = allocateId()

    private val commands = StringBuilder()

    private sealed class FontResource(open val id: PdfFontId, val resourceName: String) {
        class BuiltInFontResource(
            override val id: PdfFontId.BuiltInFontId,
            resourceName: String,
            val resourceId: Int,
        ) : FontResource(id, resourceName)

        class TtfResource(
            override val id: PdfFontId.TtfFontId,
            resourceName: String,
            val type0Id: Int,
            val cidFontId: Int,
            val descriptorId: Int,
            val fileStreamId: Int,
            val toUnicodeId: Int,
            val parsedTtf: ParsedTtf,
        ) : FontResource(id, resourceName)
    }

    private val registeredFonts = mutableMapOf<PdfFontId, FontResource>()
    private var activeFont: PdfFont? = null
    private var activeFontSize: Double? = null
    private val usedChars = mutableMapOf<PdfFontId, MutableSet<Char>>()

    sealed class ImageResource(val name: String, val objId: Int) {
        class Jpeg(name: String, objId: Int, val data: ByteArray) : ImageResource(name, objId)
        class Other(name: String, objId: Int, val sMaskId: Int, val data: ByteArray) : ImageResource(name, objId)
    }

    private val registeredImages = mutableListOf<ImageResource>()

    /** Begin a text section. */
    fun beginText() {
        commands.appendLine("BT")
    }

    /** End the current text section. */
    fun endText() {
        commands.appendLine("ET")
    }

    /** Start a new line offset by ([offsetX], [offsetY]) from the current line. */
    fun newLineAtOffset(offsetX: Double, offsetY: Double) {
        commands.appendLine("${offsetX.fmt()} ${offsetY.fmt()} Td")
    }

    /** Set the font to be used for text. */
    suspend fun setFont(font: PdfFont, size: Double) {
        activeFont = font
        activeFontSize = size
    }

    /** Get the width of the given [text] with font [font] and font size [size]. */
    suspend fun getTextWidth(text: String, font: PdfFont, size: Double): Double {
        val fontResource = loadFont(font)
        when (fontResource) {
            is FontResource.TtfResource -> {
                val metadata = fontResource.parsedTtf
                var totalUnits = 0L
                val shapedText = applyLigatures(text, metadata)
                for (char in shapedText) {
                    totalUnits += metadata.charToWidth[char] ?: 0
                }
                return (totalUnits * size) / metadata.unitsPerEm.toDouble()
            }

            is FontResource.BuiltInFontResource -> {
                val widthsMap = when (fontResource.id) {
                    PdfFontId.BuiltInFontId.TIMES_ROMAN -> BuiltInFontMetrics.TimesRomanWidths
                    PdfFontId.BuiltInFontId.TIMES_BOLD -> BuiltInFontMetrics.TimesBoldWidths
                    PdfFontId.BuiltInFontId.TIMES_ITALIC -> BuiltInFontMetrics.TimesItalicWidths
                    PdfFontId.BuiltInFontId.TIMES_BOLD_ITALIC -> BuiltInFontMetrics.TimesBoldItalicWidths
                    // All Courier fonts have a uniform width of 600 for every character.
                    PdfFontId.BuiltInFontId.COURIER,
                    PdfFontId.BuiltInFontId.COURIER_BOLD,
                    PdfFontId.BuiltInFontId.COURIER_ITALIC,
                    PdfFontId.BuiltInFontId.COURIER_BOLD_ITALIC -> return text.length * 600 * size / 1000.0
                }
                var totalUnits = 0L
                for (char in text) {
                    totalUnits += widthsMap[char] ?: 0
                }
                return (totalUnits * size) / 1000.0
            }
        }
    }

    /** Draw and stroke the given [text]. */
    suspend fun drawText(text: String) {
        val font = activeFont ?: return
        val size = activeFontSize ?: return

        val fontResource = loadFont(font)

        commands.appendLine("${fontResource.resourceName} ${size.fmt()} Tf")

        when (fontResource) {
            is FontResource.TtfResource -> {
                val metadata = fontResource.parsedTtf
                val shapedText = applyLigatures(text, metadata)
                usedChars.getOrPut(font.id) { mutableSetOf() }.addAll(shapedText.toList())
                val hexString = shapedText.map { char ->
                    (metadata.charToGlyph[char] ?: 0).toHexString(HEX_FORMAT)
                }.joinToString("")
                commands.appendLine("<$hexString> Tj")
            }

            is FontResource.BuiltInFontResource -> {
                commands.appendLine("<${Encodings.encodeCp1252(text).toHexString()}> Tj")
            }
        }
    }

    private suspend fun loadFont(font: PdfFont): FontResource {
        return registeredFonts.getOrPut(font.id) {
            val resourceName = "/F${registeredFonts.size + 1}"
            when (font) {
                is PdfFont.TtfFont -> {
                    FontResource.TtfResource(
                        id = font.id,
                        resourceName = resourceName,
                        type0Id = allocateId(),
                        cidFontId = allocateId(),
                        descriptorId = allocateId(),
                        fileStreamId = allocateId(),
                        toUnicodeId = allocateId(),
                        parsedTtf = ParsedTtf(font.loadFn())
                    )
                }

                is PdfFont.BuiltInFont -> {
                    FontResource.BuiltInFontResource(font.id, resourceName, allocateId())
                }
            }
        }
    }

    /** Replace the characters composing common ligatures with the corresponding Unicode ligature characters. */
    private fun applyLigatures(text: String, metadata: ParsedTtf): String {
        var result = text
        // We iterate from longest (ffi) to shortest (ff) to ensure "ffi" doesn't get caught by the "ff" rule first.
        LIGATURE_MAP.entries.sortedByDescending { it.key.length }.forEach { (sequence, ligature) ->
            // Only apply the ligature if the font actually supports that glyph.
            if (metadata.charToGlyph.containsKey(ligature)) {
                result = result.replace(sequence, ligature.toString())
            }
        }
        return result
    }

    /** Set the line width. */
    fun setLineWidth(width: Double) {
        commands.appendLine("${width.fmt()} w")
    }

    /** Set the stroke color. */
    fun setStrokeColor(r: Double, g: Double, b: Double) {
        commands.appendLine("${r.fmt()} ${g.fmt()} ${b.fmt()} RG")
    }

    /** Set the fill color. */
    fun setFillColor(r: Double, g: Double, b: Double) {
        commands.appendLine("${r.fmt()} ${g.fmt()} ${b.fmt()} rg")
    }

    /** Draw a line from ([x1], [y1]) to ([x2], [y2]). */
    fun drawLine(x1: Double, y1: Double, x2: Double, y2: Double) {
        commands.appendLine("${x1.fmt()} ${y1.fmt()} m ${x2.fmt()} ${y2.fmt()} l S")
    }

    /** Draw a rectangle from bottom-left coordinates ([x], [y]). */
    fun drawRect(x: Double, y: Double, width: Double, height: Double, stroke: Boolean = false, fill: Boolean = false) {
        val op = getPaintOp(stroke, fill) ?: return
        commands.appendLine("${x.fmt()} ${y.fmt()} ${width.fmt()} ${height.fmt()} re $op")
    }

    /** Draw a circle of radius [radius] from bottom-left coordinates ([x], [y]). */
    fun drawCircle(x: Double, y: Double, radius: Double, stroke: Boolean = false, fill: Boolean = false) {
        val op = getPaintOp(stroke, fill) ?: return

        // PDFs don't support circles directly. We approximate them using curves.
        val cx = x + radius
        val cy = y + radius
        val m = radius * 0.552284749831

        commands.appendLine("${cx.fmt()} ${(cy + radius).fmt()} m")
        commands.appendLine(
            "${(cx + m).fmt()} ${(cy + radius).fmt()} " +
                    "${(cx + radius).fmt()} ${(cy + m).fmt()} " +
                    "${(cx + radius).fmt()} ${cy.fmt()} c"
        )
        commands.appendLine(
            "${(cx + radius).fmt()} ${(cy - m).fmt()} " +
                    "${(cx + m).fmt()} ${(cy - radius).fmt()} " +
                    "${cx.fmt()} ${(cy - radius).fmt()} c"
        )
        commands.appendLine(
            "${(cx - m).fmt()} ${(cy - radius).fmt()} " +
                    "${(cx - radius).fmt()} ${(cy - m).fmt()} " +
                    "${(cx - radius).fmt()} ${cy.fmt()} c"
        )
        commands.appendLine(
            "${(cx - radius).fmt()} ${(cy + m).fmt()} " +
                    "${(cx - m).fmt()} ${(cy + radius).fmt()} " +
                    "${cx.fmt()} ${(cy + radius).fmt()} c"
        )
        commands.appendLine("h $op")
    }

    /** Draw the given image from bottom-left coordinates ([x], [y]). */
    suspend fun drawImage(x: Double, y: Double, width: Double, height: Double, image: Puzzle.Image.Data) {
        val name = "/Im${registeredImages.size + 1}"
        registeredImages.add(
            if (image.format == Puzzle.ImageFormat.JPG) {
                ImageResource.Jpeg(name = name, objId = allocateId(), data = image.bytes.toByteArray())
            } else {
                ImageResource.Other(
                    name = name, objId = allocateId(), sMaskId = allocateId(), data = image.bytes.toByteArray()
                )
            }
        )
        commands.appendLine("q ${width.fmt()} 0 0 ${height.fmt()} ${x.fmt()} ${y.fmt()} cm $name Do Q")
    }

    private fun getPaintOp(stroke: Boolean, fill: Boolean): String? = when {
        stroke && fill -> "B"
        fill -> "f"
        stroke -> "S"
        else -> null
    }

    /** Generate the CMap that allows PDF readers to map Glyph IDs back to Unicode. */
    private suspend fun generateToUnicodeCMap(ttfResource: FontResource.TtfResource): String {
        val usedCharsForFont = usedChars[ttfResource.id] ?: setOf()
        val glyphToChar =
            ttfResource.parsedTtf.charToGlyph.entries.filter { it.key in usedCharsForFont && it.value != 0 }
        val glyphMap = glyphToChar.joinToString("\n") {
            "<${it.value.toHexString(HEX_FORMAT)}> <${it.key.code.toHexString(HEX_FORMAT)}>"
        }
        return """
            /CIDInit /ProcSet findresource begin
            12 dict begin
            begincmap
            /CIDSystemInfo << /Registry (Adobe) /Ordering (UCS) /Supplement 0 >> def
            /CMapName /Adobe-Identity-UCS def
            /CMapType 2 def
            1 begincodespacerange <0000> <FFFF> endcodespacerange
            ${glyphToChar.size} beginbfchar
            $glyphMap
            endbfchar
            endcmap
            CMapName currentdict /CMap defineresource pop
            end end
        """.trimIndent()
    }

    /** Return this document as a PDF [ByteArray]. */
    suspend fun toByteArray(): ByteArray {
        val mainBuffer = Buffer()
        val xrefOffsets = mutableMapOf<Int, Long>()

        fun writeObj(id: Int, content: String) {
            xrefOffsets[id] = mainBuffer.size
            mainBuffer.writeUtf8("$id 0 obj\n$content\nendobj\n")
        }

        fun writeStreamObj(
            id: Int,
            data: ByteArray,
            filter: String,
            additionalTags: String? = null,
        ) {
            xrefOffsets[id] = mainBuffer.size
            mainBuffer.writeUtf8("$id 0 obj\n<< ")
            additionalTags?.let { mainBuffer.writeUtf8("$additionalTags ") }
            mainBuffer.writeUtf8("/Length ${data.size} /Filter /$filter ")
            mainBuffer.writeUtf8(">>\nstream\n")
            mainBuffer.write(data)
            mainBuffer.writeUtf8("\nendstream\nendobj\n")
        }

        fun writeFlateStreamObj(
            id: Int, data: ByteArray, additionalTags: String? = null, includeUncompressedLength: Boolean = false
        ) {
            val compressedData = Encodings.deflate(data)
            val allTags =
                listOfNotNull(additionalTags, if (includeUncompressedLength) "/Length1 ${data.size}" else null)
                    .joinToString(" ").ifEmpty { null }
            writeStreamObj(id, compressedData, filter = "FlateDecode", additionalTags = allTags)
        }

        fun writeImageObj(
            id: Int,
            data: ByteArray,
            width: Int,
            height: Int,
            colorSpace: String,
            sMaskId: Int? = null,
            filter: String? = null
        ) {
            val additionalTags =
                StringBuilder(
                    "/Type /XObject /Subtype /Image " +
                            "/Width $width /Height $height /ColorSpace /$colorSpace /BitsPerComponent 8"
                )
            if (sMaskId != null) {
                additionalTags.append(" /SMask $sMaskId 0 R")
            }
            if (filter != null) {
                writeStreamObj(id, data, filter, additionalTags.toString())
            } else {
                writeFlateStreamObj(id, data, additionalTags.toString())
            }
        }

        mainBuffer.writeUtf8("%PDF-1.4\n%\u00E2\u00E3\u00CF\u00D3\n")
        writeObj(catalogId, "<< /Type /Catalog /Pages $pagesId 0 R >>")
        writeObj(pagesId, "<< /Type /Pages /Kids [$pageId 0 R] /Count 1 >>")

        val fontEntries = registeredFonts.values.joinToString(" ") { resource ->
            val id = when (resource) {
                is FontResource.BuiltInFontResource -> resource.resourceId
                is FontResource.TtfResource -> resource.type0Id
            }
            "${resource.resourceName} $id 0 R"
        }
        val imageEntries = registeredImages.joinToString(" ") { "${it.name} ${it.objId} 0 R" }
        val resources = when {
            (registeredFonts.isNotEmpty() && registeredImages.isNotEmpty()) ->
                "/Resources << /Font << $fontEntries >> /XObject << $imageEntries >> "

            registeredFonts.isNotEmpty() -> "/Resources << /Font << $fontEntries >> "
            registeredImages.isNotEmpty() -> "/Resources << /XObject << $imageEntries >> "
            else -> ""
        }
        writeObj(
            pageId,
            "<< /Type /Page /Parent $pagesId 0 R /MediaBox [0 0 ${width.fmt()} ${height.fmt()}] " +
                    "/Contents $contentId 0 R $resources>> >>"
        )

        writeFlateStreamObj(contentId, commands.trim().toString().encodeToByteArray())

        registeredFonts.forEach { (font, resource) ->
            when (resource) {
                is FontResource.BuiltInFontResource -> {
                    writeObj(
                        resource.resourceId,
                        "<< /Type /Font /Subtype /Type1 /BaseFont /${resource.id.fontName} " +
                                "/Encoding /WinAnsiEncoding >>"
                    )
                }

                is FontResource.TtfResource -> {
                    val meta = resource.parsedTtf
                    val fontName = resource.id.fontName

                    writeObj(
                        resource.type0Id,
                        "<< /Type /Font /Subtype /Type0 /BaseFont /$fontName " +
                                "/Encoding /Identity-H /DescendantFonts [${resource.cidFontId} 0 R] " +
                                "/ToUnicode ${resource.toUnicodeId} 0 R >>"
                    )

                    val charsInDoc = usedChars[font] ?: emptySet()
                    val charToGlyph = meta.charToGlyph.filter { charsInDoc.contains(it.key) }

                    val widths =
                        charToGlyph.entries.sortedBy { (_, glyphId) -> glyphId }.joinToString(" ") { (char, glyphId) ->
                            "$glyphId [${meta.charToWidth[char] ?: 0}]"
                        }
                    writeObj(
                        resource.cidFontId,
                        "<< /Type /Font /Subtype /CIDFontType2 /BaseFont /$fontName /CIDSystemInfo " +
                                "<< /Registry (Adobe) /Ordering (Identity) /Supplement 0 >> " +
                                "/FontDescriptor ${resource.descriptorId} 0 R /CIDToGIDMap /Identity /W [ $widths ] >>"
                    )

                    writeObj(
                        resource.descriptorId,
                        "<< /Type /FontDescriptor /FontName /$fontName /Flags ${resource.parsedTtf.flags} " +
                                "/ItalicAngle ${resource.parsedTtf.italicAngle} /Ascent ${resource.parsedTtf.ascent} " +
                                "/Descent ${resource.parsedTtf.descent} /CapHeight ${resource.parsedTtf.capHeight} " +
                                "/StemV 0 /FontFile2 ${resource.fileStreamId} 0 R >>"
                    )

                    writeFlateStreamObj(resource.toUnicodeId, generateToUnicodeCMap(resource).encodeToByteArray())

                    val subsettedFontData =
                        resource.parsedTtf.createSubsettedFont(usedChars[font] ?: emptySet())
                    writeFlateStreamObj(resource.fileStreamId, subsettedFontData, includeUncompressedLength = true)
                }
            }
        }

        registeredImages.forEach { res ->
            when (res) {
                is ImageResource.Jpeg -> {
                    // PDFs natively support JPGs, so we can include the data directly with the appropriate filter.
                    val coreImageInfo = CoreImage.info(res.data)
                    writeImageObj(
                        id = res.objId,
                        data = res.data,
                        width = coreImageInfo.width,
                        height = coreImageInfo.height,
                        colorSpace = "DeviceRGB",
                        filter = "DCTDecode"
                    )
                }

                is ImageResource.Other -> {
                    // Decode other image formats into RGB + Alpha bitmaps.
                    val image = CoreImage.decodeBytes(res.data).to32()
                    val pixelCount = image.height * image.width
                    val rgbData = ByteArray(pixelCount * 3)
                    val alphaData = ByteArray(pixelCount)

                    for (i in 0 until pixelCount) {
                        val color = CoreImage32Color(image.data[i])
                        rgbData[i * 3] = color.red.toByte()
                        rgbData[i * 3 + 1] = color.green.toByte()
                        rgbData[i * 3 + 2] = color.blue.toByte()
                        alphaData[i] = color.alpha.toByte()
                    }

                    writeImageObj(
                        id = res.objId,
                        data = rgbData,
                        width = image.width,
                        height = image.height,
                        colorSpace = "DeviceRGB",
                        sMaskId = res.sMaskId
                    )
                    writeImageObj(
                        id = res.sMaskId,
                        data = alphaData,
                        width = image.width,
                        height = image.height,
                        colorSpace = "DeviceGray"
                    )
                }
            }
        }

        val startXref = mainBuffer.size
        mainBuffer.writeUtf8("xref\n")
        mainBuffer.writeUtf8("0 $nextObjectId\n")
        mainBuffer.writeUtf8("0000000000 65535 f\r\n")
        for (id in 1 until nextObjectId) {
            val offset = xrefOffsets[id] ?: 0L
            val line = "${offset.toString().padStart(10, '0')} 00000 n\r\n"
            mainBuffer.writeUtf8(line)
        }

        mainBuffer.writeUtf8("trailer\n<< /Size $nextObjectId /Root $catalogId 0 R >>\n")
        mainBuffer.writeUtf8("startxref\n$startXref\n%%EOF\n")

        return mainBuffer.readByteArray()
    }

    /** Formats a floating-point value for the PDF content stream. */
    fun Double.fmt(): String {
        if (this.isNaN()) return "0"
        if (this.isInfinite()) return if (this > 0) "32767" else "-32767"

        val precision = 4
        val factor = 10.0.pow(precision)
        val rounded = (this * factor).roundToLong()
        val absValue = abs(rounded)
        val intPart = absValue / factor.toLong()
        val fracPart = absValue % factor.toLong()
        val sign = if (rounded < 0) "-" else ""
        var fracStr = fracPart.toString().padStart(precision, '0').trimEnd('0')

        return if (fracStr.isEmpty()) {
            "$sign$intPart"
        } else {
            "$sign$intPart.$fracStr"
        }
    }

    companion object {
        /** Standard Unicode points for common ligatures. */
        private val LIGATURE_MAP = mapOf(
            "ff" to '\uFB00',
            "fi" to '\uFB01',
            "fl" to '\uFB02',
            "ffi" to '\uFB03',
            "ffl" to '\uFB04'
        )

        private val HEX_FORMAT =
            HexFormat {
                number {
                    removeLeadingZeros = true
                    minLength = 4
                }
            }
    }
}