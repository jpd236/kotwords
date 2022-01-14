package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.formats.Puzzleable

data class MarchingBands(
    val title: String,
    val creator: String,
    val copyright: String,
    val description: String,
    val grid: List<List<Char?>>,
    val bandClues: List<List<String>>,
    val rowClues: List<List<String>>,
    val includeRowNumbers: Boolean,
    val lightBandColor: String,
    val darkBandColor: String,
) : Puzzleable {
    init {
        val height = grid.size
        require(grid.count { it.size == height } == height) {
            "Only square grids are supported, but at least one grid row has a width != $height"
        }
        require(rowClues.size == height) {
            "Grid has height $height but has ${rowClues.size} row clue sets"
        }
        val bandCount = height / 2
        require(bandClues.size == bandCount) {
            "Grid should have $bandCount bands but has ${bandClues.size} band clue sets"
        }
    }

    override suspend fun asPuzzle(): Puzzle {
        val puzzleGrid = grid.mapIndexed { y, row ->
            row.mapIndexed { x, ch ->
                if (ch == null) {
                    Puzzle.Cell(cellType = Puzzle.CellType.BLOCK)
                } else {
                    val rowNumber = if (x == 0) "${y + 1}" else ""
                    val bandLetter = if (x == y && x < grid.size / 2) "${'A' + x}" else ""
                    val backgroundColor =
                        if (listOf(x, y, grid.size - y - 1, grid[y].size - x - 1).minOrNull()!! % 2 == 0) {
                            lightBandColor
                        } else {
                            darkBandColor
                        }
                    Puzzle.Cell(
                        solution = "$ch",
                        number = if (includeRowNumbers) rowNumber else bandLetter,
                        topRightNumber = if (includeRowNumbers) bandLetter else "",
                        backgroundColor = backgroundColor
                    )
                }
            }
        }
        val (rowClueList, rowWordList) = rowClues.mapIndexed { y, clues ->
            val cells = puzzleGrid[y].mapIndexedNotNull { x, cell ->
                if (cell.cellType == Puzzle.CellType.BLOCK) null else Puzzle.Coordinate(x = x, y = y)
            }
            Puzzle.Clue(y + 1, "${y + 1}", clues.joinToString(" / ")) to Puzzle.Word(y + 1, cells)
        }.unzip()
        val (bandClueList, bandWordList) = bandClues.mapIndexed { i, clues ->
            val cells =
                (i until puzzleGrid[i].size - i).map { x -> Puzzle.Coordinate(x = x, y = i) } +
                        (i + 1 until puzzleGrid.size - i)
                            .map { y -> Puzzle.Coordinate(x = puzzleGrid[y].size - i - 1, y = y) } +
                        (i until puzzleGrid[i].size - i - 1)
                            .map { x -> Puzzle.Coordinate(x = x, y = puzzleGrid.size - i - 1) }.reversed() +
                        (i + 1 until puzzleGrid.size - i - 1).map { y -> Puzzle.Coordinate(x = i, y = y) }.reversed()
            Puzzle.Clue(1000 + i + 1, "${'A' + i}", clues.joinToString(" / ")) to Puzzle.Word(1000 + i + 1, cells)
        }.unzip()
        return Puzzle(
            title = title,
            creator = creator,
            copyright = copyright,
            description = description,
            grid = puzzleGrid,
            clues = listOf(Puzzle.ClueList("Bands", bandClueList), Puzzle.ClueList("Rows", rowClueList)),
            words = bandWordList + rowWordList,
        )
    }
}