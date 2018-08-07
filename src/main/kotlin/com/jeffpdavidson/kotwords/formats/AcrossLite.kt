package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.model.BLACK_SQUARE
import com.jeffpdavidson.kotwords.model.Crossword
import com.jeffpdavidson.kotwords.model.Square
import kotlinx.io.core.ByteOrder
import kotlinx.io.core.BytePacketBuilder
import kotlinx.io.core.buildPacket
import kotlinx.io.core.readBytes
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util.Locale

private val WINDOWS_1252 = Charset.forName("windows-1252")
private const val FILE_MAGIC = "ACROSS&DOWN"
private const val FORMAT_VERSION = "1.4"

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
        val magic = String(binaryData, 0x02, 0xB, WINDOWS_1252)
        if (FILE_MAGIC != magic) {
            throw InvalidFormatException("Invalid file: incorrect file magic: $magic")
        }
    }

    override fun asCrossword(): Crossword {
        with(ByteBuffer.wrap(binaryData)) {
            order(java.nio.ByteOrder.LITTLE_ENDIAN)

            position(0x2C)
            val width = get()
            val height = get()

            position(0x34)
            val grid = mutableListOf<List<Square>>()
            for (y in 0 until height) {
                val row = mutableListOf<Square>()
                for (x in 0 until width) {
                    val solution = get().toChar()
                    row.add(if (solution == '.') {
                        BLACK_SQUARE
                    } else {
                        Square(solution)
                    })
                }
                grid.add(row)
            }

            // Skip the response grid.
            position(position() + width * height)

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
            while (hasRemaining()) {
                val sectionTitleBytes = ByteArray(4)
                get(sectionTitleBytes)
                val sectionTitle = String(sectionTitleBytes, WINDOWS_1252)
                val sectionLength = short
                // Skip the checksum
                position(position() + 2)

                when (sectionTitle) {
                    "GRBS" -> {
                        for (y in 0 until height) {
                            for (x in 0 until width) {
                                val square = get().toInt()
                                if (square != 0) {
                                    rebusMap[x to y] = square - 1
                                }
                            }
                        }
                        // Skip the null terminator.
                        get()
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
                                val square = get().toInt()
                                if (square and 0x80 != 0) {
                                    circles += x to y
                                }
                            }
                        }
                        // Skip the null terminator.
                        get()
                    }
                    else -> {
                        // Skip section + null terminator.
                        position(position() + sectionLength + 1)
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
                                        isCircled = circles.contains(x to y))
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
            // Extra section positions are not fixed, so prepare a map from offset of checksum to
            // offset+length of data to be checksummed as we construct the puzzle data.
            val checksumsToCalculate: MutableMap<Int, Pair<Int, Int>> = mutableMapOf()
            fun BytePacketBuilder.writeExtraSection(name: String, length: Int, writeDataFn: () -> Unit) {
                writeStringUtf8(name)
                writeShort(length.toShort())
                // Checksum placeholder
                val checksumOffset = size
                writeShort(0)
                checksumsToCalculate[checksumOffset] = Pair(size, length)
                writeDataFn()
                writeByte(0)
            }

            val clueCount = acrossClues.size + downClues.size
            val squareCount = grid.size * grid[0].size

            // Construct the puzzle data, leaving placeholders for each checksum.
            val output = buildPacket {
                byteOrder = ByteOrder.LITTLE_ENDIAN

                // 0x00-0x01: file checksum placeholder
                writeShort(0)

                // 0x02-0x0D: file magic
                writeNullTerminatedString(FILE_MAGIC)

                // 0x0E-0x17: checksum placeholders
                writeFully(ByteArray(10))

                // 0x18-0x1B: format version
                writeNullTerminatedString(FORMAT_VERSION)

                // 0x1C-0x1D: unknown
                writeShort(0)

                // 0x1E-0x1F: solution checksum for scrambled puzzles
                writeShort(0)

                // 0x20-0x2B: unknown
                writeFully(ByteArray(12))

                // 0x2C: width
                writeByte(grid[0].size.toByte())

                // 0x2D: height
                writeByte(grid.size.toByte())

                // 0x2E-0x2F: number of clues
                writeShort(clueCount.toShort())

                // 0x30-0x31: puzzle type (normal vs. diagramless)
                writeShort(1)

                // 0x32-0x33: scrambled tag (unscrambled vs. scrambled vs. no solution)
                writeShort(0)

                // Board solution, reading left to right, top to bottom
                writeGrid(grid, '.'.toByte()) { it.solution!!.toByte() }

                // Player state, reading left to right, top to bottom
                writeGrid(grid, '.'.toByte()) {
                    if (solved) {
                        it.solution!!.toByte()
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
                    writeExtraSection("GRBS", squareCount) {
                        // 0 for non-rebus squares, 1+n for entry with key n in the rebus table.
                        writeGrid(grid, 0) {
                            if (it.solutionRebus in rebusTable) {
                                (1 + rebusTable[it.solutionRebus]!!).toByte()
                            } else {
                                0
                            }
                        }
                    }

                    // RTBL section: rebus table.
                    val rtblData = rebusTable.entries.joinToString(";", postfix = ";") {
                        "% 2d:%s".format(Locale.ROOT, it.value, it.key)
                    }
                    writeExtraSection("RTBL", rtblData.length) {
                        writeStringUtf8(rtblData)
                    }

                    if (solved) {
                        // RUSR section: user rebus entries.
                        writeExtraSection("RUSR", grid.flatten().sumBy { it.solutionRebus.length + 1 }) {
                            grid.forEach { row ->
                                row.forEach { square ->
                                    writeNullTerminatedString(square.solutionRebus)
                                }
                            }
                        }
                    }
                }

                // GEXT section for circled squares.
                if (grid.flatAny { it.isCircled }) {
                    writeExtraSection("GEXT", squareCount) {
                        // 0x80 for circled squares, 0 otherwise.
                        writeGrid(grid, 0) { if (it.isCircled) 0x80.toByte() else 0 }
                    }
                }

                if (solved) {
                    // LTIM section: timer (stopped at 0).
                    writeExtraSection("LTIM", 3) {
                        writeStringUtf8("0,1")
                    }
                }

            }
            val puzBytes = output.readBytes()

            val puzByteBuffer = ByteBuffer.wrap(puzBytes)
            puzByteBuffer.order(java.nio.ByteOrder.LITTLE_ENDIAN)

            // Calculate puzzle checksums.
            puzByteBuffer.putShort(0x0E, checksumCib(puzBytes).toShort())
            puzByteBuffer.putShort(0, checksumPrimaryBoard(puzBytes, squareCount, clueCount).toShort())
            puzByteBuffer.putLong(0x10, checksumPrimaryBoardMasked(puzBytes, squareCount, clueCount))

            // Calculate extra section checksums.
            checksumsToCalculate.forEach { (checksumOffset, dataEntry) ->
                puzByteBuffer.putShort(checksumOffset,
                        checksumRegion(puzBytes, dataEntry.first, dataEntry.second, 0).toShort())
            }

            return puzBytes
        }
    }
}

