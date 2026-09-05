package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.formats.Puzzleable

data class Cascades(
    val title: String,
    val creator: String,
    val copyright: String,
    val description: String,
    val grid: List<List<String>>,
    val rowClues: List<List<String>>,
    val cascadeClues: List<List<String>>,
    val includeRowNumbers: Boolean,
    val lightCascadeColor: String,
    val darkCascadeColor: String,
) : Puzzleable() {

    init {
        val height = grid.size
        require(height >= 3) {
            "Grid height must be at least 3, but was $height"
        }
        val width = height + 1
        require(grid.all { it.size == width }) {
            "Grid width must be height + 1 ($width), but at least one row differed"
        }
        require(rowClues.size == height) {
            "Grid has height $height but has ${rowClues.size} row clue sets"
        }
        val cascadeCount = height - 1
        require(cascadeClues.size == cascadeCount) {
            "Grid has $cascadeCount cascades but has ${cascadeClues.size} cascade clue sets"
        }
    }

    override suspend fun createPuzzle(): Puzzle {
        val height = grid.size
        val width = height + 1

        fun isVoid(x: Int, y: Int): Boolean = (x == width - 1 && y == 0) || (x == 0 && y == height - 1)

        fun getCascadeIndex(x: Int, y: Int): Int = (height - 1 - (x - y)) / 2

        val cascadeCells = (0 until height - 1).map { mutableListOf<Puzzle.Coordinate>() }
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (!isVoid(x, y)) {
                    cascadeCells[getCascadeIndex(x, y)].add(Puzzle.Coordinate(x, y))
                }
            }
        }

        val cascadeStartCoordinates = cascadeCells.mapNotNull { it.firstOrNull() }.toSet()
        val cascadeLabels = (0 until height - 1).map { getCascadeLabel(it) }

        val puzzleGrid = (0 until height).map { y ->
            val firstNonVoidX = (0 until width).firstOrNull { !isVoid(it, y) }
            (0 until width).map { x ->
                if (isVoid(x, y)) {
                    Puzzle.Cell(cellType = Puzzle.CellType.VOID)
                } else {
                    val c = getCascadeIndex(x, y)
                    val isRowStart = x == firstNonVoidX
                    val isCascadeStart = cascadeStartCoordinates.contains(Puzzle.Coordinate(x, y))
                    val cascadeLabel = cascadeLabels[c]

                    val number = if (includeRowNumbers) {
                        if (isRowStart) "${y + 1}" else ""
                    } else {
                        if (isCascadeStart) cascadeLabel else ""
                    }
                    val topRightNumber = if (includeRowNumbers && isCascadeStart) cascadeLabel else ""

                    val borders = mutableSetOf<Puzzle.BorderDirection>()
                    if (y > 0 && !isVoid(x, y - 1) && getCascadeIndex(x, y - 1) != c) {
                        borders.add(Puzzle.BorderDirection.TOP)
                    }
                    if (y < height - 1 && !isVoid(x, y + 1) && getCascadeIndex(x, y + 1) != c) {
                        borders.add(Puzzle.BorderDirection.BOTTOM)
                    }
                    if (x > 0 && !isVoid(x - 1, y) && getCascadeIndex(x - 1, y) != c) {
                        borders.add(Puzzle.BorderDirection.LEFT)
                    }
                    if (x < width - 1 && !isVoid(x + 1, y) && getCascadeIndex(x + 1, y) != c) {
                        borders.add(Puzzle.BorderDirection.RIGHT)
                    }

                    Puzzle.Cell(
                        solution = grid[y][x].uppercase(),
                        backgroundColor = if (c % 2 == 0) darkCascadeColor else lightCascadeColor,
                        number = number,
                        topRightNumber = topRightNumber,
                        borderDirections = borders,
                    )
                }
            }
        }

        val (rowClueList, rowWordList) = rowClues.mapIndexed { y, clues ->
            val cells = (0 until width).filter { !isVoid(it, y) }.map { x -> Puzzle.Coordinate(x, y) }
            Puzzle.Clue(y + 1, "${y + 1}", clues.joinToString(" / ")) to Puzzle.Word(y + 1, cells)
        }.unzip()

        val (cascadeClueList, cascadeWordList) = cascadeClues.mapIndexed { c, clues ->
            val wordId = 1001 + c
            val label = cascadeLabels[c]
            Puzzle.Clue(wordId, label, clues.joinToString(" / ")) to Puzzle.Word(wordId, cascadeCells[c])
        }.unzip()

        return Puzzle(
            title = title,
            creator = creator,
            copyright = copyright,
            description = description,
            grid = puzzleGrid,
            clues = listOf(
                Puzzle.ClueList("Rows", rowClueList),
                Puzzle.ClueList("Cascades", cascadeClueList),
            ),
            words = rowWordList + cascadeWordList,
        )
    }

    companion object {
        internal fun getCascadeLabel(clueIndex: Int): String = "${'A' + (clueIndex % 26)}".repeat(clueIndex / 26 + 1)
    }
}
