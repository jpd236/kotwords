package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.json.JsonSerializer
import com.jeffpdavidson.kotwords.formats.json.UclickJson
import com.jeffpdavidson.kotwords.model.BLACK_SQUARE
import com.jeffpdavidson.kotwords.model.Crossword
import com.jeffpdavidson.kotwords.model.Square
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val JSON_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd")
private val TITLE_DATE_FORMAT = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")

/** Container for a puzzle in the Universal Uclick JSON format. */
class UclickJson(private val json: String,
                 private val copyright: String = "",
                 private val addDateToTitle: Boolean = true) : Crosswordable {

    override fun asCrossword(): Crossword {
        val response = JsonSerializer.fromJson(UclickJson.Response::class.java, json)
        val date = LocalDate.parse(response.date, JSON_DATE_FORMAT)
        val copyright = if (!response.copyright.isEmpty()) response.copyright else copyright
        val grid = response.allAnswer.chunked(response.width).map { row ->
            row.map { square -> if (square == '-') BLACK_SQUARE else Square(square) }
        }
        val title = if (addDateToTitle) {
            "${response.title} - ${TITLE_DATE_FORMAT.format(date)}"
        } else {
            response.title
        }
        return Crossword(
                title = title,
                author = response.author,
                copyright = "\u00a9 ${date.year} $copyright",
                grid = grid,
                acrossClues = toClueMap(response.acrossClue),
                downClues = toClueMap(response.downClue))
    }

    private fun toClueMap(clueString: String): Map<Int, String> {
        return clueString.split("\n").takeWhile { it != "end" }.map {
            val parts = it.split('|')
            parts[0].toInt() to parts[1]
        }.toMap()
    }
}