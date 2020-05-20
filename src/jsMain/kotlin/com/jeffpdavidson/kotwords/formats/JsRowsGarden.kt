package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.model.Cell
import com.jeffpdavidson.kotwords.model.CellType
import com.jeffpdavidson.kotwords.model.Clue
import com.jeffpdavidson.kotwords.model.ClueList
import com.jeffpdavidson.kotwords.model.CrosswordSolverSettings
import com.jeffpdavidson.kotwords.model.Jpz
import com.jeffpdavidson.kotwords.model.Word
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import kotlin.js.Promise
import kotlin.math.floor
import kotlin.math.roundToInt

// TODO: Combine w/ RowsGarden once JPZ is in common.
class JsRowsGarden(private val rg: RowsGarden) {
    @JsName("asJpz")
    fun asJpz(
            lightBloomColor: String,
            mediumBloomColor: String,
            darkBloomColor: String,
            addWordCount: Boolean,
            addHyphenated: Boolean,
            crosswordSolverSettings: CrosswordSolverSettings): Jpz {
        require(rg.rows.size == 12) {
            "Only 21x12 grids are supported"
        }
        // solutionGrid[y][x] = the solution letter at (x, y)
        val solutionGrid = rg.rows.mapIndexed { y, words ->
            val letters = words.joinToString("") { word -> word.answer.filter(::isAlphanumeric) }
            if (y == 0 || y == 11) {
                require(letters.length == 9) {
                    "Row $y has ${letters.length} letters; should have 9"
                }
                "...${letters.slice(0..2)}...${letters.slice(3..5)}...${letters.slice(6..8)}..."
            } else {
                require(letters.length == 21) {
                    "Row $y has ${letters.length} letters; should have 21"
                }
                letters
            }
        }

        // Loop over the grid, adding the JPZ Clues for each bloom as we encounter them.
        data class BloomData(
                val color: String,
                val clues: List<RowsGarden.Entry>,
                val jpzClues: MutableList<Clue> = mutableListOf())
        val blooms = mapOf(
                BloomType.LIGHT to BloomData(lightBloomColor, rg.light),
                BloomType.MEDIUM to BloomData(mediumBloomColor, rg.medium),
                BloomType.DARK to BloomData(darkBloomColor, rg.dark))
        val cellMap = mutableMapOf<Pair<Int, Int>, Cell>()
        fun getOrCreateCell(x: Int, y: Int): Cell {
            if (cellMap.containsKey(x to y)) {
                return cellMap.getValue(x to y)
            }
            val bloomType = BloomType.forCoordinate(x, y)
            cellMap[x to y] =
                    if (bloomType == BloomType.NONE) {
                        Cell(x + 1, y + 1, cellType = CellType.BLOCK)
                    } else {
                        var bloomIndex = 0
                        val number = when {
                            ((y == 0 || y == 11) && x == 3) || ((y != 0 && y != 11) && x == 0) -> {
                                // Start of a row
                                "${'A' + y}"
                            }
                            (y % 2 == 0 && x % 6 == 5) || (y % 2 == 1 && x % 6 == 2) -> {
                                // Start of a bloom
                                bloomIndex = blooms.getValue(bloomType).jpzClues.size + 1
                                "${bloomType.name[0]}${bloomIndex}"
                            }
                            else -> ""
                        }
                        val cell = Cell(x + 1, y + 1,
                                number = number,
                                solution = "${solutionGrid[y][x]}",
                                backgroundColor = blooms.getValue(bloomType).color)
                        if (bloomIndex > 0) {
                            blooms[bloomType]?.jpzClues!!.add(Clue(
                                    Word(1000 * bloomType.ordinal + bloomIndex, listOf(
                                            getOrCreateCell(x - 2, y),
                                            getOrCreateCell(x - 1, y),
                                            cell,
                                            getOrCreateCell(x, y + 1),
                                            getOrCreateCell(x - 1, y + 1),
                                            getOrCreateCell(x - 2, y + 1))),
                                    number,
                                    formatClue(blooms.getValue(bloomType).clues[bloomIndex - 1],
                                            addWordCount, addHyphenated)
                            ))
                        }
                        cell
                    }
            return cellMap.getValue(x to y)
        }
        val grid = (0 until 12).map { y ->
            (0 until 21).map { x ->
                getOrCreateCell(x, y)
            }
        }

        val rowClues = grid.mapIndexed { y, row ->
            val letters = row.filter { it.cellType != CellType.BLOCK }
            Clue(Word(y + 1, letters), "${y + 1}", rg.rows[y].joinToString(" / ") {
                formatClue(it, addWordCount, addHyphenated)
            })
        }

        val bloomClues = listOf(
                blooms.getValue(BloomType.LIGHT).jpzClues,
                blooms.getValue(BloomType.MEDIUM).jpzClues,
                blooms.getValue(BloomType.DARK).jpzClues)
                .flatten()

        return Jpz(
                title = rg.title,
                creator = rg.author,
                copyright = rg.copyright.replace("(c)", "©"),
                description = rg.notes,
                grid = grid,
                clues = listOf(ClueList("Rows", rowClues), ClueList("Blooms", bloomClues)),
                crosswordSolverSettings = crosswordSolverSettings)
    }

    private fun isAlphanumeric(ch : Char) = ch in 'A'..'Z' || ch in 'a'..'z' || ch in '0'..'9'

    private fun formatClue(entry: RowsGarden.Entry,
                           addWordCount: Boolean,
                           addHyphenated: Boolean): String {
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
        NONE,
        LIGHT,
        MEDIUM,
        DARK;

        companion object {
            fun forCoordinate(x: Int, y: Int): BloomType {
                var yOffset = 0
                if (x % 6 < 3) {
                    if (y == 0 || y == 11) {
                        return NONE
                    } else {
                        yOffset = 3
                    }
                }
                return when (floor((y + yOffset) / 2.0).roundToInt() % 3) {
                    0 -> MEDIUM
                    1 -> DARK
                    else -> LIGHT
                }
            }
        }
    }

    companion object {
        @JsName("parseRowsGarden")
        fun parseRowsGarden(rgz: ByteArray): Promise<JsRowsGarden> =
                GlobalScope.promise { JsRowsGarden(RowsGarden.parse(rgz)) }
    }
}