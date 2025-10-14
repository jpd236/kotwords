package com.jeffpdavidson.kotwords.formats.pdf

/** Represents a parsed TTF font. */
internal class ParsedTtf(val fontData: ByteArray) {
    /** The number of glyph space units per em for this font. */
    val unitsPerEm: Int

    /** Map from unicode character to the width of that character, in glyph space units. */
    val charToWidth: Map<Char, Int>

    /** Map from unicode character to the ID of the glyph representing that character. */
    val charToGlyph: Map<Char, Int>

    // Metadata about the font needed for PDF embedding.
    val italicAngle: Int
    val ascent: Int
    val descent: Int
    val capHeight: Int
    val flags: Int

    private val tableOffsets: Map<String, TableEntry>

    private data class TableEntry(val offset: Long, val length: Long)

    init {
        tableOffsets = parseTableDirectory()

        val head = tableOffsets["head"] ?: error("TTF font is missing head table")
        unitsPerEm = readUInt16(head.offset.toInt() + 18)
        val scale = 1000.0 / unitsPerEm

        val hhea = tableOffsets["hhea"] ?: error("TTF font is missing hhea table")
        ascent = (readInt16(hhea.offset.toInt() + 4) * scale).toInt()
        descent = (readInt16(hhea.offset.toInt() + 6) * scale).toInt()
        val numOfHMetrics = readUInt16(hhea.offset.toInt() + 34)

        val post = tableOffsets["post"] ?: error("TTF font is missing post table")
        italicAngle = readInt16(post.offset.toInt() + 4)
        val isFixedPitch = readUInt32(post.offset.toInt() + 12) > 0

        val os2 = tableOffsets["OS/2"] ?: error("TTF font is missing OS/2 table")
        val familyClassId = readUInt16(os2.offset.toInt() + 60) shr 8
        capHeight = (readInt16(os2.offset.toInt() + 88) * scale).toInt()

        flags = 32 or (if (isFixedPitch) 1 else 0) or (if (italicAngle != 0) 64 else 0)

        val hmtx = tableOffsets["hmtx"] ?: error("TTF font is missing hmtx table")
        val glyphWidths = IntArray(numOfHMetrics)
        for (i in 0 until numOfHMetrics) {
            glyphWidths[i] = readUInt16(hmtx.offset.toInt() + (i * 4))
        }

        val cmap = tableOffsets["cmap"] ?: error("TTF font is missing cmap table")
        charToGlyph = parseCmap(cmap.offset.toInt())
        charToWidth =
            charToGlyph
                .filterValues { glyphId -> glyphId < numOfHMetrics }.mapValues { (_, glyphId) -> glyphWidths[glyphId] }
    }

    /**
     * Create a copy of this font with glyph data for characters other than [usedChars] zeroed out.
     *
     * Zeroing out unused data is a simple way of saving storage space when the font is deflated. An alternative would
     * be to rewrite the whole font from scratch with unused characters removed; however, this would require
     * significantly more bookkeeping to update every section of the font, including offset references, to match.
     */
    fun createZeroedFont(usedChars: Set<Char>): ByteArray {
        val result = fontData.copyOf()

        val head = tableOffsets["head"] ?: return result
        val loca = tableOffsets["loca"] ?: return result
        val glyf = tableOffsets["glyf"] ?: return result

        val isLongLoca = readUInt16(head.offset.toInt() + 50) == 1

        val usedGlyphs = mutableSetOf(0)
        usedChars.forEach { char ->
            charToGlyph[char]?.let { id ->
                resolveGlyphDependencies(
                    glyphId = id,
                    locaOffset = loca.offset.toInt(),
                    glyfOffset = glyf.offset.toInt(),
                    isLongLoca = isLongLoca,
                    dependencies = usedGlyphs
                )
            }
        }

        val numGlyphs = (loca.length / (if (isLongLoca) 4 else 2)).toInt() - 1
        for (id in 0 until numGlyphs) {
            if (id in usedGlyphs) continue

            val offset = getGlyphOffset(glyphId = id, locaOffset = loca.offset.toInt(), isLongLoca = isLongLoca)
            val nextOffset = getGlyphOffset(glyphId = id + 1, locaOffset = loca.offset.toInt(), isLongLoca = isLongLoca)
            val length = nextOffset - offset

            if (length > 0) {
                val start = glyf.offset.toInt() + offset
                result.fill(0, start, start + length)
            }
        }

        return result
    }

    private fun parseTableDirectory(): Map<String, TableEntry> {
        val numTables = readUInt16(4)

        val tables = mutableMapOf<String, TableEntry>()
        var currentOffset = 12
        for (i in 0 until numTables) {
            val tag = fontData.decodeToString(currentOffset, currentOffset + 4)
            val offset = readUInt32(currentOffset + 8)
            val length = readUInt32(currentOffset + 12)
            tables[tag] = TableEntry(offset, length)
            currentOffset += 16
        }

        return tables
    }

