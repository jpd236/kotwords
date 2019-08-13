package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.json.JsonSerializer
import com.jeffpdavidson.kotwords.formats.json.WallStreetJournalJson
import com.jeffpdavidson.kotwords.model.BLACK_SQUARE
import com.jeffpdavidson.kotwords.model.Crossword
import com.jeffpdavidson.kotwords.model.Square
import org.jsoup.parser.Parser
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val PUBLISH_DATE_FORMAT = DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy")

/** Container for a puzzle in the Wall Street Journal JSON format. */
class WallStreetJournal(private val json: String,
                        private val includeDateInTitle: Boolean = true) : Crosswordable {

    override fun asCrossword(): Crossword {
        val response = JsonSerializer.fromJson(WallStreetJournalJson.Response::class.java, json)
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
                            isCircled = square.style.highlight || !square.style.shapebg.isEmpty())
                }
            }
        }.toList()
        val title = if (includeDateInTitle) {
            "${response.data.copy.title} - ${response.data.copy.datePublish}"
        } else {
            response.data.copy.title
        }
        val date = LocalDate.parse(response.data.copy.datePublish, PUBLISH_DATE_FORMAT)
        return Crossword(
                title = title,
                author = response.data.copy.byline,
                copyright = "\u00a9 ${date.year} ${response.data.copy.publisher}",
                notes = response.data.copy.description,
                grid = grid,
                acrossClues = getClueMap(response, "Across"),
                downClues = getClueMap(response, "Down"))
    }

    private fun getClueMap(response: WallStreetJournalJson.Response, direction: String):
            Map<Int, String> {
        return response.data.copy.clues.first { it.title == direction }.clues
                .map {
                    it.number to Parser.unescapeEntities(it.clue, /* inAttribute= */ false)
                }.toMap()
    }
}