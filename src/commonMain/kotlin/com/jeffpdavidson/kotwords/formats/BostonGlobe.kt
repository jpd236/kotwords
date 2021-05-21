package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.model.BLACK_SQUARE
import com.jeffpdavidson.kotwords.model.Crossword
import com.jeffpdavidson.kotwords.model.Square

// Matches "TITLE Author, Copyright"
private val SUB_HEADER_REGEX = "([^a-z]+(?![a-z])) ([^,]+), (.+)".toRegex()

/** Container for a puzzle in the Boston Globe HTML format. */
class BostonGlobe(private val html: String) : Crosswordable {
    override fun asCrossword(): Crossword {
        val document = Html.parse(html)

        val subHeader = document.selectFirst("p.subhed")?.text
            ?: throw InvalidFormatException("No sub header")
        val subHeaderMatch = SUB_HEADER_REGEX.matchEntire(subHeader)
            ?: throw InvalidFormatException("Invalid sub header: $subHeader")
        val title = subHeaderMatch.groupValues[1]
        val author = subHeaderMatch.groupValues[2]
        val copyright = "\u00a9 ${subHeaderMatch.groupValues[3]}"

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
                    val solutionRebus = if (solution.length > 1) {
                        solution
                    } else {
                        ""
                    }
                    Square(solution = solution[0], solutionRebus = solutionRebus)
                } else {
                    BLACK_SQUARE
                }
            }.toList()
        }.toList()

        val acrossClues = toClueMap(document.select("div.clues-across label"))
        val downClues = toClueMap(document.select("div.clues-down label"))

        return Crossword(
            title = title,
            author = author,
            copyright = copyright,
            grid = grid,
            acrossClues = acrossClues,
            downClues = downClues
        )
    }

    private fun toClueMap(clueElements: Iterable<Element>): Map<Int, String> {
        return clueElements.associate {
            val clue = it.text!!
            clue.substringBefore(". ", clue).toInt() to clue.substringAfter(". ", clue)
        }.toMap()
    }
}