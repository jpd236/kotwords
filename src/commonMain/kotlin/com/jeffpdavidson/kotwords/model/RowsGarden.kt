package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.formats.Puzzleable
import kotlinx.serialization.Serializable
import kotlin.math.floor
import kotlin.math.roundToInt

@Serializable
data class RowsGarden(
    val title: String,
    val author: String,
    val copyright: String,
    val notes: String? = null,
    val rows: List<List<Entry>>,
    val light: List<Entry>,
    val medium: List<Entry>,
    val dark: List<Entry>,
    val lightBloomColor: String = "#FFFFFF",
    val mediumBloomColor: String = "#C3C8FA",
    val darkBloomColor: String = "#5765F7",
    val addWordCount: Boolean = true,
    val addHyphenated: Boolean = true,
) : Puzzleable {
    @Serializable
    data class Entry(val clue: String, val answer: String)

    override fun asPuzzle(): Puzzle {
        require(rows.size > 2) {
            "Must have at least 3 rows"
        }
        val rowLetters = rows.map { words ->
            words.joinToString("") { word -> word.answer.filter(Char::isLetterOrDigit) }
        }
        require(rowLetters.subList(1, rowLetters.size - 1).all { it.length == rowLetters[1].length }) {
            "All rows except the first and last must have the same width"
        }
        require(rowLetters[1].length % 3 == 0 && rowLetters[1].length % 2 == 1) {
            "Grid width must be an odd multiple of 3"
        }
        // solutionGrid[y][x] = the solution letter at (x, y)
        var hasShortFirstRow = false
        val solutionGrid = rowLetters.mapIndexed { y, letters ->
            if (y == 0 || y == rowLetters.size - 1) {
                val validWidthShort = rowLetters[1].length / 3 / 2 * 3
                val validWidthLong = (rowLetters[1].length / 3 / 2 + 1) * 3
                when (letters.length) {
                    validWidthShort -> {
                        if (y == 0) {
                            hasShortFirstRow = true
                        }
                        letters.chunked(3).joinToString(separator = "...", prefix = "...", postfix = "...")
                    }
                    validWidthLong -> {
                        letters.chunked(3).joinToString(separator = "...")
                    }
                    else -> {
                        throw IllegalArgumentException("Outer row length must be $validWidthShort or $validWidthLong")
                    }
                }
            } else {
                letters
            }
        }

        // Loop over the grid, adding the Puzzle clues for each bloom as we encounter them.
        data class BloomData(
            val color: String,
            val clues: List<Entry>,
            val puzzleClues: MutableList<Puzzle.Clue> = mutableListOf()
        )

        val blooms = mapOf(
            BloomType.LIGHT to BloomData(lightBloomColor, light),
            BloomType.MEDIUM to BloomData(mediumBloomColor, medium),
            BloomType.DARK to BloomData(darkBloomColor, dark)
        )
        val cellMap = mutableMapOf<Pair<Int, Int>, Puzzle.Cell>()
        val bloomsWords = mutableListOf<Puzzle.Word>()
        fun getOrCreateCell(x: Int, y: Int): Puzzle.Cell {
            if (cellMap.containsKey(x to y)) {
                return cellMap.getValue(x to y)
            }
            val bloomType = BloomType.forCoordinate(x, y, hasShortFirstRow)

            cellMap[x to y] =
                if (solutionGrid[y][x] == '.') {
                    Puzzle.Cell(cellType = Puzzle.CellType.BLOCK)
                } else {
                    var bloomIndex = 0
                    val yOffset = if (hasShortFirstRow) 0 else 1
                    val number = when {
                        (x == solutionGrid[y].indexOfFirst { it != '.' }) -> {
                            // Start of a row
                            "${'A' + y}"
                        }
                        ((y + yOffset) % 2 == 0 && x % 6 == 5) || ((y + yOffset) % 2 == 1 && x % 6 == 2) -> {
                            // Start of a bloom
                            bloomIndex = blooms.getValue(bloomType).puzzleClues.size + 1
                            "${bloomType.name[0]}${bloomIndex}"
                        }
                        else -> ""
                    }
                    val cell = Puzzle.Cell(
                        number = number,
                        solution = "${solutionGrid[y][x]}",
                        backgroundColor = blooms.getValue(bloomType).color
                    )
                    if (bloomIndex > 0) {
                        val wordId = 1000 * (bloomType.ordinal + 1) + bloomIndex
                        bloomsWords.add(
                            Puzzle.Word(
                                wordId, listOf(
                                    Puzzle.Coordinate(x = x - 2, y = y),
                                    Puzzle.Coordinate(x = x - 1, y),
                                    Puzzle.Coordinate(x = x, y = y),
                                    Puzzle.Coordinate(x = x, y = y + 1),
                                    Puzzle.Coordinate(x = x - 1, y = y + 1),
                                    Puzzle.Coordinate(x = x - 2, y = y + 1),
                                )
                            )
                        )
                        blooms[bloomType]?.puzzleClues!!.add(
                            Puzzle.Clue(
                                wordId,
                                number,
                                formatClue(
                                    blooms.getValue(bloomType).clues[bloomIndex - 1],
                                    addWordCount, addHyphenated
                                )
                            )
                        )
                    }
                    cell
                }
            return cellMap.getValue(x to y)
        }

        val grid = solutionGrid.indices.map { y ->
            solutionGrid[y].indices.map { x ->
                getOrCreateCell(x, y)
            }
        }

        val (rowClues, rowsWords) = grid.mapIndexed { y, row ->
            val letters = row.mapIndexedNotNull { x, cell ->
                if (cell.cellType == Puzzle.CellType.BLOCK) null else Puzzle.Coordinate(x = x, y = y)
            }
            Puzzle.Clue(y + 1, "${y + 1}", rows[y].joinToString(" / ") {
                formatClue(it, addWordCount, addHyphenated)
            }) to Puzzle.Word(y + 1, letters)
        }.unzip()

        val bloomClues = listOf(
            blooms.getValue(BloomType.LIGHT).puzzleClues,
            blooms.getValue(BloomType.MEDIUM).puzzleClues,
            blooms.getValue(BloomType.DARK).puzzleClues
        )
            .flatten()

        return Puzzle(
            title = title,
            creator = author,
            copyright = copyright.replace("(c)", "©"),
            description = notes ?: "",
            grid = grid,
            clues = listOf(Puzzle.ClueList("Rows", rowClues), Puzzle.ClueList("Blooms", bloomClues)),
            words = rowsWords + bloomsWords.sortedBy { it.id },
        )
    }

    private fun formatClue(
        entry: Entry,
        addWordCount: Boolean,
        addHyphenated: Boolean
    ): String {
        val suffixes = mutableListOf<String>()
        if (addWordCount) {
            val wordCount = entry.answer.count { it == ' ' } + 1
            if (wordCount > 1) {
                suffixes.add("$wordCount wds.")
            }
        }
        if (addHyphenated && entry.answer.contains('-')) {
            suffixes.add("hyph.")
        }
        if (suffixes.isEmpty()) {
            return entry.clue
        }
        return "${entry.clue}: ${suffixes.joinToString(", ")}"
    }

    private enum class BloomType {
        LIGHT,
        MEDIUM,
        DARK;

        companion object {
            fun forCoordinate(x: Int, y: Int, hasShortFirstRow: Boolean): BloomType {
                val baseOffset = if (x % 6 < 3) 3 else 0
                val yOffset = if (hasShortFirstRow) baseOffset else baseOffset + 1
                return when (floor((y + yOffset) / 2.0).roundToInt() % 3) {
                    0 -> MEDIUM
                    1 -> DARK
                    else -> LIGHT
                }
            }
        }
    }
}