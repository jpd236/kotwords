package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.model.BLACK_SQUARE
import com.jeffpdavidson.kotwords.model.Crossword
import com.jeffpdavidson.kotwords.model.Square
import kotlinx.io.charsets.Charsets
import kotlinx.io.core.BytePacketBuilder
import kotlinx.io.core.ByteReadPacket
import kotlinx.io.core.String
import kotlinx.io.core.buildPacket
import kotlinx.io.core.discardExact
import kotlinx.io.core.readBytes
import kotlinx.io.core.readShortLittleEndian
import kotlinx.io.core.toByteArray
import kotlinx.io.core.writeFully
import kotlinx.io.core.writeLongLittleEndian
import kotlinx.io.core.writeShortLittleEndian

private const val FILE_MAGIC = "ACROSS&DOWN"
private const val FORMAT_VERSION = "1.4"
private val validSymbolRegex = "[@#$%&+?A-Z0-9]".toRegex()

/**
 * Container for a puzzle in the Across Lite (1.4) binary file format.
 *
 * This implements [Crosswordable] and as such can create a [Crossword] structure for the puzzle it
 * represents with [asCrossword]. However, in the common case that the puzzle is just being
 * serialized to disk in this format, prefer [binaryData] which is already in the correct format.
 *
 * @param binaryData The raw binary data in Across Lite format.
 */
class AcrossLite(val binaryData: ByteArray) : Crosswordable {

    init {
        // Verify file magic to catch any unexpected file contents.
        if (binaryData.size < 0xD) {
            throw InvalidFormatException("Invalid file: length too short: ${binaryData.size}")
        }
        val magic = String(binaryData, 0x02, 0xB, Charsets.ISO_8859_1)
        if (FILE_MAGIC != magic) {
            throw InvalidFormatException("Invalid file: incorrect file magic: $magic")
        }
    }

    override fun asCrossword(): Crossword {
        with(ByteReadPacket(binaryData)) {
            discardExact(0x2C)
            val width = readByte()
            val height = readByte()

            discardExact(6)
            val grid = mutableListOf<List<Square>>()
            for (y in 0 until height) {
                val row = mutableListOf<Square>()
                for (x in 0 until width) {
                    val solution = readByte().toChar()
                    row.add(
                        if (solution == '.') {
                            BLACK_SQUARE
                        } else {
                            Square(solution)
                        }
                    )
                }
                grid.add(row)
            }

            // Skip the response grid.
            discardExact(width * height)

            val title = readNullTerminatedString()
            val author = readNullTerminatedString()
            val copyright = readNullTerminatedString()

            val acrossClues = mutableMapOf<Int, String>()
            val downClues = mutableMapOf<Int, String>()
            Crossword.forEachNumberedSquare(grid) { _, _, clueNumber, isAcross, isDown ->
                if (isAcross) {
                    acrossClues[clueNumber] = readNullTerminatedString()
                }
                if (isDown) {
                    downClues[clueNumber] = readNullTerminatedString()
                }
            }

            val notes = readNullTerminatedString()

            // Read the extra sections for rebuses and circled squares.
            val rebusMap = mutableMapOf<Pair<Int, Int>, Int>()
            val rebusTable = mutableMapOf<Int, String>()
            val circles = mutableSetOf<Pair<Int, Int>>()
            while (canRead()) {
                val sectionTitleBytes = readBytes(4)
                val sectionTitle = String(sectionTitleBytes, charset = Charsets.ISO_8859_1)
                val sectionLength = readShortLittleEndian()
                // Skip the checksum
                discardExact(2)

                when (sectionTitle) {
                    "GRBS" -> {
                        for (y in 0 until height) {
                            for (x in 0 until width) {
                                val square = readByte().toInt()
                                if (square != 0) {
                                    rebusMap[x to y] = square - 1
                                }
                            }
                        }
                        // Skip the null terminator.
                        discardExact(1)
                    }
                    "RTBL" -> {
                        val data = readNullTerminatedString()
                        data.substringBeforeLast(';').split(';').forEach {
                            val parts = it.split(':')
                            rebusTable[parts[0].trim().toInt()] = parts[1]
                        }
                    }
                    "GEXT" -> {
                        for (y in 0 until height) {
                            for (x in 0 until width) {
                                val square = readByte().toInt()
                                if (square and 0x80 != 0) {
                                    circles += x to y
                                }
                            }
                        }
                        // Skip the null terminator.
                        discardExact(1)
                    }
                    else -> {
                        // Skip section + null terminator.
                        discardExact(sectionLength + 1)
                    }
                }
            }

            return Crossword(
                title = title,
                author = author,
                copyright = copyright,
                notes = notes,
                acrossClues = acrossClues,
                downClues = downClues,
                grid = grid.mapIndexed { y, row ->
                    row.mapIndexed { x, square ->
                        if (square == BLACK_SQUARE) {
                            square
                        } else {
                            val solutionRebus = rebusMap[x to y]?.let { rebusTable[it] } ?: ""
                            Square(
                                solution = square.solution,
                                solutionRebus = solutionRebus,
                                isCircled = circles.contains(x to y)
                            )
                        }
                    }
                }
            )
        }
    }

