package com.jeffpdavidson.kotwords.model

/** Extension functions to make crosswords downs-only. */
object DownsOnly {
    /**
     * Return a copy of this puzzle with only the down clues (usually).
     *
     * Solving with down clues generally works because all the theme answers, which are typically
     * the longest answers in the grid, run across the grid. Thus, it's generally possible to infer
     * their answers from a few of the crossing letters, even without knowing the clue. However,
     * some themes have more theme material running down the grid, in which case it is "fairer" to
     * solve with only the across clues.
     *
     * Thus, we use a rough heuristic to decide which direction of clues is more useful: if one
     * direction's max word length is longer than the other's, we clear the clues in that direction;
     * otherwise, we use the number of words of that max length in each direction as a tiebreaker.
     * If there are an equal number of max-length words in both directions, we return the puzzle
     * with only the down clues.
     */
    fun Puzzle.withDownsOnly(): Puzzle {
        val acrossClues = getClues("Across")
        val downClues = getClues("Down")
        require(acrossClues != downClues && acrossClues != null && downClues != null && clues.size == 2) {
            "Cannot convert puzzle with non-standard clue lists to Downs Only"
        }
        val directionToClear = getDirectionToClearForDownsOnly()
        return copy(
            clues = when (directionToClear) {
                ClueDirection.ACROSS -> listOf(clearClues(acrossClues), downClues)
                ClueDirection.DOWN -> listOf(acrossClues, clearClues(downClues))
            }
        )
    }

    internal enum class ClueDirection { ACROSS, DOWN }

    internal fun Puzzle.getDirectionToClearForDownsOnly(): ClueDirection {
        data class WordStats(
            var maxWordLength: Int = 0,
            var wordsAtMaxLength: Int = 0
        )

        val acrossWordStats = WordStats()
        val downWordStats = WordStats()

        var curWordLength = 0
        fun WordStats.onWordFinished() {
            if (curWordLength > maxWordLength) {
                maxWordLength = curWordLength
                wordsAtMaxLength = 1
            } else if (curWordLength == maxWordLength) {
                wordsAtMaxLength++
            }
            curWordLength = 0
        }

        for (y in grid.indices) {
            for (x in grid[y].indices) {
                if (grid[y][x].cellType.isBlack()) {
                    if (curWordLength > 0) {
                        acrossWordStats.onWordFinished()
                    }
                } else {
                    curWordLength++
                }
            }
            if (curWordLength > 0) {
                acrossWordStats.onWordFinished()
            }
        }

        for (x in grid[0].indices) {
            for (y in grid.indices) {
                if (grid[y][x].cellType.isBlack()) {
                    if (curWordLength > 0) {
                        downWordStats.onWordFinished()
                    }
                } else {
                    curWordLength++
                }
            }
            if (curWordLength > 0) {
                downWordStats.onWordFinished()
            }
        }

        if (acrossWordStats.maxWordLength > downWordStats.maxWordLength) {
            return ClueDirection.ACROSS
        }
        if (acrossWordStats.maxWordLength < downWordStats.maxWordLength
            || acrossWordStats.wordsAtMaxLength < downWordStats.wordsAtMaxLength
        ) {
            return ClueDirection.DOWN
        }
        return ClueDirection.ACROSS
    }

    private fun clearClues(clueList: Puzzle.ClueList): Puzzle.ClueList =
        clueList.copy(clues = clueList.clues.map { clue -> clue.copy(text = "-", format = "") })
}