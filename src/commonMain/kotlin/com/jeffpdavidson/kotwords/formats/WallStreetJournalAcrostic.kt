package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.Encodings.decodeHtmlEntities
import com.jeffpdavidson.kotwords.formats.json.JsonSerializer
import com.jeffpdavidson.kotwords.formats.json.WallStreetJournalJson
import com.jeffpdavidson.kotwords.model.Acrostic
import com.soywiz.klock.DateFormat
import com.soywiz.klock.parse

private val PUBLISH_DATE_FORMAT = DateFormat("EEEE, dd MMMM yyyy")

/** Container for an acrostic puzzle in the Wall Street Journal JSON format. */
class WallStreetJournalAcrostic(private val json: String) : DelegatingPuzzleable() {

    override suspend fun getPuzzleable(): Acrostic {
        val response = JsonSerializer.fromJson<WallStreetJournalJson.AcrosticJson>(json)
        val gridKey = mutableMapOf<Char, MutableMap<Char, Int>>()
        response.celldata
            .filter { it in 'A'..'Z' }
            .zip(response.cluedata.filter { it in 'A'..'Z' })
            .forEachIndexed { gridIndex, (cellId, clueId) ->
                val wordKey = gridKey.getOrPut(cellId) { mutableMapOf() }
                wordKey[clueId] = gridIndex + 1
            }
        val publishDate = decodeHtmlEntities(response.copy.datePublish)
        val title = decodeHtmlEntities(response.copy.title)
        val date = PUBLISH_DATE_FORMAT.parse(publishDate)
        return Acrostic(
            title = title,
            creator = decodeHtmlEntities(response.copy.byline ?: ""),
            copyright = "\u00a9 ${date.yearInt} ${decodeHtmlEntities(response.copy.publisher)}",
            description = "",
            suggestedWidth = response.copy.gridsize.cols,
            solution = response.settings.solution,
            gridKey = gridKey.keys.sorted().map { wordKey ->
                gridKey[wordKey]!!.entries.sortedBy { it.key }.map { it.value }
            },
            clues = response.settings.clues.map { decodeHtmlEntities(it.clue) },
            completionMessage = decodeHtmlEntities(response.copy.description ?: ""),
            includeAttribution = true,
        )
    }
}