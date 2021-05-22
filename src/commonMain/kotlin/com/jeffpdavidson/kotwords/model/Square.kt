package com.jeffpdavidson.kotwords.model

/**
 * A square in the grid of a [Crossword].
 *
 * Black squares will have [isBlack] set to true and will equal [BLACK_SQUARE]; the other parameters
 * are undefined in this case. White squares will have a valid solution character.
 *
 * @constructor Create a new white Square. (For black squares, use [BLACK_SQUARE]).
 * @param isBlack Whether the square is black.
 * @param solution The solution as a single character.
 * @param solutionRebus An optional alternative solution representing a full rebus entry (multiple
 *                      letters in a single square). Only upper-case letters and numbers are
 *                      permitted, and the maximum length is 8.
 * @param isCircled Whether the square contains a circle.
 * @param entry Optional field denoting the user's entry for the square.
 * @param isGiven Whether the solution should be prefilled for the user.
 * @param number Optional field indicating the clue number in the square. If all squares in a grid
 *               leave the number unset, then the puzzle will be assumed to use conventional
 *               crossword numbering.
 */
// TODO: Support entry rebuses.
data class Square(
    val solution: Char?,
    val isBlack: Boolean = false,
    val solutionRebus: String = "",
    val isCircled: Boolean = false,
    val entry: Char? = null,
    val isGiven: Boolean = false,
    val number: Int? = null
) {
    init {
        if (isBlack) {
            require(
                solution == null &&
                        solutionRebus == "" &&
                        !isCircled &&
                        entry == null &&
                        !isGiven &&
                        number == null
            ) {
                "Black squares must not set other properties"
            }
        }
    }
}

/** Constant representing a black square. */
val BLACK_SQUARE = Square(isBlack = true, solution = null)
