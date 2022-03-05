package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.unidecode.Unidecode
import com.jeffpdavidson.kotwords.model.Crossword
import com.jeffpdavidson.kotwords.model.Puzzle

private val CROSS_REFERENCE_PATTERN = "([1-9][0-9]*)-(?:(?!Across|Down).)*(Across|Down)".toRegex()

/** Utility to constrain clues and metadata to fit Across Lite format restrictions. */
internal object AcrossLiteSanitizer {
    /**
     * Sanitize the given across/down clues to handle format differences with Across Lite.
     *
     * Richer crossword formats like JPZ support two features that Across Lite doesn't:
     * 1. Clue numbers may be omitted from squares where they should be present, or present in
     *    squares where they should be absent. In these cases, we need to renumber the grid per
     *    normal rules and update the clue mappings. Any clue which references another clue will
     *    also need to be updated to point to the new location.
     * 2. Clues may be omitted where they should be present. Any missing clues are set to a
     *    dummy "-".
     *
     * If [sanitizeCharacters] is true, characters that aren't supported in ISO-8859-1 are replaced
     * with equivalents which are.
     */
    fun sanitizeClues(
        grid: List<List<Puzzle.Cell>>,
        acrossClues: Puzzle.ClueList,
        downClues: Puzzle.ClueList,
        sanitizeCharacters: Boolean,
    ): Pair<Map<Int, String>, Map<Int, String>> {
        var hasCustomNumbering = false
        Crossword.forEachCell(grid) { x, y, clueNumber, _, _, _ ->
            hasCustomNumbering = hasCustomNumbering || grid[y][x].number != (clueNumber?.toString() ?: "")
        }
        val givenToSanitizedClueNumberMap = if (hasCustomNumbering) mapGivenToSanitizedClueNumbers(grid) else null

        val acrossCluesByClueNumber = acrossClues.clues.associate { it.number to it.text }
        val downCluesByClueNumber = downClues.clues.associate { it.number to it.text }
        val sanitizedAcrossClues: MutableMap<Int, String> = mutableMapOf()
        val sanitizedDownClues: MutableMap<Int, String> = mutableMapOf()
        Crossword.forEachNumberedCell(grid) { x, y, clueNumber, isAcross, isDown ->
            val givenSquareNumber = grid[y][x].number.ifEmpty { if (hasCustomNumbering) "" else clueNumber.toString() }
            if (isAcross) {
                sanitizedAcrossClues[clueNumber] = sanitizeClue(
                    acrossCluesByClueNumber[givenSquareNumber], givenToSanitizedClueNumberMap, sanitizeCharacters
                )
            }
            if (isDown) {
                sanitizedDownClues[clueNumber] = sanitizeClue(
                    downCluesByClueNumber[givenSquareNumber], givenToSanitizedClueNumberMap, sanitizeCharacters
                )
            }
        }
        return Pair(sanitizedAcrossClues, sanitizedDownClues)
    }

    /** Generate a map from given clue numbers to clue numbers in the sanitized grid. */
    internal fun mapGivenToSanitizedClueNumbers(grid: List<List<Puzzle.Cell>>): Map<String, String> {
        val givenToSanitizedClueNumberMap: MutableMap<String, String> = mutableMapOf()
        Crossword.forEachNumberedCell(grid) { x, y, clueNumber, _, _ ->
            val givenSquareNumber = grid[y][x].number
            if (givenSquareNumber.isNotEmpty()) {
                givenToSanitizedClueNumberMap[givenSquareNumber] = clueNumber.toString()
            }
        }
        return givenToSanitizedClueNumberMap
    }

    /**
     * Sanitize the given clue.
     *
     * Missing clues are set to "-", and cross references are updated to reflect any shifted
     * clue numbers in the sanitized grid. If sanitizeCharacters is true, invalid characters in
     * ISO-8859-1 are replaced with supported equivalents.
     */
    internal fun sanitizeClue(
        givenClue: String?,
        givenToSanitizedClueNumberMap: Map<String, String>?,
        sanitizeCharacters: Boolean,
    ): String {
        if (givenClue == null) {
            return "-"
        }
        val renumberedClue =
            if (givenToSanitizedClueNumberMap == null) {
                givenClue
            } else {
                val sanitizedClue = StringBuilder()
                var startIndex = 0
                var matchResult: MatchResult?
                do {
                    matchResult = CROSS_REFERENCE_PATTERN.find(givenClue, startIndex)
                    if (matchResult != null) {
                        sanitizedClue.append(givenClue.substring(startIndex, matchResult.range.first))
                        sanitizedClue.append(givenToSanitizedClueNumberMap[matchResult.groupValues[1]])
                        startIndex = matchResult.range.first + matchResult.groupValues[1].length
                    }
                } while (matchResult != null)
                sanitizedClue.append(givenClue.substring(startIndex))
                sanitizedClue.toString()
            }

        return substituteUnsupportedText(renumberedClue, sanitizeCharacters).trim()
    }

    private val htmlClueReplacements = mapOf(
        "</?b>" to "*",
        "</?i>" to "\"",
        "</?sub>" to "",
        "</?sup>" to "",
        "</?span>" to "",
        "&amp;" to "&",
        "&lt;" to "<",
    )

    fun substituteUnsupportedText(text: String, sanitizeCharacters: Boolean): String {
        val withoutHtml = htmlClueReplacements
            .map { (key, value) -> key.toRegex() to value }
            .fold(text) { clue, (from, to) ->
                clue.replace(from, to)
            }
        if (!sanitizeCharacters) {
            return withoutHtml
        }
        return Unidecode.unidecode(withoutHtml)
    }
}
