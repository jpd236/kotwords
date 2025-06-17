package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.json.CrosshareJson
import com.jeffpdavidson.kotwords.formats.json.JsonSerializer
import com.jeffpdavidson.kotwords.model.Crossword
import com.jeffpdavidson.kotwords.model.Puzzle

class Crosshare(private val json: String) : DelegatingPuzzleable() {
    override suspend fun getPuzzleable(): Puzzleable {
        val rawData = JsonSerializer.fromJson<CrosshareJson.Data>(json)
        val pageProps =
            rawData.pageProps ?: rawData.props?.pageProps ?: throw InvalidFormatException("No data found in JSON")
        val data = pageProps.puzzle
        val author =
            if (data.guestConstructor?.isNotEmpty() == true) {
                "${data.guestConstructor} / Published by ${data.authorName}"
            } else {
                data.authorName
            }
        val clues = getClues(data.clues, data.clueHasts)
        return Crossword(
            title = data.title,
            creator = author,
            copyright = data.copyright ?: "",
            description = data.constructorNotes?.let { extractHtml(it) } ?: "",
            grid = data.grid.withIndex().chunked(data.size.cols).map { row ->
                row.map { (i, ch) ->
                    val borderDirections = setOfNotNull(
                        if (data.hBars.contains(i)) {
                            Puzzle.BorderDirection.BOTTOM
                        } else null,
                        if (data.vBars.contains(i)) {
                            Puzzle.BorderDirection.RIGHT
                        } else null,
                    )
                    if (ch == ".") {
                        Puzzle.Cell(
                            cellType = Puzzle.CellType.BLOCK,
                            borderDirections = borderDirections,
                        )
                    } else {
                        val backgroundShape =
                            if (data.highlighted.contains(i) || data.cellStyles.circle.contains(i)) {
                                Puzzle.BackgroundShape.CIRCLE
                            } else {
                                Puzzle.BackgroundShape.NONE
                            }
                        val backgroundColor = if (data.cellStyles.shade.contains(i)) "#dcdcdc" else ""
                        Puzzle.Cell(
                            solution = ch,
                            backgroundShape = backgroundShape,
                            backgroundColor = backgroundColor,
                            borderDirections = borderDirections,
                        )
                    }
                }
            },
            acrossClues = clues.first,
            downClues = clues.second,
            hasHtmlClues = true,
        )
    }

    private fun extractHtml(htmlTag: CrosshareJson.HtmlTag): String {
        val myText = if (htmlTag.type == "text" && htmlTag.value != null) {
            htmlTag.value.replace("&", "&amp;").replace("<", "&lt;")
        } else {
            ""
        }
        val rawChildText = htmlTag.children.map { extractHtml(it) }.joinToString("")
        val childText = when (htmlTag.tagName) {
            "em" -> "<i>$rawChildText</i>"
            "strong" -> "<b>$rawChildText</b>"
            "i", "b", "sub", "sup" -> "<${htmlTag.tagName}>$rawChildText</${htmlTag.tagName}>"
            else -> rawChildText
        }
        return myText + childText
    }

    private fun getClues(
        clues: List<CrosshareJson.Clue>,
        htmlClues: List<CrosshareJson.HtmlTag>
    ): Pair<Map<Int, String>, Map<Int, String>> {
        if (clues.size != htmlClues.size) {
            // Can't correlate with HTML clues; just return the raw ones.
            return clues.filter { it.dir == 0 }.associate { it.num to it.clue } to
                    clues.filter { it.dir == 1 }.associate { it.num to it.clue }
        }
        // HTML clues are sorted by clue number. Across is before down if two clues have the same number.
        val combinedClues = clues.sortedWith(compareBy(CrosshareJson.Clue::num, CrosshareJson.Clue::dir)).zip(htmlClues)
        return combinedClues.filter { it.first.dir == 0 }.associate { it.first.num to extractHtml(it.second) } to
                combinedClues.filter { it.first.dir == 1 }.associate { it.first.num to extractHtml(it.second) }
    }
}