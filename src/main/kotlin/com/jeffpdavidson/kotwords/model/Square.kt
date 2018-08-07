package com.jeffpdavidson.kotwords.model

/**
 * A square in the grid of a [Crossword].
 *
 * Black squares will have [isBlack] set to true and will equal [BLACK_SQUARE]; the other parameters
 * are undefined in this case. White squares will have a valid solution character.
 *
 * While this is a generic container, it only represents features supported by Across Lite.
 *
 * @constructor Create a new white Square. (For black squares, use [BLACK_SQUARE]).
 * @param isBlack Whether the square is black.
 * @param solution The solution as a single character. The only permitted characters are upper-case
 *                 letters, numbers, and the characters '@', '#', '$', '%', '&', '+', and '?', as
 *                 these are the only characters that can be inserted by the user in Across Lite
 *                 (see [the text format specification](
 *                 http://www.litsoft.com/across/docs/AcrossTextFormat.pdf)).
 * @param solutionRebus An optional alternative solution representing a full rebus entry (multiple
 *                      letters in a single square). Only upper-case letters and numbers are
 *                      permitted, and the maximum length is 8.
 * @param isCircled Whether the square contains a circle.
 */
data class Square(
        val solution: Char?,
        val isBlack: Boolean = false,
        val solutionRebus: String = "",
        val isCircled: Boolean = false) {
    private val validSymbols = setOf('@', '#', '$', '%', '&', '+', '?')

    init {
        if (isBlack) {
            require(solution == null && solutionRebus == "" && !isCircled) {
                "Black squares must not set other properties"
            }
        } else {
            require(isValidCharacter()) {
                "Unsupported solution character: $solution"
            }
        }
        require(solutionRebus.length <= 8 && !solutionRebus.any { !isValidCharacter() }) {
            "Invalid rebus: $solutionRebus"
        }
    }

    private fun isValidCharacter(): Boolean {
        if (solution!!.isLetterOrDigit()) {
            return !solution.isLetter() || solution.isUpperCase()
        }
        return validSymbols.contains(solution)
    }
}

/** Constant representing a black square. */
val BLACK_SQUARE = Square(isBlack = true, solution = null)
