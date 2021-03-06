package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.model.BLACK_SQUARE
import com.jeffpdavidson.kotwords.model.Crossword
import com.jeffpdavidson.kotwords.model.Square
import com.soywiz.klock.Date
import com.soywiz.klock.DateFormat
import com.soywiz.klock.format

private val TITLE_DATE_FORMAT = DateFormat("EEEE, MMMM d, yyyy")

/** Container for a puzzle in the Universal Uclick XML format. */
class UclickXml(
    private val xml: String,
    private val date: Date,
    private val addDateToTitle: Boolean = true
) : Crosswordable {

    override fun asCrossword(): Crossword {
        val document = Xml.parse(xml)

        val rawTitle = document.selectFirst("Title")?.attr("v") ?: ""
        val title = if (addDateToTitle) {
            "$rawTitle - ${TITLE_DATE_FORMAT.format(date)}"
        } else {
            rawTitle
        }

        val author = document.selectFirst("Author")?.attr("v") ?: ""
        val copyright = document.selectFirst("Copyright")?.attr("v") ?: ""

        val allAnswer = document.selectFirst("AllAnswer")?.attr("v") ?: ""
        val width = document.selectFirst("Width")?.attr("v")?.toInt() ?: 0
        val grid = allAnswer.chunked(width).map { row ->
            row.map { square ->
                if (square == '-') {
                    BLACK_SQUARE
                } else {
                    Square(square)
                }
            }
        }

        val acrossClues = document.select("across > *")
        val downClues = document.select("down > *")

        return Crossword(
            title = title,
            author = author,
            copyright = "\u00a9 ${date.year} $copyright",
            grid = grid,
            acrossClues = toClueMap(acrossClues),
            downClues = toClueMap(downClues)
        )
    }

    private fun toClueMap(clues: Iterable<Element>): Map<Int, String> {
        return clues.associate {
            it.attr("cn").toInt() to Encodings.decodeUrl(it.attr("c"))
        }
    }
}
