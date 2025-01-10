package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.formats.Puzzleable

data class MarchingBands(
    val title: String,
    val creator: String,
    val copyright: String,
    val description: String,
    val grid: List<List<String>>,
    val bandClues: List<List<String>>,
    val rowClues: List<List<String>>,
    val includeRowNumbers: Boolean,
    val lightBandColor: String,
    val darkBandColor: String,
) : Puzzleable() {
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

    override suspend fun createPuzzle(): Puzzle {
        val puzzleGrid = grid.mapIndexed { y, row ->
            row.mapIndexed { x, cell ->
                if (cell.isEmpty()) {
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
                        solution = cell,
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
            val cells = getBandCells(width = puzzleGrid[i].size, height = puzzleGrid.size, bandIndex = i)
            Puzzle.Clue(1000 + i + 1, "${'A' + i}", clues.joinToString(" / ")) to Puzzle.Word(1000 + i + 1, cells)
        }.unzip()
        return Puzzle(
            title = title,
            creator = creator,
            copyright = copyright,
            description = description,
            grid = puzzleGrid,
            clues = listOf(Puzzle.ClueList("Rows", rowClueList), Puzzle.ClueList("Bands", bandClueList)),
            words = bandWordList + rowWordList,
        )
    }

    companion object {
        /** Return the coordinates in band [bandIndex] in a grid of size width x height. */
        internal fun getBandCells(width: Int, height: Int, bandIndex: Int): List<Puzzle.Coordinate> {
            return (bandIndex until width - bandIndex).map { x -> Puzzle.Coordinate(x = x, y = bandIndex) } +
                    (bandIndex + 1 until height - bandIndex)
                        .map { y -> Puzzle.Coordinate(x = width - bandIndex - 1, y = y) } +
                    (bandIndex until width - bandIndex - 1)
                        .map { x -> Puzzle.Coordinate(x = x, y = height - bandIndex - 1) }.reversed() +
                    (bandIndex + 1 until height - bandIndex - 1).map { y -> Puzzle.Coordinate(x = bandIndex, y = y) }
                        .reversed()
        }
    }
}