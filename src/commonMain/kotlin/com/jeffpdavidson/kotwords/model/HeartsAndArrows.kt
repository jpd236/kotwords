package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.formats.Puzzleable
import kotlinx.serialization.Serializable
import kotlin.math.floor
import kotlin.math.roundToInt

@Serializable
data class HeartsAndArrows(
    val title: String,
    val creator: String,
    val copyright: String,
    val description: String,
    val solutionGrid: List<List<Char>>,
    val arrows: List<List<RowsGarden.Entry>>,
    val light: List<RowsGarden.Entry>,
    val medium: List<RowsGarden.Entry>,
    val dark: List<RowsGarden.Entry>,
    val lightHeartColor: String = "#FFFFFF",
    val mediumHeartColor: String = "#F4BABA",
    val darkHeartColor: String = "#E06666",
    val addWordCount: Boolean = false,
    val addHyphenated: Boolean = false,
    val hasHtmlClues: Boolean = false,
    val labelHearts: Boolean = true,
) : Puzzleable() {
    override suspend fun createPuzzle(): Puzzle {
        val originX = solutionGrid[0].indexOfFirst { it != '.' }

        return RowsGarden.createPuzzle(
            title = title,
            creator = creator,
            copyright = copyright,
            description = description,
            solutionGrid = solutionGrid,
            rows = arrows,
            light = light,
            medium = medium,
            dark = dark,
            lightColor = lightHeartColor,
            mediumColor = mediumHeartColor,
            darkColor = darkHeartColor,
            addWordCount = addWordCount,
            addHyphenated = addHyphenated,
            hasHtmlClues = hasHtmlClues,
            labelBlooms = labelHearts,
            rowsListTitle = "Arrows",
            bloomsListTitle = "Hearts",
            emptyCellType = Puzzle.CellType.VOID,
            bloomTypeForCoordinate = { x, y ->
                val heartTopLeft = when ((3 * y - x + originX).mod(8)) {
                    0 -> x to y
                    1 -> x - 2 to y - 1
                    2 -> x - 1 to y - 1
                    3 -> x to y - 1
                    4 -> x - 2 to y - 2
                    5 -> x - 1 to y - 2
                    6 -> x to y - 2
                    else -> x - 1 to y
                }
                val heartX = (heartTopLeft.first + heartTopLeft.second - originX) / 4
                val heartY = (3 * heartTopLeft.second - heartTopLeft.first + originX) / 8
                when ((heartX + heartY).mod(3)) {
                    0 -> RowsGarden.BloomType.DARK
                    1 -> RowsGarden.BloomType.LIGHT
                    else -> RowsGarden.BloomType.MEDIUM
                }
            },
            isStartOfBloom = { x, y -> (3 * y - x + originX).mod(8) == 7 },
            getBloomCoordinates = { x, y ->
                listOf(
                    Puzzle.Coordinate(x = x - 1, y = y),
                    Puzzle.Coordinate(x = x, y = y),
                    Puzzle.Coordinate(x = x, y = y + 1),
                    Puzzle.Coordinate(x = x + 1, y = y + 1),
                    Puzzle.Coordinate(x = x + 1, y = y + 2),
                    Puzzle.Coordinate(x = x, y = y + 2),
                    Puzzle.Coordinate(x = x - 1, y = y + 2),
                    Puzzle.Coordinate(x = x - 1, y = y + 1),
                )
            },
        )
    }
}