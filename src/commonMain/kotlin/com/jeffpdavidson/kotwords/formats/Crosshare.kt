package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.json.CrosshareJson
import com.jeffpdavidson.kotwords.formats.json.JsonSerializer
import com.jeffpdavidson.kotwords.model.Crossword
import com.jeffpdavidson.kotwords.model.Puzzle

class Crosshare(private val json: String) : DelegatingPuzzleable() {
    override suspend fun getPuzzleable(): Puzzleable {
        val data = JsonSerializer.fromJson<CrosshareJson.Data>(json).props.pageProps.puzzle
        val author =
            if (data.guestConstructor?.isNotEmpty() == true) {
                "${data.guestConstructor} / Published by ${data.authorName}"
            } else {
                data.authorName
            }
        return Crossword(
            title = data.title,
            creator = author,
            copyright = data.copyright ?: "",
            description = data.constructorNotes?.let { extractText(it) } ?: "",
            grid = data.grid.withIndex().chunked(data.size.cols).map { row ->
                row.map { (i, ch) ->
                    if (ch == ".") {
                        Puzzle.Cell(cellType = Puzzle.CellType.BLOCK)
                    } else {
                        val backgroundShape =
                            if (data.highlighted.contains(i)) {
                                Puzzle.BackgroundShape.CIRCLE
                            } else {
                                Puzzle.BackgroundShape.NONE
                            }
                        Puzzle.Cell(
                            solution = ch,
                            backgroundShape = backgroundShape,
                        )
                    }
                }
            },
            acrossClues = getClues(data.clues, 0),
            downClues = getClues(data.clues, 1),
        )
    }

    private fun extractText(htmlTag: CrosshareJson.HtmlTag): String {
        // For now, just join all text elements together. We can make this more sophisticated if we see more examples of
        // how this looks in the wild.
        val myText = if (htmlTag.type == "text" && htmlTag.value != null) listOf(htmlTag.value) else listOf()
        return (myText + htmlTag.children.map { extractText(it) }).joinToString(" ")
    }

    private fun getClues(clues: List<CrosshareJson.Clue>, direction: Int): Map<Int, String> {
        return clues.filter { it.dir == direction }.associate { it.num to it.clue }
    }
}