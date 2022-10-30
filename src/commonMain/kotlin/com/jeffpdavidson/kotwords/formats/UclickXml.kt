package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.model.Crossword
import com.jeffpdavidson.kotwords.model.Puzzle
import com.soywiz.klock.Date
import com.soywiz.klock.DateFormat
import com.soywiz.klock.format

private val TITLE_DATE_FORMAT = DateFormat("EEEE, MMMM d, yyyy")

/** Container for a puzzle in the Universal Uclick XML format. */
class UclickXml(
    private val xml: String,
    private val date: Date,
    private val addDateToTitle: Boolean = true
) : Puzzleable {

    override suspend fun asPuzzle(): Puzzle {
        val document = Xml.parse(xml)

        val category = Encodings.decodeUrl(document.selectFirst("Category")?.attr("v") ?: "")
        val rawTitle = Encodings.decodeUrl(document.selectFirst("Title")?.attr("v") ?: "")
        val formattedDate = if (addDateToTitle) TITLE_DATE_FORMAT.format(date) else null
        val categoryAndDate = listOfNotNull(category.ifEmpty { null }, formattedDate).joinToString(", ")
        val title = listOfNotNull(rawTitle.ifEmpty { null }, categoryAndDate.ifEmpty { null }).joinToString(" - ")

        val author = Encodings.decodeUrl(document.selectFirst("Author")?.attr("v") ?: "")
        val editor = Encodings.decodeUrl(document.selectFirst("Editor")?.attr("v") ?: "")
        val byline = listOfNotNull(
            author.ifEmpty { null },
            if (editor.isEmpty()) { null } else { "Edited by $editor" }
        ).joinToString(" / ")
        val copyright = Encodings.decodeUrl(document.selectFirst("Copyright")?.attr("v") ?: "")

        val allAnswer = document.selectFirst("AllAnswer")?.attr("v") ?: ""
        val width = document.selectFirst("Width")?.attr("v")?.toInt() ?: 0
        val grid = allAnswer.chunked(width).map { row ->
            row.map { square ->
                if (square == '-') {
                    Puzzle.Cell(cellType = Puzzle.CellType.BLOCK)
                } else {
                    Puzzle.Cell(solution = "$square")
                }
            }
        }

        val acrossClues = document.select("across > *")
        val downClues = document.select("down > *")

        return Crossword(
            title = title,
            creator = byline,
            copyright = if (copyright.isEmpty()) "" else "\u00a9 ${date.year} $copyright",
            grid = grid,
            acrossClues = toClueMap(acrossClues),
            downClues = toClueMap(downClues)
        ).asPuzzle()
    }

    private fun toClueMap(clues: Iterable<Element>): Map<Int, String> {
        return clues.associate {
            it.attr("cn").toInt() to Encodings.decodeUrl(it.attr("c"))
        }
    }
}
