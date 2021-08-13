package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.model.Crossword
import com.jeffpdavidson.kotwords.model.Puzzle
import io.ktor.utils.io.charsets.Charset
import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.core.BytePacketBuilder
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.String
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.fill
import io.ktor.utils.io.core.readBytes
import io.ktor.utils.io.core.readShortLittleEndian
import io.ktor.utils.io.core.readTextExactCharacters
import io.ktor.utils.io.core.toByteArray
import io.ktor.utils.io.core.writeFully
import io.ktor.utils.io.core.writeLongLittleEndian
import io.ktor.utils.io.core.writeShortLittleEndian
import io.ktor.utils.io.core.writeText

private const val FILE_MAGIC = "ACROSS&DOWN"
private const val FORMAT_VERSION = "1.4"
private const val UTF8_FORMAT_VERSION = "2.0"
private val validSymbolRegex = "[@#$%&+?A-Z0-9]".toRegex()

/**
 * Container for a puzzle in the Across Lite binary file format.
 *
 * This implements [Puzzleable] and as such can create a [Puzzle] structure for the puzzle it
 * represents with [asPuzzle]. However, in the common case that the puzzle is just being
 * serialized to disk in this format, prefer [binaryData] which is already in the correct format.
 *
 * @param binaryData The raw binary data in Across Lite format.
 */
class AcrossLite(val binaryData: ByteArray) : Puzzleable {

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

    override fun asPuzzle(): Puzzle = asCrossword().asPuzzle()

