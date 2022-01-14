package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.json.JsonSerializer
import com.jeffpdavidson.kotwords.formats.json.WorldOfCrosswordsJson
import com.jeffpdavidson.kotwords.model.Crossword
import com.jeffpdavidson.kotwords.model.Puzzle
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
) : Puzzleable {

    override suspend fun asPuzzle(): Puzzle {
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
        val grid = mutableListOf<List<Puzzle.Cell>>()
        val answerLetters =
            clueData.first.joinToString("") { it[1].jsonPrimitive.content }.uppercase()
        var answerLetterIndex = 0
        for (y in 0 until size) {
            val row = mutableListOf<Puzzle.Cell>()
            for (x in 0 until size) {
                if (whiteSquareCoordinates.contains(x to y)) {
                    row.add(Puzzle.Cell(solution = "${answerLetters[answerLetterIndex++]}"))
                } else {
                    row.add(Puzzle.Cell(cellType = Puzzle.CellType.BLOCK))
                }
            }
            grid.add(row)
        }

        return Crossword(
            title = title,
            creator = author,
            copyright = "\u00a9 $year $copyright",
            grid = grid,
            acrossClues = buildClueMap(clueData.first),
            downClues = buildClueMap(clueData.second),
            hasHtmlClues = true,
        ).asPuzzle()
    }

    private fun buildClueMap(clueData: List<JsonArray>): Map<Int, String> {
        return clueData.associate {
            it[4].jsonPrimitive.int to it[6].jsonPrimitive.content
        }
    }
}