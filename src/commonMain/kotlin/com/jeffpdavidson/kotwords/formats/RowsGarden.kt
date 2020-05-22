package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.model.Puzzle
import kotlinx.io.charsets.Charsets
import kotlinx.io.core.String
import kotlinx.serialization.Serializable
import net.mamoe.yamlkt.Yaml
import kotlin.js.JsName
import kotlin.math.floor
import kotlin.math.roundToInt

@Serializable
data class RowsGarden(
        val title: String,
        val author: String,
        val copyright: String,
        val notes: String = "",
        val rows: List<List<Entry>>,
        val light: List<Entry>,
        val medium: List<Entry>,
        val dark: List<Entry>) {
    @Serializable
    data class Entry(val clue: String, val answer: String)

    @JsName("asPuzzle")
    fun asPuzzle(
            lightBloomColor: String,
            mediumBloomColor: String,
            darkBloomColor: String,
            addWordCount: Boolean,
            addHyphenated: Boolean,
            crosswordSolverSettings: Puzzle.CrosswordSolverSettings): Puzzle {
        require(rows.size == 12) {
            "Only 21x12 grids are supported"
        }
        // solutionGrid[y][x] = the solution letter at (x, y)
        val solutionGrid = rows.mapIndexed { y, words ->
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

        // Loop over the grid, adding the Puzzle clues for each bloom as we encounter them.
        data class BloomData(
                val color: String,
                val clues: List<Entry>,
                val puzzleClues: MutableList<Puzzle.Clue> = mutableListOf())

        val blooms = mapOf(
                BloomType.LIGHT to BloomData(lightBloomColor, light),
                BloomType.MEDIUM to BloomData(mediumBloomColor, medium),
                BloomType.DARK to BloomData(darkBloomColor, dark))
        val cellMap = mutableMapOf<Pair<Int, Int>, Puzzle.Cell>()
        fun getOrCreateCell(x: Int, y: Int): Puzzle.Cell {
            if (cellMap.containsKey(x to y)) {
                return cellMap.getValue(x to y)
            }
            val bloomType = BloomType.forCoordinate(x, y)
            cellMap[x to y] =
                    if (bloomType == BloomType.NONE) {
                        Puzzle.Cell(x + 1, y + 1, cellType = Puzzle.CellType.BLOCK)
                    } else {
                        var bloomIndex = 0
                        val number = when {
                            ((y == 0 || y == 11) && x == 3) || ((y != 0 && y != 11) && x == 0) -> {
                                // Start of a row
                                "${'A' + y}"
                            }
                            (y % 2 == 0 && x % 6 == 5) || (y % 2 == 1 && x % 6 == 2) -> {
                                // Start of a bloom
                                bloomIndex = blooms.getValue(bloomType).puzzleClues.size + 1
                                "${bloomType.name[0]}${bloomIndex}"
                            }
                            else -> ""
                        }
                        val cell = Puzzle.Cell(x + 1, y + 1,
                                number = number,
                                solution = "${solutionGrid[y][x]}",
                                backgroundColor = blooms.getValue(bloomType).color)
                        if (bloomIndex > 0) {
                            blooms[bloomType]?.puzzleClues!!.add(Puzzle.Clue(
                                    Puzzle.Word(1000 * bloomType.ordinal + bloomIndex, listOf(
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
            val letters = row.filter { it.cellType != Puzzle.CellType.BLOCK }
            Puzzle.Clue(Puzzle.Word(y + 1, letters), "${y + 1}", rows[y].joinToString(" / ") {
                formatClue(it, addWordCount, addHyphenated)
            })
        }

        val bloomClues = listOf(
                blooms.getValue(BloomType.LIGHT).puzzleClues,
                blooms.getValue(BloomType.MEDIUM).puzzleClues,
                blooms.getValue(BloomType.DARK).puzzleClues)
                .flatten()

        return Puzzle(
                title = title,
                creator = author,
                copyright = copyright.replace("(c)", "©"),
                description = notes,
                grid = grid,
                clues = listOf(Puzzle.ClueList("Rows", rowClues), Puzzle.ClueList("Blooms", bloomClues)),
                crosswordSolverSettings = crosswordSolverSettings)
    }

    private fun isAlphanumeric(ch: Char) = ch in 'A'..'Z' || ch in 'a'..'z' || ch in '0'..'9'

    private fun formatClue(entry: Entry,
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
        @JsName("fromRawInput")
        fun fromRawInput(
                title: String,
                author: String,
                copyright: String,
                notes: String,
                rowClues: String,
                rowAnswers: String,
                lightClues: String,
                lightAnswers: String,
                mediumClues: String,
                mediumAnswers: String,
                darkClues: String,
                darkAnswers: String): RowsGarden {
            return RowsGarden(
                    title = title.trim(),
                    author = author.trim(),
                    copyright = copyright.trim(),
                    notes = notes.trim(),
                    rows = rowClues.trim().split("\n").zip(rowAnswers.trim().split("\n"))
                            .map { (clues, answers) ->
                                clues.trim().split("/")
                                        .zip(answers.trim().split("/"))
                                        .map { (clue, answer) -> Entry(clue.trim(), answer.trim()) }
                            },
                    light = lightClues.trim().split("\n").zip(lightAnswers.trim().split("\n"))
                            .map { (clue, answer) -> Entry(clue.trim(), answer.trim()) },
                    medium = mediumClues.trim().split("\n").zip(mediumAnswers.trim().split("\n"))
                            .map { (clue, answer) -> Entry(clue.trim(), answer.trim()) },
                    dark = darkClues.trim().split("\n").zip(darkAnswers.trim().split("\n"))
                            .map { (clue, answer) -> Entry(clue.trim(), answer.trim()) })
        }

        suspend fun parse(rgz: ByteArray): RowsGarden {
            val rg =
                    try {
                        Zip.unzip(rgz)
                    } catch (e: InvalidZipException) {
                        // Try as a plain-text file.
                        rgz
                    }
            return parseRg(String(rg, charset = Charsets.UTF_8))
        }

        private fun parseRg(rg: String): RowsGarden {
            return Yaml.default.parse(serializer(), fixInvalidYamlValues(rg))
        }

        /**
         * Fix invalid values in the provided YAML text.
         *
         * .rg files often contain invalid YAML due to values containing reserved characters. This
         * method attempts to adds quotes around any value, escaping existing quotes as needed.
         */
        private fun fixInvalidYamlValues(yaml: String): String {
            return yaml.lines().joinToString("\n") { line ->
                // Find a value on this line - anything following a ":", ignoring any whitespace at
                // the beginning and end.
                val valuePattern = ":\\s*([^\\s].+[^\\s])\\s*".toRegex()
                val result = valuePattern.find(line)
                if (result == null) {
                    // Nothing found; insert the line as is.
                    line
                } else {
                    // Insert the line with the value surrounded by quotes and any existing quotes
                    // escaped.
                    val escapedValue = result.groupValues[1].replace("\"", "\\\"")
                    line.replace(
                            ":\\s*.+".toRegex(),
                            Regex.escapeReplacement(": \"$escapedValue\""))
                }
            }
        }
    }
}