    companion object {
        /**
         * Serialize this crossword into Across Lite binary format.
         *
         * @param solved If true, the grid will be filled in with the correct solution.
         */
        fun Crossword.toAcrossLiteBinary(solved: Boolean = false): ByteArray {
            // Validate that the solution and entry grids only contains supported characters.
            grid.flatten().forEach { square ->
                square.run {
                    if (isBlack) {
                        require(solution == null && solutionRebus == "" && !isCircled && entry == null && !isGiven) {
                            "Black squares must not set other properties"
                        }
                    } else {
                        require(isValidGridCharacter(solution!!)) {
                            "Unsupported solution character: $solution"
                        }
                        require(entry == null || isValidGridCharacter(entry)) {
                            "Unsupported entry character: $entry"
                        }
                    }
                    require(solutionRebus.length <= 8 && !solutionRebus.any { !isValidGridCharacter(it) }) {
                        "Invalid rebus: $solutionRebus"
                    }
                }
            }

            fun BytePacketBuilder.writeExtraSection(
                name: String, length: Int,
                writeDataFn: (BytePacketBuilder) -> Unit
            ) {
                writeStringUtf8(name)
                writeShortLittleEndian(length.toShort())

                // Write the data to a separate packet so we can calculate the checksum.
                val dataPacket = BytePacketBuilder()
                writeDataFn(dataPacket)
                val dataBytes = dataPacket.build().readBytes()

                writeShortLittleEndian(checksumRegion(dataBytes, 0, dataBytes.size, 0).toShort())
                writeFully(dataBytes)
                writeByte(0)
            }

            val clueCount = acrossClues.size + downClues.size
            val squareCount = grid.size * grid[0].size

            // Construct the puzzle data, leaving placeholders for each checksum.
            val output = buildPacket {
                // 0x00-0x01: file checksum placeholder
                writeShortLittleEndian(0)

                // 0x02-0x0D: file magic
                writeNullTerminatedString(FILE_MAGIC)

                // 0x0E-0x17: checksum placeholders
                fill(10, 0)

                // 0x18-0x1B: format version
                writeNullTerminatedString(FORMAT_VERSION)

                // 0x1C-0x1D: unknown
                writeShortLittleEndian(0)

                // 0x1E-0x1F: solution checksum for scrambled puzzles
                writeShortLittleEndian(0)

                // 0x20-0x2B: unknown
                fill(12, 0)

                // 0x2C: width
                writeByte(grid[0].size.toByte())

                // 0x2D: height
                writeByte(grid.size.toByte())

                // 0x2E-0x2F: number of clues
                writeShortLittleEndian(clueCount.toShort())

                // 0x30-0x31: puzzle type (normal vs. diagramless)
                writeShortLittleEndian(1)

                // 0x32-0x33: scrambled tag (unscrambled vs. scrambled vs. no solution)
                writeShortLittleEndian(0)

                // Board solution, reading left to right, top to bottom
                writeGrid(grid, '.'.toByte()) {
                    it.solution!!.toByte()
                }

                // Player state, reading left to right, top to bottom
                writeGrid(grid, '.'.toByte()) {
                    if (solved) {
                        it.solution!!.toByte()
                    } else if (it.entry != null) {
                        it.entry.toByte()
                    } else {
                        '-'.toByte()
                    }
                }

                // Strings
                writeNullTerminatedString(title)
                writeNullTerminatedString(author)
                writeNullTerminatedString(copyright)

                // Clues in numerical order. If two clues have the same number, across comes before down.
                acrossClues.keys.plus(downClues.keys).sorted().forEach { clueNum ->
                    if (clueNum in acrossClues) {
                        writeNullTerminatedString(acrossClues[clueNum]!!)
                    }
                    if (clueNum in downClues) {
                        writeNullTerminatedString(downClues[clueNum]!!)
                    }
                }

                writeNullTerminatedString(notes)

                // GRBS/RUSR/RTBL sections for rebus squares.
                if (grid.flatAny { !it.solutionRebus.isEmpty() }) {
                    // Create map from solution rebus to a unique index for that rebus, starting at 1.
                    val rebusTable = grid.flatMap { row ->
                        row.map { square ->
                            square.solutionRebus
                        }
                    }.filterNot { it == "" }.distinct().mapIndexed { index, it -> it to index + 1 }.toMap()

                    // GRBS section: map grid squares to rebus table entries.
                    writeExtraSection("GRBS", squareCount) { packetBuilder ->
                        // 0 for non-rebus squares, 1+n for entry with key n in the rebus table.
                        packetBuilder.writeGrid(grid, 0) {
                            if (it.solutionRebus in rebusTable) {
                                (1 + rebusTable[it.solutionRebus]!!).toByte()
                            } else {
                                0
                            }
                        }
                    }

                    // RTBL section: rebus table.
                    val rtblData = rebusTable.entries.joinToString(";", postfix = ";") {
                        "${if (it.value < 10) " " else ""}${it.value}:${it.key}"
                    }
                    writeExtraSection("RTBL", rtblData.length) { packetBuilder ->
                        packetBuilder.writeStringUtf8(rtblData)
                    }

                    if (solved) {
                        // RUSR section: user rebus entries.
                        val length = grid.flatten().sumBy { it.solutionRebus.length + 1 }
                        writeExtraSection("RUSR", length) { packetBuilder ->
                            grid.forEach { row ->
                                row.forEach { square ->
                                    packetBuilder.writeNullTerminatedString(square.solutionRebus)
                                }
                            }
                        }
                    }
                }

                // GEXT section for circled/given squares.
                if (grid.flatAny { it.isCircled || it.isGiven }) {
                    writeExtraSection("GEXT", squareCount) { packetBuilder ->
                        packetBuilder.writeGrid(grid, 0) {
                            var status = 0
                            if (it.isCircled) status = status or 0x80
                            if (it.isGiven) status = status or 0x40
                            status.toByte()
                        }
                    }
                }

                if (solved) {
                    // LTIM section: timer (stopped at 0).
                    writeExtraSection("LTIM", 3) { packetBuilder ->
                        packetBuilder.writeStringUtf8("0,1")
                    }
                }

            }
            val puzBytes = output.readBytes()

            val checksumPacketBuilder = BytePacketBuilder()

            // Calculate puzzle checksums.
            checksumPacketBuilder.writeShortLittleEndian(
                checksumPrimaryBoard(puzBytes, squareCount, clueCount).toShort()
            )
            checksumPacketBuilder.writeFully(puzBytes, 0x2, 0xC)
            checksumPacketBuilder.writeShortLittleEndian(checksumCib(puzBytes).toShort())
            checksumPacketBuilder.writeLongLittleEndian(
                checksumPrimaryBoardMasked(puzBytes, squareCount, clueCount)
            )
            checksumPacketBuilder.writeFully(puzBytes, 0x18)

            return checksumPacketBuilder.build().readBytes()
        }
    }
}

