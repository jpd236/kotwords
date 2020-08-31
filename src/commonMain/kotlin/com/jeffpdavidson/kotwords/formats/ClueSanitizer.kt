package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.model.Crossword
import com.jeffpdavidson.kotwords.model.Square

private val CROSS_REFERENCE_PATTERN = "([1-9][0-9]*)-(?:(?!Across|Down).)*(Across|Down)".toRegex()

/** Utility to constrain clues to fit Across Lite format restrictions. */
object ClueSanitizer {
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
    internal fun sanitizeClues(
            grid: List<List<Square>>,
            givenSquareNumbers: Map<Pair<Int, Int>, Int>,
            acrossClues: Map<Int, String>,
            downClues: Map<Int, String>
    ):
            Pair<Map<Int, String>, Map<Int, String>> {
        val givenToSanitizedClueNumberMap =
                mapGivenToSanitizedClueNumbers(grid, givenSquareNumbers)
        val sanitizedAcrossClues: MutableMap<Int, String> = mutableMapOf()
        val sanitizedDownClues: MutableMap<Int, String> = mutableMapOf()
        Crossword.forEachNumberedSquare(grid) { x, y, clueNumber, isAcross, isDown ->
            val givenSquareNumber = givenSquareNumbers[x to y] ?: -1
            if (isAcross) {
                sanitizedAcrossClues[clueNumber] = sanitizeClue(
                        acrossClues[givenSquareNumber], givenToSanitizedClueNumberMap
                )
            }
            if (isDown) {
                sanitizedDownClues[clueNumber] = sanitizeClue(
                        downClues[givenSquareNumber], givenToSanitizedClueNumberMap
                )
            }
        }
        return Pair(sanitizedAcrossClues, sanitizedDownClues)
    }

    /** Generate a map from given clue numbers to clue numbers in the sanitized grid. */
    internal fun mapGivenToSanitizedClueNumbers(grid: List<List<Square>>, givenSquareNumbers: Map<Pair<Int, Int>, Int>):
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
    internal fun sanitizeClue(givenClue: String?, givenToSanitizedClueNumberMap: Map<Int, Int>): String {
        if (givenClue == null) {
            return "-"
        }
        val sanitizedClue = StringBuilder()
        var startIndex = 0
        var matchResult: MatchResult?
        do {
            matchResult = CROSS_REFERENCE_PATTERN.find(givenClue, startIndex)
            if (matchResult != null) {
                sanitizedClue.append(givenClue.substring(startIndex, matchResult.range.first))
                sanitizedClue.append(givenToSanitizedClueNumberMap[matchResult.groupValues[1].toInt()])
                startIndex = matchResult.range.first + matchResult.groupValues[1].length
            }
        } while (matchResult != null)
        sanitizedClue.append(givenClue.substring(startIndex))
        return sanitizedClue.toString().replace('★', '*')
    }
}
