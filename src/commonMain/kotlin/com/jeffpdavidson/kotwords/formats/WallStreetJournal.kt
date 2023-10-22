package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.json.JsonSerializer
import com.jeffpdavidson.kotwords.formats.json.WallStreetJournalJson
import com.jeffpdavidson.kotwords.model.Crossword
import com.jeffpdavidson.kotwords.model.Puzzle
import korlibs.time.DateFormat
import korlibs.time.parse

private val PUBLISH_DATE_FORMAT = DateFormat("EEEE, dd MMMM yyyy")

/** Container for a puzzle in the Wall Street Journal JSON format. */
class WallStreetJournal(
    private val json: String,
    private val includeDateInTitle: Boolean = true
) : DelegatingPuzzleable() {

    override suspend fun getPuzzleable(): Puzzleable {
        val response = JsonSerializer.fromJson<WallStreetJournalJson.CrosswordJson>(json)
        val grid = response.data.grid.map { row ->
            row.map { square ->
                if (square.blank != "") {
                    Puzzle.Cell(cellType = Puzzle.CellType.BLOCK)
                } else {
                    // Treat any kind of special square style as circled, since that's all Across
                    // Lite can render.
                    // TODO: Propagate square.style.highlight for JPZ purposes if an example of it exists.
                    val backgroundShape =
                        if (square.style.highlight || square.style.shapebg.isNotEmpty()) {
                            Puzzle.BackgroundShape.CIRCLE
                        } else {
                            Puzzle.BackgroundShape.NONE
                        }
                    Puzzle.Cell(
                        solution = square.letter,
                        backgroundShape = backgroundShape,
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
        val creator = (response.data.meta.author ?: "").ifBlank {
            response.data.copy.byline ?: ""
        }.unescapeEntities()
        val description = (response.data.copy.crosswordAdditionalCopy ?: "").ifBlank {
            response.data.copy.description ?: ""
        }.unescapeEntities()
        return Crossword(
            title = title,
            creator = creator,
            copyright = "\u00a9 ${date.yearInt} ${response.data.copy.publisher.unescapeEntities()}",
            description = description,
            grid = grid,
            acrossClues = getClueMap(response, "Across"),
            downClues = getClueMap(response, "Down")
        )
    }

    private fun getClueMap(response: WallStreetJournalJson.CrosswordJson, direction: String):
            Map<Int, String> {
        return response.data.copy.clues.first { it.title.unescapeEntities() == direction }
            .clues.associate { it.number to it.clue.unescapeEntities() }
    }

    companion object {
        private fun String.unescapeEntities(): String {
            return Encodings.decodeHtmlEntities(this)
        }
    }
}