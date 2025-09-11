package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.formats.Puzzleable
import okio.ByteString

// TODO: Validate data structures.
data class Puzzle(
    val title: String,
    val creator: String,
    val copyright: String,
    val description: String,
    val grid: List<List<Cell>>,
    val clues: List<ClueList>,
    val words: List<Word>,
    val hasHtmlClues: Boolean = false,
    val completionMessage: String = "",
    val puzzleType: PuzzleType = PuzzleType.CROSSWORD,
    val hasUnsupportedFeatures: Boolean = false,
    val diagramless: Boolean = false,
) : Puzzleable() {

    enum class CellType {
        REGULAR,
        BLOCK,
        CLUE,
        VOID;

        fun isBlack(): Boolean {
            return this == BLOCK || this == VOID
        }
    }

    enum class BackgroundShape {
        NONE,
        CIRCLE
    }

    enum class BorderDirection {
        TOP,
        LEFT,
        RIGHT,
        BOTTOM
    }

    enum class ImageFormat {
        GIF,
        JPG,
        PNG,
    }

    sealed interface Image {
        object None : Image
        data class Data(val format: ImageFormat, val bytes: ByteString) : Image
    }

    data class Cell(
        val solution: String = "",
        val entry: String = "",
        val foregroundColor: String = "",
        val backgroundColor: String = "",
        val backgroundImage: Image = Image.None,
        val number: String = "",
        val topRightNumber: String = "",
        val cellType: CellType = CellType.REGULAR,
        val backgroundShape: BackgroundShape = BackgroundShape.NONE,
        val borderDirections: Set<BorderDirection> = setOf(),
        val moreAnswers: List<String> = listOf(),
        val hint: Boolean = false,
    )

    data class Coordinate(
        val x: Int,
        val y: Int,
    )

    data class Word(
        val id: Int,
        val cells: List<Coordinate>
    )

    data class Clue(
        val wordId: Int,
        val number: String,
        val text: String,
        val format: String = "",
    ) {
        fun textAndFormat(): String {
            if (format.isNotBlank()) {
                return "$text ($format)"
            }
            return text
        }
    }

    data class ClueList(
        val title: String,
        val clues: List<Clue>,
        val direction: String = "",
    )

    enum class PuzzleType {
        CROSSWORD,
        ACROSTIC,
        CODED,
    }

    override suspend fun createPuzzle(): Puzzle = this

    /** Returns the clue list whose title contains the given text (case-insensitive), if one exists. */
    fun getClues(titleText: String): ClueList? =
        clues.find { it.title.contains(titleText.toRegex(RegexOption.IGNORE_CASE)) }

    /**
     * Returns whether there are unclued words in the puzzle.
     *
     * This is true if any clue has text (to ignore puzzles with no text clues, like coded crosswords) and if the set of
     * entries in the puzzle isn't a 1-1 match with the set of clues.
     */
    fun hasUncluedWords(): Boolean {
        val anyClueHasText = clues.any { clueList -> clueList.clues.any { it.text.isNotEmpty() } }
        if (!anyClueHasText) {
            return false
        }
        val wordsByWordId = words.associate { it.id to it.cells }
        val clueWordIds = clues.map { it.clues }.flatten().map { it.wordId }.toSet()
        return wordsByWordId.keys != clueWordIds
    }

    /**
     * Returns whether the grid's solution is provided.
     *
     * This is true if any regular cell has non-empty solution text.
     */
    fun hasSolution(): Boolean {
        return grid.flatten().any {
            it.cellType == Puzzle.CellType.REGULAR && it.solution.isNotEmpty()
        }
    }
}
