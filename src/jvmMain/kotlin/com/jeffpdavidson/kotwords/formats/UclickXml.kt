package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.Xml.getChildElementList
import com.jeffpdavidson.kotwords.formats.Xml.getElementByTagName
import com.jeffpdavidson.kotwords.model.BLACK_SQUARE
import com.jeffpdavidson.kotwords.model.Crossword
import com.jeffpdavidson.kotwords.model.Square
import org.w3c.dom.Element
import java.net.URLDecoder
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val TITLE_DATE_FORMAT = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")

/** Container for a puzzle in the Universal Uclick XML format. */
class UclickXml(
    private val xml: String,
    private val date: LocalDate,
    private val addDateToTitle: Boolean = true
) : Crosswordable {

    override fun asCrossword(): Crossword {
        val document = Xml.parseDocument(xml)

        val rawTitle = document.getElementByTagName("Title").getAttribute("v")
        val title = if (addDateToTitle) {
            "$rawTitle - ${TITLE_DATE_FORMAT.format(date)}"
        } else {
            rawTitle
        }

        val author = document.getElementByTagName("Author").getAttribute("v")
        val copyright = document.getElementByTagName("Copyright").getAttribute("v")

        val allAnswer = document.getElementByTagName("AllAnswer").getAttribute("v")
        val width = document.getElementByTagName("Width").getAttribute("v").toInt()
        val grid = allAnswer.chunked(width).map { row ->
            row.map { square ->
                if (square == '-') {
                    BLACK_SQUARE
                } else {
                    Square(square)
                }
            }
        }

        val acrossClues = document.getElementByTagName("across").getChildElementList()
        val downClues = document.getElementByTagName("down").getChildElementList()

        return Crossword(
            title = title,
            author = author,
            copyright = "\u00a9 ${date.year} $copyright",
            grid = grid,
            acrossClues = toClueMap(acrossClues),
            downClues = toClueMap(downClues)
        )
    }

    private fun toClueMap(clues: List<Element>): Map<Int, String> {
        return clues.associate {
            it.getAttribute("cn").toInt() to URLDecoder.decode(it.getAttribute("c"), "UTF-8")
        }
    }
}
