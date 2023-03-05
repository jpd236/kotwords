package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.Encodings.decodeHtmlEntities
import com.jeffpdavidson.kotwords.formats.json.JsonSerializer
import com.jeffpdavidson.kotwords.formats.json.xwordinfo.XWordInfoAcrosticJson
import com.jeffpdavidson.kotwords.model.Acrostic
import com.soywiz.klock.DateFormat
import com.soywiz.klock.parse

private val DATE_FORMAT = DateFormat("M/d/yyyy")
private val TITLE_DATE_FORMAT = DateFormat("EEEE, MMMM d, yyyy")

/** Container for an acrostic puzzle in the XWord Info JSON format. */
class XWordInfoAcrostic(private val json: String, private val author: String) : DelegatingPuzzleable() {

    override suspend fun getPuzzleable(): Acrostic {
        val response = JsonSerializer.fromJson<XWordInfoAcrosticJson.Response>(json)
        val date = DATE_FORMAT.parse(response.date)
        return Acrostic(
            title = "Acrostic for ${TITLE_DATE_FORMAT.format(date)}",
            creator = author,
            copyright = "\u00a9 ${decodeHtmlEntities(response.copyright)}",
            description = "",
            suggestedWidth = response.cols,
            solution = response.answerKey,
            gridKey = response.clueData.map { clueKey -> clueKey.split(",").map { it.toInt() } },
            clues = response.clues.map { decodeHtmlEntities(it) },
            completionMessage = response.quote?.let { decodeHtmlEntities(it) } ?: response.answerKey,
            includeAttribution = true,
        )
    }
}