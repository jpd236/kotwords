package com.jeffpdavidson.kotwords.model

/**
 * A square in the grid of a [Crossword].
 *
 * Black squares will have [isBlack] set to true. White squares will have a non-null [solution].
 *
 * @constructor Create a new white Square. (For black squares, use [BLACK_SQUARE]).
 * @param isBlack Whether the square is black.
 * @param solution The solution.
 * @param isCircled Whether the square contains a circle.
 * @param entry Optional field denoting the user's entry for the square.
 * @param entryRebus Optional field denoting the user's rebus entry for the square.
 * @param isGiven Whether the solution should be prefilled for the user.
 * @param number Optional field indicating the clue number in the square. If all squares in a grid
 *               leave the number unset, then the puzzle will be assumed to use conventional
 *               crossword numbering.
 * @param foregroundColor Optional field indicating the foreground (text) color of the square.
 * @param backgroundColor Optional field indicating the background color of the square.
 * @param borderDirections Optional field indicating the directions of any barred borders (walls).
 */
data class Square(
    val solution: String?,
    val isBlack: Boolean = false,
    val isCircled: Boolean = false,
    val entry: String? = null,
    val isGiven: Boolean = false,
    val number: Int? = null,
    val foregroundColor: String? = null,
    val backgroundColor: String? = null,
    val borderDirections: Set<BorderDirection> = setOf(),
    val moreAnswers: List<String> = listOf(),
) {
    init {
        if (isBlack) {
            require(
                solution == null &&
                        !isCircled &&
                        entry == null &&
                        !isGiven &&
                        number == null &&
                        borderDirections.isEmpty() &&
                        moreAnswers.isEmpty()
            ) {
                // Note: OK for black squares to have different background colors.
                "Black squares must not set other properties"
            }
        }
    }

    enum class BorderDirection {
        TOP,
        LEFT,
        RIGHT,
        BOTTOM
    }
}

/**
 * Constant representing a black square.
 *
 * Note that not all black squares will equal BLACK_SQUARE, since black squares may have other properties like
 * background color set to non-default values. This may be used as shorthand to create a standard black square, but
 * checking if a square is black should always be done with [Square.isBlack].
 */
val BLACK_SQUARE = Square(isBlack = true, solution = null)
