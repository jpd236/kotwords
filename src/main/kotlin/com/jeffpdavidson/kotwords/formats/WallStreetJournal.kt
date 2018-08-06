package com.jeffpdavidson.kotwords.formats

import com.squareup.moshi.Json
import com.jeffpdavidson.kotwords.model.BLACK_SQUARE
import com.jeffpdavidson.kotwords.model.Crossword
import com.jeffpdavidson.kotwords.model.Square
import se.ansman.kotshi.JsonSerializable
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val PUBLISH_DATE_FORMAT = DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy")

/** Container for a puzzle in the Wall Street Journal JSON format. */
class WallStreetJournal(private val json: String,
                        private val includeDateInTitle: Boolean = true) : Crosswordable {
    @JsonSerializable
    internal data class Gridsize(val cols: Int, val rows: Int)

    @JsonSerializable
    internal data class Clue(val number: Int, val clue: String)

    @JsonSerializable
    internal data class ClueSet(val title: String, val clues: List<Clue>)

    @JsonSerializable
    internal data class Copy(
            val title: String,
            val byline: String,
            val description: String,
            @Json(name = "date-publish") val datePublish: String,
            val publisher: String,
            val gridsize: Gridsize,
            val clues: List<ClueSet>)

    @JsonSerializable
    internal data class Square(@Json(name = "Letter") val letter: String)

    @JsonSerializable
    internal data class Data(val copy: Copy, val grid: List<List<Square>>)

    @JsonSerializable
    internal data class Response(val data: Data)

    override fun asCrossword(): Crossword {
        val response = JsonSerializer.fromJson(Response::class.java, json)
        val grid = response.data.grid.map { row ->
            row.map { square ->
                if (square.letter == "") {
                    BLACK_SQUARE
                } else {
                    Square(
                            solution = square.letter[0],
                            solutionRebus = if (square.letter.length > 1) square.letter else "")
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

    private fun getClueMap(response: Response, direction: String): Map<Int, String> {
        return response.data.copy.clues.first { it.title == direction }
                .clues.map { it.number to it.clue }.toMap()
    }
}