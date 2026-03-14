package com.jeffpdavidson.kotwords.formats.pdf

import okio.Buffer

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
     * Create a copy of this font with glyph data for characters other than [usedChars] removed.
     *
     * Rewrites the 'glyf' and 'loca' tables to exclusively contain data for used characters, packing them tightly.
     * Unused characters are still present (to avoid having to modify composite glyph structures) but have a length of
     * 0.
     */
    fun createSubsettedFont(usedChars: Set<Char>): ByteArray {
        val head = tableOffsets["head"] ?: return fontData
        val loca = tableOffsets["loca"] ?: return fontData
        val glyf = tableOffsets["glyf"] ?: return fontData

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

        val newGlyf = Buffer()
        val newLoca = Buffer()

        for (id in 0..numGlyphs) {
            val locaValue = if (isLongLoca) newGlyf.size.toInt() else newGlyf.size.toInt() / 2
            if (isLongLoca) {
                newLoca.writeInt(locaValue)
            } else {
                newLoca.writeShort(locaValue)
            }

            if (id < numGlyphs && id in usedGlyphs) {
                val offset = getGlyphOffset(glyphId = id, locaOffset = loca.offset.toInt(), isLongLoca = isLongLoca)
                val nextOffset =
                    getGlyphOffset(glyphId = id + 1, locaOffset = loca.offset.toInt(), isLongLoca = isLongLoca)
                val length = nextOffset - offset

                if (length > 0) {
                    val start = glyf.offset.toInt() + offset
                    newGlyf.write(fontData, start, length)
                }
            }
        }

        val unpaddedGlyfSize = newGlyf.size.toInt()
        while (newGlyf.size % 4 != 0L) {
            newGlyf.writeByte(0)
        }
        val unpaddedLocaSize = newLoca.size.toInt()
        while (newLoca.size % 4 != 0L) {
            newLoca.writeByte(0)
        }

        // Values are padded to be 4-byte aligned.
        val newTablesData =
            mutableMapOf<String, ByteArray>("glyf" to newGlyf.readByteArray(), "loca" to newLoca.readByteArray())
        // Lengths of each table, without padding.
        val unpaddedTableLengths = mutableMapOf<String, Int>("glyf" to unpaddedGlyfSize, "loca" to unpaddedLocaSize)

        tableOffsets.filterKeys { it in TABLES_TO_KEEP }.forEach { (tag, entry) ->
            val length = entry.length.toInt()
            unpaddedTableLengths[tag] = length
            val padding = if (length % 4 == 0) 0 else 4 - (length % 4)
            val data = ByteArray(length + padding)
            fontData.copyInto(
                destination = data,
                destinationOffset = 0,
                startIndex = entry.offset.toInt(),
                endIndex = (entry.offset + entry.length).toInt()
            )
            newTablesData[tag] = data
        }

        val result = Buffer()
        result.write(fontData, 0, 4)

        val numTables = newTablesData.size
        var maxPow2 = 1
        var entrySelector = 0
        while (maxPow2 * 2 <= numTables) {
            maxPow2 *= 2
            entrySelector++
        }
        val searchRange = maxPow2 * 16
        val rangeShift = numTables * 16 - searchRange

        result.writeShort(numTables)
        result.writeShort(searchRange)
        result.writeShort(entrySelector)
        result.writeShort(rangeShift)

        val sortedTags = newTablesData.keys.sorted()

        var currentOffset = 12 + (newTablesData.size * 16)
        var headOffset = 0
        for (tag in sortedTags) {
            if (tag == "head") {
                headOffset = currentOffset
            }
            val data = newTablesData[tag]!!
            result.writeUtf8(tag)
            result.writeInt(calculateChecksum(data))
            result.writeInt(currentOffset)
            result.writeInt(unpaddedTableLengths[tag]!!)
            currentOffset += data.size
        }

        for (tag in sortedTags) {
            result.write(newTablesData[tag]!!)
        }

        val resultBytes = result.readByteArray()

        // Calculate the global checksum in the head table.
        resultBytes[headOffset + 8] = 0
        resultBytes[headOffset + 9] = 0
        resultBytes[headOffset + 10] = 0
        resultBytes[headOffset + 11] = 0

        val globalChecksum = calculateChecksum(resultBytes)
        val adjustment = (0xB1B0AFBAL - globalChecksum) and 0xFFFFFFFFL

        resultBytes[headOffset + 8] = (adjustment shr 24).toByte()
        resultBytes[headOffset + 9] = (adjustment shr 16).toByte()
        resultBytes[headOffset + 10] = (adjustment shr 8).toByte()
        resultBytes[headOffset + 11] = adjustment.toByte()

        return resultBytes
    }

    private fun calculateChecksum(data: ByteArray): Int {
        var sum = 0L
        for (i in 0 until data.size step 4) {
            var value = 0L
            if (i < data.size) value = value or ((data[i].toLong() and 0xFF) shl 24)
            if (i + 1 < data.size) value = value or ((data[i + 1].toLong() and 0xFF) shl 16)
            if (i + 2 < data.size) value = value or ((data[i + 2].toLong() and 0xFF) shl 8)
            if (i + 3 < data.size) value = value or (data[i + 3].toLong() and 0xFF)
            sum = (sum + value) and 0xFFFFFFFFL
        }
        return sum.toInt()
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

    companion object {
        /** Tables to keep when creating the subset font. */
        private val TABLES_TO_KEEP = setOf("head", "hhea", "maxp", "cvt ", "prep", "fpgm", "hmtx", "gasp")
    }
}