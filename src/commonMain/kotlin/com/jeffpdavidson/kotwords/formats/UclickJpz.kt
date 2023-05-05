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
        // Try to URL-decode the whole file in case there are URL-encoded fields in the metadata. If it fails due to
        // invalid sequences, we leave it untouched; we'll still try decoding each individual clue below.
        val puzzle = JpzFile(tryDecodeUrl(jpzXml).encodeToByteArray(), stripFormats = true).asPuzzle()

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
                        clue.copy(text = decodeClue(clue.text))
                    }
                )
            }
        )
    }

    companion object {
        internal fun decodeClue(text: String): String =
            tryDecodeUrl(text).replace(CLUE_ANNOTATION_REGEX, "")

        /** Try URL decoding the text, but leave it untouched if it contains invalid sequences. */
        private fun tryDecodeUrl(text: String): String {
            return try {
                Encodings.decodeUrl(text)
            } catch (e: Throwable) {
                text
            }
        }
    }
}