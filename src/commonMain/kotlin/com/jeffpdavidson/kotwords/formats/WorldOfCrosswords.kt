package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.json.JsonSerializer
import com.jeffpdavidson.kotwords.formats.json.WorldOfCrosswordsJson
import com.jeffpdavidson.kotwords.model.BLACK_SQUARE
import com.jeffpdavidson.kotwords.model.Crossword
import com.jeffpdavidson.kotwords.model.Square
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

/** Container for a puzzle in the World of Crosswords format. */
class WorldOfCrosswords(
    private val json: String,
    private val year: Int,
    private val author: String,
    private val copyright: String
) : Crosswordable {
    override fun asCrossword(): Crossword {
        val data = JsonSerializer.fromJson<WorldOfCrosswordsJson>(json)
        if (!data.success) {
            throw InvalidFormatException("API failure")
        }

        val title = data.msg[0].jsonPrimitive.content
        val clueData = data.msg[4].jsonArray
            .map { it.jsonArray }.partition { it[5].jsonPrimitive.int == 1 }
        val gridData = data.msg[3].jsonArray
        val size = gridData[0].jsonPrimitive.int + 1
        val whiteSquareCoordinates = gridData[1].jsonArray.map {
            val square = it.jsonArray
            square[0].jsonPrimitive.int to (size - 1 - square[1].jsonPrimitive.int)
        }.toSet()
        val grid = mutableListOf<List<Square>>()
        val answerLetters =
            clueData.first.joinToString("") { it[1].jsonPrimitive.content }.toUpperCase()
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
            copyright = "\u00a9 $year $copyright",
            grid = grid,
            acrossClues = buildClueMap(clueData.first),
            downClues = buildClueMap(clueData.second)
        )
    }

    private fun buildClueMap(clueData: List<JsonArray>): Map<Int, String> {
        // Replace italics in HTML (for titles) with quotes.
        return clueData.associate {
            it[4].jsonPrimitive.int to it[6].jsonPrimitive.content.replace("</?i>".toRegex(), "\"")
        }
    }
}