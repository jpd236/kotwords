package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.model.Puzzle
import com.soywiz.klock.Date
import com.soywiz.klock.DateFormat
import com.soywiz.klock.format

private val TITLE_DATE_FORMAT = DateFormat("EEEE, MMMM d, yyyy")
private val CLUE_ANNOTATION_REGEX = """ (?:@@|\|\|).*$""".toRegex()

/** Extension to JPZ used by The Puzzle Society (in The Modern Crossword). */
class UclickJpz(
    private val jpzXml: String,
    private val date: Date,
    private val addDateToTitle: Boolean = true
) : Puzzleable() {

    override suspend fun createPuzzle(): Puzzle {
        val puzzle = JpzFile(jpzXml.encodeToByteArray(), stripFormats = true).asPuzzle()

        // Add date to title if provided.
        val rawTitle = puzzle.title
        val formattedDate = if (addDateToTitle) TITLE_DATE_FORMAT.format(date) else null
        val title = listOfNotNull(rawTitle.ifEmpty { null }, formattedDate).joinToString(" - ")

        // Remove unsupported annotations from clues.
        return puzzle.copy(
            title = title,
            clues = puzzle.clues.map { clueList ->
                clueList.copy(
                    clues = clueList.clues.map { clue ->
                        clue.copy(
                            text = clue.text.replace(CLUE_ANNOTATION_REGEX, "")
                        )
                    }
                )
            }
        )
    }
}