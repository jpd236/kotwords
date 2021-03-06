package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.json.JsonSerializer
import com.jeffpdavidson.kotwords.formats.json.WallStreetJournalJson
import com.jeffpdavidson.kotwords.model.BLACK_SQUARE
import com.jeffpdavidson.kotwords.model.Crossword
import com.jeffpdavidson.kotwords.model.Square
import com.soywiz.klock.DateFormat
import com.soywiz.klock.parse

private val PUBLISH_DATE_FORMAT = DateFormat("EEEE, dd MMMM yyyy")

/** Container for a puzzle in the Wall Street Journal JSON format. */
class WallStreetJournal(
    private val json: String,
    private val includeDateInTitle: Boolean = true
) : Crosswordable {

    override fun asCrossword(): Crossword {
        val response = JsonSerializer.fromJson<WallStreetJournalJson.Response>(json)
        val grid = response.data.grid.map { row ->
            row.map { square ->
                if (square.letter == "") {
                    BLACK_SQUARE
                } else {
                    // Treat any kind of special square style as circled, since that's all Across
                    // Lite can render.
                    Square(
                        solution = square.letter[0],
                        solutionRebus = if (square.letter.length > 1) square.letter else "",
                        isCircled = square.style.highlight || square.style.shapebg.isNotEmpty()
                    )
                }
            }
        }.toList()
        val publishDate = response.data.copy.datePublish.unescapeEntities()
        val title = if (includeDateInTitle) {
            "${response.data.copy.title.unescapeEntities()} - $publishDate"
        } else {
            response.data.copy.title.unescapeEntities()
        }
        val date = PUBLISH_DATE_FORMAT.parse(publishDate)
        return Crossword(
            title = title,
            author = response.data.copy.byline.unescapeEntities(),
            copyright = "\u00a9 ${date.yearInt} ${response.data.copy.publisher.unescapeEntities()}",
            notes = response.data.copy.description.unescapeEntities(),
            grid = grid,
            acrossClues = getClueMap(response, "Across"),
            downClues = getClueMap(response, "Down")
        )
    }

    private fun getClueMap(response: WallStreetJournalJson.Response, direction: String):
            Map<Int, String> {
        // TODO(#2): Generalize and centralize accented character replacement.
        return response.data.copy.clues.first { it.title.unescapeEntities() == direction }
            .clues.associate { it.number to it.clue.unescapeEntities().replace('⁄', '/') }
    }

    companion object {
        private fun String.unescapeEntities(): String {
            return Encodings.decodeHtmlEntities(this)
        }
    }
}