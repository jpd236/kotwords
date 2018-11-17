package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.Xml.getElementByTagName
import com.jeffpdavidson.kotwords.formats.Xml.getElementListByTagName
import com.jeffpdavidson.kotwords.model.BLACK_SQUARE
import com.jeffpdavidson.kotwords.model.Crossword
import com.jeffpdavidson.kotwords.model.Square
import org.w3c.dom.Element
import java.util.Locale

private val CROSS_REFERENCE_PATTERN = "([1-9][0-9]*)-(?:(?!Across|Down).)*(Across|Down)".toRegex()

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
                        isCircled = isCircled)
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
                downClues = downClues)
    }

    companion object {
        /** Convert the given list of <clues> elements into across and down clues lists. */
        private fun buildClueMaps(grid: List<List<Square>>, clues: List<Element>,
                                  givenSquareNumbers: Map<Pair<Int, Int>, Int>):
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
            return sanitizeClues(grid, givenSquareNumbers, acrossClues, downClues)
        }

        /**
         * Sanitize the given across/down clues to handle format differences with Across Lite.
         *
         * The JPZ format supports two features that Across Lite doesn't:
         * 1. Clue numbers may be omitted from squares where they should be present, or present in
         *    squares where they should be absent. In these cases, we need to renumber the grid per
         *    normal rules and update the clue mappings. Any clue which references another clue will
         *    also need to be updated to point to the new location.
         * 2. Clues may be omitted where they should be present. Any missing clues are set to a
         *    dummy "-".
         *
         * Characters that aren't encodable in Windows-1252 must also be replaced with equivalents
         * which are.
         */
        private fun sanitizeClues(grid: List<List<Square>>,
                                  givenSquareNumbers: Map<Pair<Int, Int>, Int>,
                                  acrossClues: Map<Int, String>,
                                  downClues: Map<Int, String>):
                Pair<Map<Int, String>, Map<Int, String>> {
            val givenToSanitizedClueNumberMap =
                    mapGivenToSanitizedClueNumbers(grid, givenSquareNumbers)
            val sanitizedAcrossClues: MutableMap<Int, String> = mutableMapOf()
            val sanitizedDownClues: MutableMap<Int, String> = mutableMapOf()
            Crossword.forEachNumberedSquare(grid) { x, y, clueNumber, isAcross, isDown ->
                val givenSquareNumber = givenSquareNumbers[x to y] ?: -1
                if (isAcross) {
                    sanitizedAcrossClues[clueNumber] = sanitizeClue(
                            acrossClues[givenSquareNumber], givenToSanitizedClueNumberMap)
                }
                if (isDown) {
                    sanitizedDownClues[clueNumber] = sanitizeClue(
                            downClues[givenSquareNumber], givenToSanitizedClueNumberMap)
                }
            }
            return Pair(sanitizedAcrossClues, sanitizedDownClues)
        }

        /** Generate a map from given clue numbers to clue numbers in the sanitized grid. */
        internal fun mapGivenToSanitizedClueNumbers(grid: List<List<Square>>,
                                                    givenSquareNumbers: Map<Pair<Int, Int>, Int>):
                Map<Int, Int> {
            val givenToSanitizedClueNumberMap: MutableMap<Int, Int> = mutableMapOf()
            Crossword.forEachNumberedSquare(grid) { x, y, clueNumber, _, _ ->
                val givenSquareNumber = givenSquareNumbers[x to y] ?: -1
                if (givenSquareNumber != -1) {
                    givenToSanitizedClueNumberMap[givenSquareNumber] = clueNumber
                }
            }
            return givenToSanitizedClueNumberMap
        }

        /**
         * Sanitize the given clue.
         *
         * Missing clues are set to "-", and cross references are updated to reflect any shifted
         * clue numbers in the sanitized grid. Invalid characters in Windows-1252 are replaced with
         * encodable equivalents.
         */
        internal fun sanitizeClue(givenClue: String?, givenToSanitizedClueNumberMap: Map<Int, Int>):
                String {
            if (givenClue == null) {
                return "-"
            }
            val sanitizedClue = StringBuilder()
            var startIndex = 0
            var matchResult: MatchResult?
            do {
                matchResult = CROSS_REFERENCE_PATTERN.find(givenClue, startIndex)
                if (matchResult != null) {
                    sanitizedClue.append(givenClue.substring(startIndex, matchResult.range.start))
                    sanitizedClue.append(
                            givenToSanitizedClueNumberMap[matchResult.groupValues[1].toInt()])
                    startIndex = matchResult.groups[1]!!.range.endInclusive + 1
                }
            } while (matchResult != null)
            sanitizedClue.append(givenClue.substring(startIndex))
            return sanitizedClue.toString().replace('★', '*')
        }
    }
}
