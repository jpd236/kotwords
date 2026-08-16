package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.json.JsonSerializer
import com.jeffpdavidson.kotwords.formats.json.UclickJson.Response
import com.jeffpdavidson.kotwords.formats.json.UclickJson.UsaTodayResponse
import com.jeffpdavidson.kotwords.model.Crossword
import com.jeffpdavidson.kotwords.model.Puzzle
import korlibs.time.DateFormat
import korlibs.time.format
import korlibs.time.parseDate

private val JSON_DATE_FORMAT = DateFormat("yyyyMMdd")
private val USA_TODAY_DATE_FORMAT = DateFormat("yyyy-MM-dd")
private val TITLE_DATE_FORMAT = DateFormat("EEEE, MMMM d, yyyy")

/** Container for a puzzle in the Universal Uclick JSON format. */
class UclickJson internal constructor(
    private val json: String,
    private val copyright: String,
    private val addDateToTitle: Boolean,
    private val dateFormat: DateFormat,
    private val deserializeFn: (String) -> Response,
) : DelegatingPuzzleable() {

    constructor(
        json: String,
        copyright: String = "",
        addDateToTitle: Boolean = true
    ) : this(json, copyright, addDateToTitle, JSON_DATE_FORMAT, ::deserializeUclickJson)

    override suspend fun getPuzzleable(): Puzzleable {
        val response = deserializeFn(json)
        val date = dateFormat.parseDate(response.date)
        val copyright = if (response.copyright.isNotEmpty()) decode(response.copyright) else copyright
        val grid = if (response.allAnswer.isNotEmpty()) {
            toGrid(response.allAnswer.chunked(response.width), '-')
        } else if (response.solution.isNotEmpty()) {
            toGrid(response.solution, ' ')
        } else {
            throw UnsupportedOperationException("Uclick JSON missing all known solution fields")
        }
        val rawTitle = decode(response.title)
        val title = if (addDateToTitle) {
            "$rawTitle - ${TITLE_DATE_FORMAT.format(date)}"
        } else {
            rawTitle
        }
        return Crossword(
            title = title,
            creator = decode(response.author),
            copyright = "\u00a9 ${date.year} $copyright",
            grid = grid,
            acrossClues = toClueMap(response.acrossClue),
            downClues = toClueMap(response.downClue)
        )
    }

    private fun toGrid(solutionRows: List<String>, blackSquareChar: Char): List<List<Puzzle.Cell>> {
        return solutionRows.map { row ->
            row.map { square ->
                if (square == blackSquareChar) {
                    Puzzle.Cell(cellType = Puzzle.CellType.BLOCK)
                } else {
                    Puzzle.Cell(solution = "$square")
                }
            }
        }
    }

    private fun toClueMap(clueString: String): Map<Int, String> {
        return clueString.split("\n").takeWhile { it != "end" }.associate {
            val parts = it.split('|')
            parts[0].toInt() to parts[1].replace('\u0092', '\'')
        }
    }

    private fun decode(input: String): String = Encodings.decodeUrl(input)

    companion object {
        fun fromUsaTodayJson(json: String, copyright: String = "", addDateToTitle: Boolean = true): UclickJson {
            return UclickJson(json, copyright, addDateToTitle, USA_TODAY_DATE_FORMAT) {
                JsonSerializer.fromJson<UsaTodayResponse>(json).data.gameData
            }
        }

        private fun deserializeUclickJson(json: String): Response {
            return JsonSerializer.fromJson<Response>(json)
        }
    }
}