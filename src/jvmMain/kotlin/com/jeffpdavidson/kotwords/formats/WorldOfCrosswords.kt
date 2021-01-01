package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.model.BLACK_SQUARE
import com.jeffpdavidson.kotwords.model.Crossword
import com.jeffpdavidson.kotwords.model.Square
import java.io.StringReader
import java.time.LocalDate
import java.util.Locale
import javax.json.Json
import javax.json.JsonArray

/** Container for a puzzle in the World of Crosswords format. */
class WorldOfCrosswords(
    private val json: String,
    private val date: LocalDate,
    private val author: String,
    private val copyright: String
) : Crosswordable {
    override fun asCrossword(): Crossword {
        // This format is so unstructured that there's no reason to use a data-class parser.
        val data = Json.createReader(StringReader(json)).readObject()
        if (!data.getBoolean("success", false)) {
            throw InvalidFormatException("API failure")
        }

        val msg = data.getJsonArray("msg")
        val title = msg.getString(0)
        val clueData = msg.getJsonArray(4)
            .map { it.asJsonArray() }.partition { it.getString(5).toInt() == 1 }
        val gridData = msg.getJsonArray(3)
        val size = gridData.getString(0).toInt() + 1
        val whiteSquareCoordinates = gridData.getJsonArray(1).map {
            val square = it.asJsonArray()
            square.getString(0).toInt() to (size - 1 - square.getString(1).toInt())
        }.toSet()
        val grid = mutableListOf<List<Square>>()
        val answerLetters =
            clueData.first.joinToString("") { it.getString(1) }.toUpperCase(Locale.ROOT)
        var answerLetterIndex = 0
        for (y in 0 until size) {
            val row = mutableListOf<Square>()
            for (x in 0 until size) {
                if (whiteSquareCoordinates.contains(x to y)) {
                    row.add(Square(answerLetters[answerLetterIndex++]))
                } else {
                    row.add(BLACK_SQUARE)
                }
            }
            grid.add(row)
        }

        return Crossword(
            title = title,
            author = author,
            copyright = "\u00a9 ${date.year} $copyright",
            grid = grid,
            acrossClues = buildClueMap(clueData.first),
            downClues = buildClueMap(clueData.second)
        )
    }

    private fun buildClueMap(clueData: List<JsonArray>): Map<Int, String> {
        // Replace italics in HTML (for titles) with quotes.
        return clueData.map {
            it.getString(4).toInt() to it.getString(6).replace("</?i>".toRegex(), "\"")
        }.toMap()
    }
}