    fun asCrossword(): Crossword {
        with(ByteReadPacket(binaryData)) {
            discardExact(0x18)
            val version = readTextExactCharacters(4, Charsets.ISO_8859_1)
            // 2.0 uses UTF-8; earlier versions use ISO-8859-1.
            val charset = if (version[0].digitToInt() > 1) Charsets.UTF_8 else Charsets.ISO_8859_1

            discardExact(0x10)
            val width = readByte()
            val height = readByte()

            discardExact(6)

            val solutions = readTextExactCharacters(width * height, Charsets.ISO_8859_1)
            val entries = readTextExactCharacters(width * height, Charsets.ISO_8859_1)

            val title = readNullTerminatedString(charset)
            val author = readNullTerminatedString(charset)
            val copyright = readNullTerminatedString(charset)

            val acrossClues = mutableMapOf<Int, String>()
            val downClues = mutableMapOf<Int, String>()
            val initialGrid = solutions.chunked(width.toInt()).map { row ->
                row.map { solutionChar ->
                    if (solutionChar == '.') Puzzle.Cell(cellType = Puzzle.CellType.BLOCK) else Puzzle.Cell()
                }
            }
            Crossword.forEachNumberedCell(initialGrid) { _, _, clueNumber, isAcross, isDown ->
                if (isAcross) {
                    acrossClues[clueNumber] = readNullTerminatedString(charset)
                }
                if (isDown) {
                    downClues[clueNumber] = readNullTerminatedString(charset)
                }
            }

            val notes = readNullTerminatedString(charset)

            // Read the extra sections for rebuses and circled squares.
            val rebusMap = mutableMapOf<Pair<Int, Int>, Int>()
            val rebusTable = mutableMapOf<Int, String>()
            val rebusEntries = mutableMapOf<Pair<Int, Int>, String>()
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
                        val data = readNullTerminatedString(Charsets.ISO_8859_1)
                        data.substringBeforeLast(';').split(';').forEach {
                            val parts = it.split(':')
                            rebusTable[parts[0].trim().toInt()] = parts[1]
                        }
                    }
                    "RUSR" -> {
                        for (y in 0 until height) {
                            for (x in 0 until width) {
                                val entryRebus = readNullTerminatedString(Charsets.ISO_8859_1)
                                if (entryRebus.isNotEmpty()) {
                                    rebusEntries[x to y] = entryRebus
                                }
                            }
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

            val grid = initialGrid.mapIndexed { y, row ->
                row.mapIndexed { x, cell ->
                    if (cell.cellType.isBlack()) {
                        cell
                    } else {
                        val solution = rebusMap[x to y]?.let { rebusTable[it] } ?: solutions[y * width + x].toString()
                        val entry = rebusEntries.getOrElse(x to y) { entries[y * width + x].toString() }
                        val backgroundShape =
                            if (circles.contains(x to y)) {
                                Puzzle.BackgroundShape.CIRCLE
                            } else {
                                Puzzle.BackgroundShape.NONE
                            }
                        Puzzle.Cell(
                            solution = solution,
                            backgroundShape = backgroundShape,
                            entry = if (entry == "-") "" else entry,
                        )
                    }
                }
            }

            return Crossword(
                title = title,
                creator = author,
                copyright = copyright,
                description = notes,
                acrossClues = acrossClues,
                downClues = downClues,
                grid = grid,
            )
        }
    }

    companion object {
        /**
         * Return whether this puzzle can be encoded as an Across Lite file.
         */
        fun Puzzle.supportsAcrossLite(): Boolean {
            if (puzzleType != Puzzle.PuzzleType.CROSSWORD) {
                return false
            }
            val acrossClues = getClues("Across")
            val downClues = getClues("Down")
            return acrossClues != null && downClues != null && acrossClues != downClues
        }

        /**
         * Serialize this puzzle into Across Lite binary format.
         *
         * @param solved If true, the grid will be filled in with the correct solution.
         * @param writeUtf8 If true, clues and metadata will be written directly as UTF-8 characters, if needed. This
         *                  uses the 2.0 version of the Across Lite format, which may not be supported by all
         *                  applications. If false, clues and metadata will be written as ISO-8859-1 characters, and
         *                  unsupported characters will be substituted or dropped.
         */
        fun Puzzle.asAcrossLiteBinary(
            solved: Boolean = false,
            writeUtf8: Boolean = true,
        ): ByteArray {
            require(supportsAcrossLite()) { "Cannot save puzzle as an Across Lite file." }

            var unsupportedFeatures = hasUnsupportedFeatures || clues.size > 2

            // Validate that the solution and entry grids only contains supported characters.
            val cleanedGrid = grid.map { row ->
                row.map { cell ->
                    if (cell.cellType.isBlack()) {
                        cell
                    } else {
                        val sanitizedSolution = getValidSolutionRebus(cell)
                        val validSolution =
                            if (sanitizedSolution != null) {
                                sanitizedSolution
                            } else {
                                // Show a warning about unsupported features, and fall back to "X".
                                unsupportedFeatures = true
                                "X"
                            }
                        require(cell.entry.isEmpty() || isValidGridString(cell.entry)) {
                            "Unsupported entry: ${cell.entry}"
                        }
                        if (validSolution == cell.solution) {
                            cell
                        } else {
                            cell.copy(solution = validSolution)
                        }
                    }
                }
            }

            fun BytePacketBuilder.writeExtraSection(
                name: String, length: Int,
                writeDataFn: (BytePacketBuilder) -> Unit
            ) {
                writeText(name, charset = Charsets.ISO_8859_1)
                writeShortLittleEndian(length.toShort())

                // Write the data to a separate packet so we can calculate the checksum.
                val dataPacket = BytePacketBuilder()
                writeDataFn(dataPacket)
                val dataBytes = dataPacket.build().readBytes()

                writeShortLittleEndian(checksumRegion(dataBytes, 0, dataBytes.size, 0).toShort())
                writeFully(dataBytes)
                writeByte(0)
            }

            val acrossClues = getClues("Across") ?: error("No Across clues")
            val downClues = getClues("Down") ?: error("No Down clues")

            val useUtf8 = writeUtf8 && needsUtf8(this, acrossClues, downClues)
            val charset = if (useUtf8) Charsets.UTF_8 else Charsets.ISO_8859_1

            // Sanitize the clue numbers/clues to be Across Lite compatible.
            val (adjustedAcrossClues, adjustedDownClues) =
                AcrossLiteSanitizer.sanitizeClues(cleanedGrid, acrossClues, downClues, sanitizeCharacters = !useUtf8)

            val clueCount = adjustedAcrossClues.size + adjustedDownClues.size
            val squareCount = cleanedGrid.size * cleanedGrid[0].size

            // Construct the puzzle data, leaving placeholders for each checksum.
            val output = buildPacket {
                // 0x00-0x01: file checksum placeholder
                writeShortLittleEndian(0)

                // 0x02-0x0D: file magic
                writeNullTerminatedString(FILE_MAGIC, Charsets.ISO_8859_1)

                // 0x0E-0x17: checksum placeholders
                fill(10, 0)

                // 0x18-0x1B: format version
                writeNullTerminatedString(if (useUtf8) UTF8_FORMAT_VERSION else FORMAT_VERSION, Charsets.ISO_8859_1)

                // 0x1C-0x1D: unknown
                writeShortLittleEndian(0)

                // 0x1E-0x1F: solution checksum for scrambled puzzles
                writeShortLittleEndian(0)

                // 0x20-0x2B: unknown
                fill(12, 0)

                // 0x2C: width
                writeByte(cleanedGrid[0].size.toByte())

                // 0x2D: height
                writeByte(cleanedGrid.size.toByte())

                // 0x2E-0x2F: number of clues
                writeShortLittleEndian(clueCount.toShort())

                // 0x30-0x31: puzzle type (normal vs. diagramless)
                writeShortLittleEndian(1)

                // 0x32-0x33: scrambled tag (unscrambled vs. scrambled vs. no solution)
                writeShortLittleEndian(0)

                // Board solution, reading left to right, top to bottom
                writeGrid(cleanedGrid, '.') {
                    it.solution[0]
                }

                // Player state, reading left to right, top to bottom
                writeGrid(cleanedGrid, '.') {
                    when {
                        solved || it.cellType == Puzzle.CellType.CLUE -> it.solution[0]
                        it.entry.isNotEmpty() -> it.entry[0]
                        else -> '-'
                    }
                }

                // Strings
                writeNullTerminatedString(
                    AcrossLiteSanitizer.substituteUnsupportedText(title, sanitizeCharacters = !useUtf8), charset
                )
                writeNullTerminatedString(
                    AcrossLiteSanitizer.substituteUnsupportedText(creator, sanitizeCharacters = !useUtf8), charset
                )
                writeNullTerminatedString(
                    AcrossLiteSanitizer.substituteUnsupportedText(copyright, sanitizeCharacters = !useUtf8), charset
                )

                // Clues in numerical order. If two clues have the same number, across comes before down.
                adjustedAcrossClues.keys.plus(adjustedDownClues.keys).sorted().forEach { clueNum ->
                    if (clueNum in adjustedAcrossClues) {
                        writeNullTerminatedString(adjustedAcrossClues[clueNum]!!, charset)
                    }
                    if (clueNum in adjustedDownClues) {
                        writeNullTerminatedString(adjustedDownClues[clueNum]!!, charset)
                    }
                }

                val combinedNotes = listOfNotNull(
                    description.ifEmpty { null },
                    if (unsupportedFeatures) UNSUPPORTED_FEATURES_WARNING else null
                ).joinToString("\n\n")
                writeNullTerminatedString(
                    AcrossLiteSanitizer.substituteUnsupportedText(combinedNotes, sanitizeCharacters = !useUtf8), charset
                )

                // GRBS/RUSR/RTBL sections for rebus squares.
                if (cleanedGrid.flatAny { it.solution.length > 1 || it.entry.length > 1 }) {
                    // Create map from solution rebus to a unique index for that rebus, starting at 1.
                    val rebusTable = cleanedGrid.flatMap { row ->
                        row.map { cell ->
                            cell.solution
                        }
                    }.filter { it.length > 1 }.distinct().mapIndexed { index, it -> it to index + 1 }.toMap()

                    // GRBS section: map grid squares to rebus table entries.
                    writeExtraSection("GRBS", squareCount) { packetBuilder ->
                        // 0 for non-rebus squares, 1+n for entry with key n in the rebus table.
                        packetBuilder.writeGrid(cleanedGrid, 0) {
                            if (it.solution in rebusTable) {
                                (1 + rebusTable[it.solution]!!).toByte()
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
                        packetBuilder.writeText(rtblData, charset = Charsets.UTF_8)
                    }

                    // RUSR section: user rebus entries.
                    if (solved || cleanedGrid.flatAny { it.cellType == Puzzle.CellType.CLUE || it.entry.length > 1 }) {
                        fun getRusr(cell: Puzzle.Cell): String {
                            val entry =
                                if (solved || cell.cellType == Puzzle.CellType.CLUE) {
                                    cell.solution
                                } else {
                                    cell.entry
                                }
                            return if (entry.length > 1) entry else ""
                        }

                        val length = cleanedGrid.flatten().sumOf { getRusr(it).length + 1 }
                        writeExtraSection("RUSR", length) { packetBuilder ->
                            cleanedGrid.forEach { row ->
                                row.forEach { square ->
                                    packetBuilder.writeNullTerminatedString(getRusr(square), Charsets.ISO_8859_1)
                                }
                            }
                        }
                    }
                }

                // GEXT section for circled/given squares.
                fun Puzzle.Cell.isCircled() = backgroundShape == Puzzle.BackgroundShape.CIRCLE
                fun Puzzle.Cell.isGiven() = cellType == Puzzle.CellType.CLUE
                fun Puzzle.Cell.hasColor() = !cellType.isBlack() && backgroundColor.isNotEmpty()
                if (cleanedGrid.flatAny { it.isCircled() || it.isGiven() || it.hasColor() }) {
                    val hasCircledSquare = cleanedGrid.flatAny { it.isCircled() }
                    writeExtraSection("GEXT", squareCount) { packetBuilder ->
                        packetBuilder.writeGrid(cleanedGrid, 0) {
                            var status = 0
                            // If at least one square is circled, respect the isCircled bit and ignore all background
                            // colors. If no squares are circled, then circle any square with an explicit background
                            // color.
                            if (it.isCircled() || (!hasCircledSquare && it.hasColor())) {
                                status = status or 0x80
                            }
                            if (it.isGiven()) status = status or 0x40
                            status.toByte()
                        }
                    }
                }

                if (solved) {
                    // LTIM section: timer (stopped at 0).
                    writeExtraSection("LTIM", 3) { packetBuilder ->
                        packetBuilder.writeText("0,1", charset = Charsets.ISO_8859_1)
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

        private fun needsUtf8(puzzle: Puzzle, acrossClues: Puzzle.ClueList, downClues: Puzzle.ClueList): Boolean {
            return puzzle.title.needsUtf8()
                    || puzzle.creator.needsUtf8()
                    || puzzle.copyright.needsUtf8()
                    || puzzle.description.needsUtf8()
                    || acrossClues.clues.any { it.text.needsUtf8() }
                    || downClues.clues.any { it.text.needsUtf8() }
        }

        private fun String.needsUtf8(): Boolean = any { it.code >= 256 }
    }
}

private inline fun BytePacketBuilder.writeGrid(
    grid: List<List<Puzzle.Cell>>, blackCellValue: Byte, crossinline whiteCellFn: (Puzzle.Cell) -> Byte
) {
    Crossword.forEachCell(grid) { _, _, _, _, _, cell ->
        writeByte(if (cell.cellType.isBlack()) blackCellValue else whiteCellFn(cell))
    }
}

private inline fun BytePacketBuilder.writeGrid(
    grid: List<List<Puzzle.Cell>>, blackCellValue: Char, crossinline whiteCellFn: (Puzzle.Cell) -> Char
) {
    Crossword.forEachCell(grid) { _, _, _, _, _, cell ->
        val char = if (cell.cellType.isBlack()) blackCellValue else whiteCellFn(cell)
        writeText(char.toString(), charset = Charsets.ISO_8859_1)
    }
}

private inline fun <T> List<List<T>>.flatAny(predicate: (T) -> Boolean): Boolean {
    return any { row -> row.any { predicate(it) } }
}

private fun ByteReadPacket.readNullTerminatedString(charset: Charset): String {
    val data = BytePacketBuilder()
    var byte: Byte
    while (run { byte = readByte(); byte } != 0.toByte()) {
        data.writeByte(byte)
    }
    return String(data.build().readBytes(), charset = charset)
}

private fun BytePacketBuilder.writeNullTerminatedString(string: String, charset: Charset) {
    val stringBytes = string.toByteArray(charset)
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

private fun isValidGridString(string: String): Boolean =
    string.length in 1..8 && !string.any { !isValidGridCharacter(it) }

private fun getValidSolutionRebus(cell: Puzzle.Cell): String? {
    if (isValidGridString(cell.solution)) {
        return cell.solution
    }

    val solutionCandidates = mutableListOf<String>()
    if (cell.solution.isEmpty()) {
        // Across Lite doesn't support empty solutions. If we have a valid alternate, just use that.
        cell.moreAnswers.firstOrNull { isValidGridString(it) }?.let { return it }
    } else {
        solutionCandidates += cell.solution
    }

    solutionCandidates += cell.moreAnswers

    // solutionCandidates has the target invalid solution (if non-empty), as well as all alternate answers, which may
    // or may not be valid. Now, try sanitizing each alternate to see if we can find a valid solution.
    solutionCandidates.forEach { candidate ->
        var processedCandidate = candidate
        if (processedCandidate.matches("[^/]+/[^/]+".toRegex())) {
            // Answer of the form A/B, which is common when the answer differs in both directions. Remove the "/".
            processedCandidate = processedCandidate.replace("/", "")
            if (isValidGridString(processedCandidate)) {
                return processedCandidate
            }
        }
        if (processedCandidate.length <= 8) {
            // Try substituting invalid characters.
            processedCandidate =
                AcrossLiteSanitizer.substituteUnsupportedText(processedCandidate, sanitizeCharacters = true)
            if (isValidGridString(processedCandidate)) {
                return processedCandidate
            }
        }
    }

    // Could not find a solution that is safe enough to use.
    return null
}
