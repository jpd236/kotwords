package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.model.BLACK_SQUARE
import com.jeffpdavidson.kotwords.model.Crossword
import com.jeffpdavidson.kotwords.model.Square
import org.jsoup.Jsoup
import org.jsoup.select.Elements

// Matches "TITLE Author, Copyright"
private val SUB_HEADER_REGEX = "([^a-z]+(?![a-z])) ([^,]+), (.+)".toRegex()

/** Container for a puzzle in the Boston Globe HTML format. */
class BostonGlobe(private val html: String) : Crosswordable {
    override fun asCrossword(): Crossword {
        val document = Jsoup.parse(html)

        val subHeader = document.selectFirst("p.subhed").text()
        val subHeaderMatch = SUB_HEADER_REGEX.matchEntire(subHeader)
                ?: throw InvalidFormatException("Invalid sub header: $subHeader")
        val title = subHeaderMatch.groupValues[1]
        val author = subHeaderMatch.groupValues[2]
        val copyright = "\u00a9 ${subHeaderMatch.groupValues[3]}"

        val gridElement = document.selectFirst("table#puzzle")
        val squareMap = gridElement.select("td[data-coords]").map {
            val coordinates = it.attr("data-coords").split(",").map(String::toInt)
            val solution = it.selectFirst("input[name]").attr("name")
            (coordinates[0] to coordinates[1]) to solution
        }.toMap()
        val width = squareMap.keys.maxBy { (x, _) -> x }!!.first
        val height = squareMap.keys.maxBy { (_, y) -> y }!!.second
        val grid = (1..height).map { y ->
            (1..width).map { x ->
                if (squareMap.containsKey(x to y)) {
                    val solution = squareMap[x to y]!!
                    val solutionRebus = if (solution.length > 1) { solution } else { "" }
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
                downClues = downClues)
    }

    private fun toClueMap(clueElements: Elements): Map<Int, String> {
        return clueElements.map {
            val clue = it.text()
            clue.substringBefore(". ", clue).toInt() to clue.substringAfter(". ", clue)
        }.toMap()
    }
}