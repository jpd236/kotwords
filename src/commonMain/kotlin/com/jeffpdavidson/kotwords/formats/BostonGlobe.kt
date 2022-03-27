package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.model.Crossword
import com.jeffpdavidson.kotwords.model.Puzzle

// Matches "TITLE Author"
private val CAPITALIZED_TITLE_REGEX = "([^a-z]+(?![a-z])) (.+)".toRegex()

/** Container for a puzzle in the Boston Globe HTML format. */
class BostonGlobe(private val html: String) : Puzzleable {
    override suspend fun asPuzzle(): Puzzle {
        val document = Xml.parse(html, format = DocumentFormat.HTML)

        val subHeaderText = document.selectFirst("p.subhed")?.text
            ?: throw InvalidFormatException("No sub header")
        val subHeader = parseSubHeader(subHeaderText)

        val gridElement = document.selectFirst("table#puzzle")
            ?: throw InvalidFormatException("No puzzle table")
        val squareMap = gridElement.select("td[data-coords]").associate {
            val coordinates = it.attr("data-coords").split(",").map(String::toInt)
            val solution = it.selectFirst("input[name]")?.attr("name")
                ?: throw InvalidFormatException("No input with name attribute")
            (coordinates[0] to coordinates[1]) to solution
        }
        val width = squareMap.keys.maxByOrNull { (x, _) -> x }!!.first
        val height = squareMap.keys.maxByOrNull { (_, y) -> y }!!.second
        val grid = (1..height).map { y ->
            (1..width).map { x ->
                val solution = squareMap[x to y]
                if (solution != null) {
                    Puzzle.Cell(solution = solution)
                } else {
                    Puzzle.Cell(cellType = Puzzle.CellType.BLOCK)
                }
            }.toList()
        }.toList()

        val acrossClues = toClueMap(document.select("div.clues-across label"))
        val downClues = toClueMap(document.select("div.clues-down label"))

        return Crossword(
            title = subHeader.title,
            creator = subHeader.author,
            copyright = subHeader.copyright,
            grid = grid,
            acrossClues = acrossClues,
            downClues = downClues
        ).asPuzzle()
    }

    private fun toClueMap(clueElements: Iterable<Element>): Map<Int, String> {
        return clueElements.associate {
            val clue = it.text!!
            clue.substringBefore(". ", clue).toInt() to clue.substringAfter(". ", clue)
        }.toMap()
    }

    companion object {
        internal data class SubHeader(
            val title: String,
            val author: String,
            val copyright: String,
        )

        internal fun parseSubHeader(subHeader: String): SubHeader {
            // Subheaders should be of the form: [Title + Author], [Copyright]
            val (titleAuthor, copyright) = if (subHeader.contains(", ")) {
                val parts = subHeader.split(", ", limit = 2)
                parts[0] to "\u00a9 ${parts[1]}"
            } else {
                // Just assume the whole thing is the title + author as a fallback.
                subHeader to ""
            }

            val (title, author) = if (titleAuthor.contains(" by ")) {
                // Format: [Title] by [Author]
                val parts = titleAuthor.split(" by ", limit = 2)
                parts[0] to parts[1]
            } else {
                // Format: [TITLE] [Author]
                val match = CAPITALIZED_TITLE_REGEX.matchEntire(titleAuthor)
                if (match != null) {
                    match.groupValues[1] to match.groupValues[2]
                } else {
                    // Just assume the whole thing is the title as a fallback.
                    titleAuthor to ""
                }
            }

            return SubHeader(title, author, copyright)
        }
    }
}