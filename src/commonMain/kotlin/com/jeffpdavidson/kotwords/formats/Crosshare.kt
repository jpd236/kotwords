package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.json.CrosshareJson
import com.jeffpdavidson.kotwords.formats.json.JsonSerializer
import com.jeffpdavidson.kotwords.model.BLACK_SQUARE
import com.jeffpdavidson.kotwords.model.Crossword
import com.jeffpdavidson.kotwords.model.Square

class Crosshare(private val json: String) : Crosswordable {
    override fun asCrossword(): Crossword {
        val data = JsonSerializer.fromJson<CrosshareJson.Data>(json).props.pageProps.puzzle
        val author =
            if (data.guestConstructor?.isNotEmpty() == true) {
                "${data.guestConstructor} / Published by ${data.authorName}"
            } else {
                data.authorName
            }
        return Crossword(
            title = data.title,
            author = author,
            copyright = data.copyright ?: "",
            notes = data.constructorNotes ?: "",
            grid = data.grid.withIndex().chunked(data.size.cols).map { row ->
                row.map { (i, ch) ->
                    if (ch == ".") {
                        BLACK_SQUARE
                    } else {
                        Square(
                            solution = ch[0],
                            solutionRebus = if (ch.length > 1) ch else "",
                            isCircled = data.highlighted.contains(i),
                        )
                    }
                }
            },
            acrossClues = getClues(data.clues, 0),
            downClues = getClues(data.clues, 1),
        )
    }

    private fun getClues(clues: List<CrosshareJson.Clue>, direction: Int): Map<Int, String> {
        return clues.filter { it.dir == direction }.associate { it.num to it.clue }
    }
}