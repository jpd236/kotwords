package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.json.JsonSerializer
import com.jeffpdavidson.kotwords.formats.json.TelegraphJson
import com.jeffpdavidson.kotwords.model.Puzzle

private val HIDE_FORMAT_VARIANTS = setOf(
    "The Cross Atlantic",
    "Cross Atlantic",
    "Mini Crossword",
)

/** Container for a puzzle in the Telegraph JSON format. */
class Telegraph(
    private val json: String,
    private val copyright: String = "",
) : DelegatingPuzzleable() {

    override suspend fun getPuzzleable(): Puzzleable {
        val response = JsonSerializer.fromJson<TelegraphJson.Response>(json)
        val data = response.json

        val hideFormat = HIDE_FORMAT_VARIANTS.contains(data.meta?.variant)

        val wordSolutions = mutableMapOf<Pair<Int, Int>, String>()
        val words = data.copy.words.map { word ->
            val xRange = parseRange(word.x)
            val yRange = parseRange(word.y)
            val cells = if (xRange.count() > 1) {
                val y = yRange.first - 1
                xRange.map { x -> Puzzle.Coordinate(x = x - 1, y = y) }
            } else if (yRange.count() > 1) {
                val x = xRange.first - 1
                yRange.map { y -> Puzzle.Coordinate(x = x, y = y - 1) }
            } else {
                listOf(Puzzle.Coordinate(x = xRange.first - 1, y = yRange.first - 1))
            }
            if (!word.solution.isNullOrEmpty()) {
                cells.forEachIndexed { i, coordinate ->
                    if (i < word.solution.length) {
                        wordSolutions[coordinate.x to coordinate.y] = "${word.solution[i]}"
                    }
                }
            }
            Puzzle.Word(
                id = word.id,
                cells = cells,
            )
        }

        val grid = data.grid.mapIndexed { y, row ->
            row.mapIndexed { x, square ->
                if (square.blank.equals("blank", ignoreCase = true)) {
                    Puzzle.Cell(cellType = Puzzle.CellType.BLOCK)
                } else {
                    val solution = square.letter.ifEmpty { wordSolutions[x to y] ?: "" }
                    val backgroundColor = if (square.shaded) "#dcdcdc" else ""
                    Puzzle.Cell(
                        solution = solution,
                        backgroundColor = backgroundColor,
                        number = square.number,
                    )
                }
            }
        }

        val clues = data.copy.clues.map { section ->
            val clueList = section.clues.map { clue ->
                Puzzle.Clue(
                    wordId = clue.word,
                    number = "${clue.number}",
                    text = toHtml(clue.clue),
                    format = if (hideFormat) "" else clue.format,
                )
            }
            val title =
                if (data.meta?.variant == "Mini Cryptic" && section.title.equals("Down", ignoreCase = true)) {
                    "Down (cryptic)"
                } else {
                    section.title
                }
            Puzzle.ClueList(
                title = "<b>$title</b>",
                clues = clueList,
            )
        }

        val title = toHtml(data.copy.title.trim().ifEmpty { data.meta?.variant ?: "" })
        val creator = toHtml(
            listOfNotNull(
                data.copy.setter.ifBlank { null },
                data.copy.byline.ifBlank { null },
                data.meta?.author?.ifBlank { null },
            ).firstOrNull() ?: ""
        )
        val description = toHtml(
            listOfNotNull(
                data.copy.description.ifBlank { null },
                data.meta?.description?.ifBlank { null },
            ).firstOrNull() ?: ""
        )

        return Puzzle(
            title = title,
            creator = creator,
            copyright = copyright,
            description = description,
            grid = grid,
            clues = clues,
            words = words,
            hasHtmlClues = true,
        )
    }

    companion object {
        private fun parseRange(rangeStr: String): IntRange {
            return if (rangeStr.contains("-")) {
                val parts = rangeStr.split("-", limit = 2)
                parts[0].toInt()..parts[1].toInt()
            } else {
                val value = rangeStr.toInt()
                value..value
            }
        }

        private fun toHtml(clue: String): String {
            return Encodings.decodeHtmlEntities(clue)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace("&lt;(/?(?:b|i))>".toRegex(RegexOption.IGNORE_CASE), "<$1>")
        }
    }
}
