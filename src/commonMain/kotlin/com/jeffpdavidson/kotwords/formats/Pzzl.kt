package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.model.Crossword
import com.jeffpdavidson.kotwords.model.Puzzle
import korlibs.time.DateFormat

private val DATE_FORMAT = DateFormat("yyMMdd")
private val CREATOR_DESCRIPTION_PATTERN = "(?:<NOTEPAD>(.*)</NOTEPAD>)?(.*)".toRegex()

// % means the following cell is circled.
// ^ means the following cell is shaded.
// For rebus squares, each character is separated by commas.
// . is used for placeholder cells which should be ignored.
private val CELL_PATTERN = "[%\\^]*.(?:,.)*".toRegex()

/** Container for a puzzle in the PZZL text format. */
class Pzzl(val data: String) : DelegatingPuzzleable() {
    override suspend fun getPuzzleable(): Puzzleable {
        val lines = data.lines()
        val creatorDescription = CREATOR_DESCRIPTION_PATTERN.matchEntire(lines[6])
        val height = lines[10].toInt()
        val acrossClueCount = lines[12].toInt()
        val downClueCount = lines[14].toInt()
        val gridData = lines.subList(16, 16 + height)
        val acrossClueList = lines.subList(17 + height, 17 + height + acrossClueCount)
        val downClueList = lines.subList(18 + height + acrossClueCount, 18 + height + acrossClueCount + downClueCount)
        val grid = gridData.map { row ->
            CELL_PATTERN.findAll(row).mapNotNull { matchResult -> parseCell(matchResult.value) }.toList()
        }.filterNot { it.isEmpty() }
        val acrossClues = mutableMapOf<Int, String>()
        val downClues = mutableMapOf<Int, String>()
        var acrossClueIndex = 0
        var downClueIndex = 0
        Crossword.forEachNumberedCell(grid) { _, _, clueNumber, isAcross, isDown ->
            if (isAcross) {
                acrossClues[clueNumber] = acrossClueList[acrossClueIndex++]
            }
            if (isDown) {
                downClues[clueNumber] = downClueList[downClueIndex++]
            }
        }
        return Crossword(
            title = lines[4],
            creator = creatorDescription?.groupValues?.get(2) ?: "",
            copyright = "",
            description = creatorDescription?.groupValues?.get(1) ?: "",
            grid = grid,
            acrossClues = acrossClues,
            downClues = downClues,
        )
    }

    private fun parseCell(cellData: String, isCircled: Boolean = false, isShaded: Boolean = false): Puzzle.Cell? {
        return when (cellData[0]) {
            '.' -> null
            '%' -> parseCell(cellData.substring(1), isCircled = true, isShaded)
            '^' -> parseCell(cellData.substring(1), isCircled, isShaded = true)
            '#' -> Puzzle.Cell(cellType = Puzzle.CellType.BLOCK)
            else -> Puzzle.Cell(
                solution = cellData.replace(",", ""),
                backgroundShape = if (isCircled) {
                    Puzzle.BackgroundShape.CIRCLE
                } else {
                    Puzzle.BackgroundShape.NONE
                },
                backgroundColor = if (isShaded) "#dcdcdc" else "",
            )
        }
    }
}