private inline fun BytePacketBuilder.writeGrid(
    grid: List<List<Square>>, blackSquareValue: Byte, whiteSquareFn: (Square) -> Byte
) {
    grid.forEach { row ->
        row.forEach { square ->
            writeByte(if (square.isBlack) blackSquareValue else whiteSquareFn(square))
        }
    }
}

private inline fun <T> List<List<T>>.flatAny(predicate: (T) -> Boolean): Boolean {
    return any { row -> row.any { predicate(it) } }
}

private fun ByteReadPacket.readNullTerminatedString(): String {
    val data = BytePacketBuilder()
    var byte: Byte = 0
    while ({ byte = readByte(); byte }() != 0.toByte()) {
        data.writeByte(byte)
    }
    return String(data.build().readBytes(), charset = Charsets.ISO_8859_1)
}

private fun BytePacketBuilder.writeNullTerminatedString(string: String) {
    // Replace fancy quotes (which don't render in Across Lite on Mac) with normal ones.
    // Ref: http://www.i18nqa.com/debug/table-iso8859-1-vs-windows-1252.html
    // TODO: More explicit handling of unsupported characters.
    val stringBytes = string.replace('‘', '\'')
        .replace('’', '\'')
        .replace('“', '"')
        .replace('”', '"')
        // en/em dashes are unsupported in ISO-8859-1
        .replace("—", "-")
        .replace("–", "-")
        .toByteArray(Charsets.ISO_8859_1)
    writeFully(stringBytes)
    writeByte(0)
}

