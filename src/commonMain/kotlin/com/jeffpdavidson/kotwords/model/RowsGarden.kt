package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.formats.Puzzleable
import kotlinx.serialization.Serializable
import kotlin.math.floor
import kotlin.math.roundToInt

@Serializable
data class RowsGarden(
    val title: String,
    val creator: String,
    val copyright: String,
    val description: String,
    val rows: List<List<Entry>>,
    val light: List<Entry>,
    val medium: List<Entry>,
    val dark: List<Entry>,
    val lightBloomColor: String = "#FFFFFF",
    val mediumBloomColor: String = "#C3C8FA",
    val darkBloomColor: String = "#5765F7",
    val addWordCount: Boolean = true,
    val addHyphenated: Boolean = true,
    val hasHtmlClues: Boolean = false,
) : Puzzleable() {
    @Serializable
    data class Entry(val clue: String, val answer: String)

    override suspend fun createPuzzle(): Puzzle {
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
            val row = if (y == 0 || y == rowLetters.size - 1) {
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
            row.toList()
        }

        return Companion.createPuzzle(
            title = title,
            creator = creator,
            copyright = copyright,
            description = description,
            solutionGrid = solutionGrid,
            rows = rows,
            light = light,
            medium = medium,
            dark = dark,
            lightColor = lightBloomColor,
            mediumColor = mediumBloomColor,
            darkColor = darkBloomColor,
            addWordCount = addWordCount,
            addHyphenated = addHyphenated,
            hasHtmlClues = hasHtmlClues,
            labelBlooms = true,
            rowsListTitle = "Rows",
            bloomsListTitle = "Blooms",
            emptyCellType = Puzzle.CellType.BLOCK,
            bloomTypeForCoordinate = { x, y ->
                val baseOffset = if (x % 6 < 3) 3 else 0
                val yOffset = if (hasShortFirstRow) baseOffset else baseOffset + 1
                when (floor((y + yOffset) / 2.0).roundToInt() % 3) {
                    0 -> BloomType.MEDIUM
                    1 -> BloomType.DARK
                    else -> BloomType.LIGHT
                }
            },
            isStartOfBloom = { x, y ->
                val yOffset = if (hasShortFirstRow) 0 else 1
                ((y + yOffset) % 2 == 0 && x % 6 == 5) || ((y + yOffset) % 2 == 1 && x % 6 == 2)
            },
            getBloomCoordinates = { x, y ->
                listOf(
                    Puzzle.Coordinate(x = x - 2, y = y),
                    Puzzle.Coordinate(x = x - 1, y),
                    Puzzle.Coordinate(x = x, y = y),
                    Puzzle.Coordinate(x = x, y = y + 1),
                    Puzzle.Coordinate(x = x - 1, y = y + 1),
                    Puzzle.Coordinate(x = x - 2, y = y + 1),
                )
            },
        )
    }

    internal enum class BloomType {
        LIGHT,
        MEDIUM,
        DARK,
    }

    companion object {
        internal fun createPuzzle(
            title: String,
            creator: String,
            copyright: String,
            description: String,
            solutionGrid: List<List<Char>>,
            rows: List<List<Entry>>,
            light: List<Entry>,
            medium: List<Entry>,
            dark: List<Entry>,
            lightColor: String,
            mediumColor: String,
            darkColor: String,
            addWordCount: Boolean,
            addHyphenated: Boolean,
            hasHtmlClues: Boolean,
            labelBlooms: Boolean,
            rowsListTitle: String,
            bloomsListTitle: String,
            emptyCellType: Puzzle.CellType,
            bloomTypeForCoordinate: (Int, Int) -> BloomType,
            isStartOfBloom: (Int, Int) -> Boolean,
            getBloomCoordinates: (Int, Int) -> List<Puzzle.Coordinate>,
        ): Puzzle {
            // Loop over the grid, adding the Puzzle clues for each bloom as we encounter them.
            data class BloomData(
                val color: String,
                val clues: List<Entry>,
                val puzzleClues: MutableList<Puzzle.Clue> = mutableListOf()
            )

            val blooms = mapOf(
                BloomType.LIGHT to BloomData(lightColor, light),
                BloomType.MEDIUM to BloomData(mediumColor, medium),
                BloomType.DARK to BloomData(darkColor, dark)
            )
            val bloomsWords = mutableListOf<Puzzle.Word>()

            val grid = solutionGrid.indices.map { y ->
                solutionGrid[y].indices.map { x ->
                    val bloomType = bloomTypeForCoordinate(x, y)

                    if (solutionGrid[y][x] == '.') {
                        Puzzle.Cell(cellType = emptyCellType)
                    } else {
                        var bloomIndex = 0
                        val number = if (x == solutionGrid[y].indexOfFirst { it != '.' }) {
                            // Start of a row
                            "${'A' + y}"
                        } else {
                            ""
                        }
                        val topRightNumber = if (isStartOfBloom(x, y)) {
                            // Start of a bloom
                            bloomIndex = blooms.getValue(bloomType).puzzleClues.size + 1
                            "${bloomType.name[0]}${bloomIndex}"
                        } else {
                            ""
                        }
                        val cell = Puzzle.Cell(
                            number = number,
                            topRightNumber = if (labelBlooms) topRightNumber else "",
                            solution = "${solutionGrid[y][x]}",
                            backgroundColor = blooms.getValue(bloomType).color
                        )
                        if (bloomIndex > 0) {
                            val wordId = 1000 * (bloomType.ordinal + 1) + bloomIndex
                            bloomsWords.add(Puzzle.Word(wordId, getBloomCoordinates(x, y)))
                            blooms[bloomType]?.puzzleClues!!.add(
                                Puzzle.Clue(
                                    wordId,
                                    if (labelBlooms) topRightNumber else "${topRightNumber[0]}",
                                    formatClue(
                                        blooms.getValue(bloomType).clues[bloomIndex - 1],
                                        addWordCount, addHyphenated
                                    )
                                )
                            )
                        }
                        cell
                    }
                }
            }

            val (rowClues, rowsWords) = grid.mapIndexed { y, row ->
                val letters = row.mapIndexedNotNull { x, cell ->
                    if (cell.cellType == emptyCellType) null else Puzzle.Coordinate(x = x, y = y)
                }
                Puzzle.Clue(y + 1, "${'A' + y}", rows[y].joinToString(" / ") {
                    formatClue(it, addWordCount, addHyphenated)
                }) to Puzzle.Word(y + 1, letters)
            }.unzip()

            fun orderBloomClues(clues: List<Puzzle.Clue>) =
                if (labelBlooms) clues else clues.sortedBy { it.text }

            val bloomClues = listOf(
                orderBloomClues(blooms.getValue(BloomType.LIGHT).puzzleClues),
                orderBloomClues(blooms.getValue(BloomType.MEDIUM).puzzleClues),
                orderBloomClues(blooms.getValue(BloomType.DARK).puzzleClues),
            ).flatten()

            return Puzzle(
                title = title,
                creator = creator,
                copyright = copyright,
                description = description,
                grid = grid,
                clues = listOf(
                    Puzzle.ClueList(if (hasHtmlClues) "<b>$rowsListTitle</b>" else rowsListTitle, rowClues),
                    Puzzle.ClueList(if (hasHtmlClues) "<b>$bloomsListTitle</b>" else bloomsListTitle, bloomClues)
                ),
                words = rowsWords + (if (labelBlooms) bloomsWords.sortedBy { it.id } else listOf()),
                hasHtmlClues = hasHtmlClues,
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
    }
}