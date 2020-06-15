package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.Xml.getElementByTagName
import com.jeffpdavidson.kotwords.formats.Xml.getElementListByTagName
import com.jeffpdavidson.kotwords.model.BLACK_SQUARE
import com.jeffpdavidson.kotwords.model.Crossword
import com.jeffpdavidson.kotwords.model.Square
import org.w3c.dom.Element
import java.util.Locale

/** Container for a puzzle in the JPZ file format. */
class Jpz(private val xml: String) : Crosswordable {

    override fun asCrossword(): Crossword {
        val document = Xml.parseDocument(xml)

        val metadataElement = document.getElementByTagName("metadata")
        val title = metadataElement.getElementByTagName("title").textContent
        val creator = metadataElement.getElementByTagName("creator").textContent
        val copyright = metadataElement.getElementByTagName("copyright").textContent
        val description = metadataElement.getElementByTagName("description").textContent

        // Extract the grid by first building a map from point to square and then converting it to a
        // list-of-lists. We also extract the square numbers for grid sanitization later (see
        // sanitizeClues).
        val gridElement = document.getElementByTagName("grid")
        val width = gridElement.getAttribute("width").toInt()
        val height = gridElement.getAttribute("height").toInt()
        val cells = gridElement.getElementListByTagName("cell")
        val gridMap: MutableMap<Pair<Int, Int>, Square> = mutableMapOf()
        val givenSquareNumbers: MutableMap<Pair<Int, Int>, Int> = mutableMapOf()
        cells.forEach {
            val position = Pair(it.getAttribute("x").toInt() - 1, it.getAttribute("y").toInt() - 1)
            if (it.getAttribute("type") == "block") {
                gridMap[position] = BLACK_SQUARE
            } else {
                val solution = it.getAttribute("solution")
                val solutionRebus = if (solution.length > 1) solution else ""
                val isCircled =
                        "circle".equals(it.getAttribute("background-shape"), ignoreCase = true)
                gridMap[position] = Square(
                        solution = solution[0],
                        solutionRebus = solutionRebus,
                        isCircled = isCircled
                )
                if (it.hasAttribute("number")) {
                    givenSquareNumbers[position] = it.getAttribute("number").toInt()
                }
            }
        }
        val grid: MutableList<MutableList<Square>> = mutableListOf()
        for (y in 0 until height) {
            val row = mutableListOf<Square>()
            for (x in 0 until width) {
                row.add(gridMap[Pair(x, y)]!!)
            }
            grid.add(row)
        }

        val (acrossClues, downClues) =
                buildClueMaps(grid, document.getElementListByTagName("clues"), givenSquareNumbers)

        return Crossword(
                title = title,
                author = creator,
                copyright = copyright,
                notes = description,
                grid = grid,
                acrossClues = acrossClues,
                downClues = downClues
        )
    }

    companion object {
        /** Convert the given list of <clues> elements into across and down clues lists. */
        private fun buildClueMaps(
                grid: List<List<Square>>, clues: List<Element>,
                givenSquareNumbers: Map<Pair<Int, Int>, Int>
        ):
                Pair<Map<Int, String>, Map<Int, String>> {
            // Create a map from clue list title to the list of <clue> elements under that title.
            val clueGroups = clues.filter { it.getElementListByTagName("title").isNotEmpty() }
                    .map {
                        val clueListTitle =
                                it.getElementByTagName("title")
                                        .textContent.trim().toLowerCase(Locale.US)
                        val clueList = it.getElementListByTagName("clue")
                        clueListTitle to clueList
                    }.toMap()

            // Convert the <clue> element lists for across/down clues into the expected map format.
            val acrossClues = clueGroups["across"]!!
                    .map { it.getAttribute("number").toInt() to it.textContent }.toMap()
            val downClues = clueGroups["down"]!!
                    .map { it.getAttribute("number").toInt() to it.textContent }.toMap()

            // Sanitize the clue numbers/clues to be Across Lite compatible.
            return ClueSanitizer.sanitizeClues(grid, givenSquareNumbers, acrossClues, downClues)
        }
    }
}
