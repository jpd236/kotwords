package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.json.JsonSerializer
import com.jeffpdavidson.kotwords.formats.json.UclickJson
import com.jeffpdavidson.kotwords.model.BLACK_SQUARE
import com.jeffpdavidson.kotwords.model.Crossword
import com.jeffpdavidson.kotwords.model.Square
import com.soywiz.klock.DateFormat
import com.soywiz.klock.format
import com.soywiz.klock.parseDate

private val JSON_DATE_FORMAT = DateFormat("yyyyMMdd")
private val TITLE_DATE_FORMAT = DateFormat("EEEE, MMMM d, yyyy")

/** Container for a puzzle in the Universal Uclick JSON format. */
class UclickJson(
    private val json: String,
    private val copyright: String = "",
    private val addDateToTitle: Boolean = true
) : Crosswordable {

    override fun asCrossword(): Crossword {
        val response = JsonSerializer.fromJson<UclickJson.Response>(json)
        val date = JSON_DATE_FORMAT.parseDate(response.date)
        val copyright = if (response.copyright.isNotEmpty()) decode(response.copyright) else copyright
        val grid = response.allAnswer.chunked(response.width).map { row ->
            row.map { square -> if (square == '-') BLACK_SQUARE else Square("$square") }
        }
        val rawTitle = decode(response.title)
        val title = if (addDateToTitle) {
            "$rawTitle - ${TITLE_DATE_FORMAT.format(date)}"
        } else {
            rawTitle
        }
        return Crossword(
            title = title,
            author = decode(response.author),
            copyright = "\u00a9 ${date.year} $copyright",
            grid = grid,
            acrossClues = toClueMap(response.acrossClue),
            downClues = toClueMap(response.downClue)
        )
    }

    private fun toClueMap(clueString: String): Map<Int, String> {
        return clueString.split("\n").takeWhile { it != "end" }.associate {
            val parts = it.split('|')
            parts[0].toInt() to parts[1].replace('\u0092', '\'')
        }
    }

    private fun decode(input: String): String = Encodings.decodeUrl(input)
}