private fun checksumRegion(data: ByteArray, offset: Int, length: Int, currentChecksum: Int): Int {
    var checksum = currentChecksum
    for (i in offset until offset + length) {
        checksum = if (checksum and 0x01 != 0) (checksum shr 1) + 0x8000 else checksum shr 1
        checksum += 0xFF and data[i].toInt()
        checksum = checksum and 0xFFFF
    }
    return checksum
}

private fun checksumCib(puzBytes: ByteArray): Int {
    return checksumRegion(puzBytes, 0x2C, 8, 0)
}

private fun checksumPrimaryBoard(puzBytes: ByteArray, squareCount: Int, clueCount: Int): Int {
    var checksum = checksumCib(puzBytes)
    checksum = checksumSolution(puzBytes, squareCount, checksum)
    checksum = checksumGrid(puzBytes, squareCount, checksum)
    checksum = checksumPartialBoard(puzBytes, squareCount, clueCount, checksum)
    return checksum
}

private fun checksumPrimaryBoardMasked(
    puzBytes: ByteArray, squareCount: Int, clueCount: Int
): Long {
    val cibChecksum = checksumCib(puzBytes)
    val solutionChecksum = checksumSolution(puzBytes, squareCount, 0)
    val gridChecksum = checksumGrid(puzBytes, squareCount, 0)
    val partialBoardChecksum = checksumPartialBoard(puzBytes, squareCount, clueCount, 0)

    var checksum = 0L
    checksum = checksum or (0x44 xor ((partialBoardChecksum and 0xFF00) shr 8)).toLong()
    checksum = (checksum shl 8) or (0x45 xor ((gridChecksum and 0xFF00) shr 8)).toLong()
    checksum = (checksum shl 8) or (0x54 xor ((solutionChecksum and 0xFF00) shr 8)).toLong()
    checksum = (checksum shl 8) or (0x41 xor ((cibChecksum and 0xFF00) shr 8)).toLong()
    checksum = (checksum shl 8) or (0x45 xor (partialBoardChecksum and 0xFF)).toLong()
    checksum = (checksum shl 8) or (0x48 xor (gridChecksum and 0xFF)).toLong()
    checksum = (checksum shl 8) or (0x43 xor (solutionChecksum and 0xFF)).toLong()
    checksum = (checksum shl 8) or (0x49 xor (cibChecksum and 0xFF)).toLong()
    return checksum
}

private fun checksumSolution(puzBytes: ByteArray, squareCount: Int, currentChecksum: Int): Int {
    return checksumRegion(puzBytes, 0x34, squareCount, currentChecksum)
}

private fun checksumGrid(puzBytes: ByteArray, squareCount: Int, currentChecksum: Int): Int {
    return checksumRegion(puzBytes, 0x34 + squareCount, squareCount, currentChecksum)
}

private fun checksumPartialBoard(
    puzBytes: ByteArray, squareCount: Int, clueCount: Int,
    currentChecksum: Int
): Int {
    var checksum = currentChecksum
    var offset = 0x34 + 2 * squareCount
    for (i in 0 until 4 + clueCount) {
        val startOffset = offset
        while (puzBytes[offset].toInt() != 0) {
            offset++
        }
        val length = offset - startOffset
        if (i > 2 && i < 3 + clueCount) {
            checksum = checksumRegion(puzBytes, startOffset, length, checksum)
        } else if (length > 0) {
            checksum = checksumRegion(puzBytes, startOffset, length + 1, checksum)
        }
        offset++
    }
    return checksum
}

/**
 * Validate a grid character.
 *
 * The only permitted characters are upper-case letters, numbers, and the characters '@', '#', '$', '%', '&', '+', and
 * '?', as these are the only characters that can be inserted by the user in Across Lite (see [the text format
 * specification](http://www.litsoft.com/across/docs/AcrossTextFormat.pdf)).
 */
private fun isValidGridCharacter(character: Char): Boolean {
    return character.toString().matches(validSymbolRegex)
}
