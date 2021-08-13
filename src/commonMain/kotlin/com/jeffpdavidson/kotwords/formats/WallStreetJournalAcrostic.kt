package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.json.JsonSerializer
import com.jeffpdavidson.kotwords.formats.json.WallStreetJournalJson
import com.jeffpdavidson.kotwords.model.Acrostic
import com.jeffpdavidson.kotwords.model.Puzzle
import com.soywiz.klock.DateFormat
import com.soywiz.klock.parse

private val PUBLISH_DATE_FORMAT = DateFormat("EEEE, dd MMMM yyyy")

/** Container for an acrostic puzzle in the Wall Street Journal JSON format. */
class WallStreetJournalAcrostic(private val json: String) : Puzzleable {

    override fun asPuzzle(): Puzzle = asAcrostic().asPuzzle()

    fun asAcrostic(): Acrostic {
        val response = JsonSerializer.fromJson<WallStreetJournalJson.AcrosticJson>(json)
        val gridKey = mutableMapOf<Char, MutableMap<Char, Int>>()
        response.celldata
            .filter { it in 'A'..'Z' }
            .zip(response.cluedata.filter { it in 'A'..'Z' })
            .forEachIndexed { gridIndex, (cellId, clueId) ->
                val wordKey = gridKey.getOrPut(cellId) { mutableMapOf() }
                wordKey[clueId] = gridIndex + 1
            }
        val publishDate = response.copy.datePublish.unescapeEntities()
        val title = response.copy.title.unescapeEntities()
        val date = PUBLISH_DATE_FORMAT.parse(publishDate)
        return Acrostic(
            title = title,
            creator = response.copy.byline.unescapeEntities(),
            copyright = "\u00a9 ${date.yearInt} ${response.copy.publisher.unescapeEntities()}",
            description = "",
            suggestedWidth = response.copy.gridsize.cols,
            solution = response.settings.solution,
            gridKey = gridKey.keys.sorted().map { wordKey ->
                gridKey[wordKey]!!.entries.sortedBy { it.key }.map { it.value }
            },
            clues = response.settings.clues.map { it.clue.unescapeEntities() },
            completionMessage = response.copy.description.unescapeEntities(),
            includeAttribution = true,
        )
    }

    companion object {
        private fun String.unescapeEntities(): String {
            return Encodings.decodeHtmlEntities(this)
        }
    }
}