private inline fun BytePacketBuilder.writeGrid(
        grid: List<List<Square>>, blackSquareValue: Byte, whiteSquareFn: (Square) -> Byte) {
    grid.forEach { row ->
        row.forEach { square ->
            writeByte(if (square.isBlack) blackSquareValue else whiteSquareFn(square))
        }
    }
}

private inline fun <T> List<List<T>>.flatAny(predicate: (T) -> Boolean): Boolean {
    return any { row -> row.any { it: T -> predicate(it) } }
}

private fun ByteBuffer.readNullTerminatedString(): String {
    var i = position()
    while (get(i).toInt() != 0) {
        i++
    }
    val data = ByteArray(i - position())
    get(data)
    position(position() + 1)
    return String(data, WINDOWS_1252)
}

private fun BytePacketBuilder.writeNullTerminatedString(string: String) {
    // Replace fancy quotes (which don't render in Across Lite) with normal ones.
    // Ref: http://www.i18nqa.com/debug/table-iso8859-1-vs-windows-1252.html
    writeFully(WINDOWS_1252.encode(
            string.replace('‘', '\'')
                    .replace('’', '\'')
                    .replace('“', '"')
                    .replace('”', '"'))
            .array())
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
        puzBytes: ByteArray, squareCount: Int, clueCount: Int): Long {
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

private fun checksumPartialBoard(puzBytes: ByteArray, squareCount: Int, clueCount: Int,
                                 currentChecksum: Int): Int {
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
