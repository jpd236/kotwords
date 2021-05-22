package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.model.Crossword
import com.jeffpdavidson.kotwords.model.Square

private val CROSS_REFERENCE_PATTERN = "([1-9][0-9]*)-(?:(?!Across|Down).)*(Across|Down)".toRegex()

/** Utility to constrain clues to fit Across Lite format restrictions. */
internal object ClueSanitizer {
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
     * Characters that aren't supported in Windows-1252 must also be replaced with equivalents
     * which are.
     */
    fun sanitizeClues(
        grid: List<List<Square>>,
        acrossClues: Map<Int, String>,
        downClues: Map<Int, String>
    ): Pair<Map<Int, String>, Map<Int, String>> {
        // If at least one square is numbered, we apply renumbering logic. Otherwise, we assume that the puzzle uses
        // normal crossword numbering and avoid any numbering adjustment to the clues.
        val hasCustomNumbering = grid.find { row -> row.find { square -> square.number != null } != null } != null
        val givenToSanitizedClueNumberMap = if (hasCustomNumbering) mapGivenToSanitizedClueNumbers(grid) else null

        val sanitizedAcrossClues: MutableMap<Int, String> = mutableMapOf()
        val sanitizedDownClues: MutableMap<Int, String> = mutableMapOf()
        Crossword.forEachNumberedSquare(grid) { x, y, clueNumber, isAcross, isDown ->
            val givenSquareNumber = grid[y][x].number ?: if (hasCustomNumbering) -1 else clueNumber
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
    internal fun mapGivenToSanitizedClueNumbers(grid: List<List<Square>>): Map<Int, Int> {
        val givenToSanitizedClueNumberMap: MutableMap<Int, Int> = mutableMapOf()
        Crossword.forEachNumberedSquare(grid) { x, y, clueNumber, _, _ ->
            val givenSquareNumber = grid[y][x].number ?: -1
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
     * supported equivalents.
     */
    internal fun sanitizeClue(givenClue: String?, givenToSanitizedClueNumberMap: Map<Int, Int>?): String {
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
                        sanitizedClue.append(givenToSanitizedClueNumberMap[matchResult.groupValues[1].toInt()])
                        startIndex = matchResult.range.first + matchResult.groupValues[1].length
                    }
                } while (matchResult != null)
                sanitizedClue.append(givenClue.substring(startIndex))
                sanitizedClue.toString()
            }

        return substituteUnsupportedText(renumberedClue)
    }

    private val clueReplacements = mapOf(
        "</?b>" to "*",
        "</?i>" to "\"",
        "</?span>" to "",
        "ł" to "l",
        "[ăāạ]" to "a",
        "Ō" to "O",
        "[ęệ]" to "e",
        "Đ" to "D",
        "₀" to "0",
        "₁" to "1",
        "₂" to "2",
        "₃" to "3",
        "₄" to "4",
        "₅" to "5",
        "₆" to "6",
        "₇" to "7",
        "₈" to "8",
        "₉" to "9",
        "♯" to "#",
        "♭" to "b",
        "[Αα]" to "[Alpha]",
        "[Ββ]" to "[Beta]",
        "[Γγ]" to "[Gamma]",
        "[Δδ]" to "[Delta]",
        "[Εε]" to "[Epsilon]",
        "[Ζζ]" to "[Zeta]",
        "[Ηη]" to "[Eta]",
        "[Θθ]" to "[Theta]",
        "[Ιι]" to "[Iota]",
        "[Κκ]" to "[Kappa]",
        "[Λλ]" to "[Lambda]",
        "[Μμ]" to "[Mu]",
        "[Νν]" to "[Nu]",
        "[Ξξ]" to "[Xi]",
        "[Οο]" to "[Omicron]",
        "[Ππ]" to "[Pi]",
        "[Ρρ]" to "[Rho]",
        "[Σσς]" to "[Sigma]",
        "[Ττ]" to "[Tau]",
        "[Υυ]" to "[Upsilon]",
        "[Φφ]" to "[Phi]",
        "[Χχ]" to "[Chi]",
        "[Ψψ]" to "[Psi]",
        "[Ωω]" to "[Omega]",
        "★" to "*",
    ).map { (key, value) -> key.toRegex() to value }.toMap()

    private fun substituteUnsupportedText(text: String): String {
        // TODO(#2): Generalize accented character replacement.
        return clueReplacements.entries.fold(text) { clue, (from, to) ->
            clue.replace(from, to)
        }
    }
}