    /**
     * Parse the cmap (character to glyph index mapping) table.
     *
     * For simplicity, only supports parsing the subtable with:
     * - platformId = Windows (3)
     * - encodingId = Unicode BMP (1)
     * - format 4
     */
    private fun parseCmap(cmapOffset: Int): Map<Char, Int> {
        val numSubtables = readUInt16(cmapOffset + 2)
        var subtableOffset = -1
        for (i in 0 until numSubtables) {
            val platformId = readUInt16(cmapOffset + 4 + (i * 8))
            val encodingId = readUInt16(cmapOffset + 4 + (i * 8) + 2)
            val offset = readUInt32(cmapOffset + 4 + (i * 8) + 4).toInt()
            if (platformId == 3 && encodingId == 1) {
                subtableOffset = cmapOffset + offset
                break
            }
        }

        if (subtableOffset == -1) throw UnsupportedOperationException("Subtable for unicode BMP not found")

        val format = readUInt16(subtableOffset)
        if (format != 4) throw UnsupportedOperationException("Unicode BMP table uses unsupported format")

        val segCount = readUInt16(subtableOffset + 6) / 2
        val endCodesOffset = subtableOffset + 14
        val startCodesOffset = endCodesOffset + 2 + (segCount * 2)
        val idDeltaOffset = startCodesOffset + (segCount * 2)
        val idRangeOffsetOffset = idDeltaOffset + (segCount * 2)

        val charToGlyph = mutableMapOf<Char, Int>()
        for (i in 0 until segCount) {
            val startCode = readUInt16(startCodesOffset + (i * 2))
            val endCode = readUInt16(endCodesOffset + (i * 2))
            val idDelta = readInt16(idDeltaOffset + (i * 2))
            val rangeOffset = readUInt16(idRangeOffsetOffset + (i * 2))

            for (code in startCode..endCode) {
                if (code == 0xFFFF) continue
                val glyphId = if (rangeOffset == 0) {
                    (code + idDelta) and 0xFFFF
                } else {
                    val glyphOffset = idRangeOffsetOffset + (i * 2) + rangeOffset + (code - startCode) * 2
                    val rawGlyphId = readUInt16(glyphOffset)
                    if (rawGlyphId != 0) (rawGlyphId + idDelta) and 0xFFFF else 0
                }
                charToGlyph[code.toChar()] = glyphId
            }
        }
        return charToGlyph
    }

    /**
     * Recursively resolve all glyphs which [glyphId] depends on.
     *
     * A TTF glyph may be composed of multiple other glyphs, so when determining which glyphs are or aren't used when
     * rendering a particular set of characters, we need to also consider glyphs that are used as part of any of those
     * characters.
     */
    private fun resolveGlyphDependencies(
        glyphId: Int,
        locaOffset: Int,
        glyfOffset: Int,
        isLongLoca: Boolean,
        dependencies: MutableSet<Int>
    ) {
        // If this glyphId is included in the output set, we've already processed it and its dependencies.
        if (glyphId in dependencies) return

        dependencies.add(glyphId)

        val offset = getGlyphOffset(glyphId, locaOffset, isLongLoca)
        val nextOffset = getGlyphOffset(glyphId + 1, locaOffset, isLongLoca)
        if (nextOffset - offset <= 0) return

        val absoluteOffset = glyfOffset + offset
        val numberOfContours = readInt16(absoluteOffset)
        if (numberOfContours < 0) {
            // This is a composite glyph - include the subglyph and any recursive dependencies.
            var cursor = absoluteOffset + 10
            var hasMore = true
            while (hasMore) {
                val flags = readUInt16(cursor)
                val subGlyphId = readUInt16(cursor + 2)
                dependencies.add(subGlyphId)

                // Recurse to find nested composites (e.g., a composite of composites)
                resolveGlyphDependencies(subGlyphId, locaOffset, glyfOffset, isLongLoca, dependencies)

                // Determine how many bytes to skip to reach the next component, if any.

                // ARG_1_AND_2_ARE_WORDS affects the length of the first two arguments.
                val argCount = if (flags and 0x0001 != 0) 4 else 2
                cursor += 4 + argCount

                // WE_HAVE_A_SCALE, WE_HAVE_AN_XY_SCALE, or WE_HAVE_A_TWO_BY_TWO indicate the presence of optional
                // fields to skip over.
                if (flags and 0x0008 != 0) cursor += 2
                else if (flags and 0x0040 != 0) cursor += 4
                else if (flags and 0x0080 != 0) cursor += 8

                // MORE_COMPONENTS indicates whether there are more dependencies after this one.
                hasMore = (flags and 0x0020 != 0)
            }
        }
    }

    /** Calculates the offset of a specific glyph within the 'glyf' table. */
    private fun getGlyphOffset(
        glyphId: Int,
        locaOffset: Int,
        isLongLoca: Boolean,
    ): Int {
        return if (isLongLoca) {
            val pos = locaOffset + (glyphId * 4)
            readUInt32(pos).toInt()
        } else {
            val pos = locaOffset + (glyphId * 2)
            readUInt16(pos) * 2
        }
    }

    private fun readUInt16(offset: Int): Int {
        return ((fontData[offset].toInt() and 0xFF) shl 8) or (fontData[offset + 1].toInt() and 0xFF)
    }

    private fun readInt16(offset: Int): Int {
        return (fontData[offset].toInt() shl 8) or (fontData[offset + 1].toInt() and 0xFF)
    }

    private fun readUInt32(offset: Int): Long {
        return ((fontData[offset].toLong() and 0xFF) shl 24) or
                ((fontData[offset + 1].toLong() and 0xFF) shl 16) or
                ((fontData[offset + 2].toLong() and 0xFF) shl 8) or
                (fontData[offset + 3].toLong() and 0xFF)
    }
}