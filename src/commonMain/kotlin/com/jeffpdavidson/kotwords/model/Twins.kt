package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.formats.Puzzleable
import com.jeffpdavidson.kotwords.util.trimmedLines

data class Twins(
    val title: String,
    val creator: String,
    val copyright: String,
    val description: String,
    val grid1: List<List<Puzzle.Cell>>,
    val grid2: List<List<Puzzle.Cell>>,
    val acrossClues: List<List<String>>,
    val downClues: List<List<String>>,
    val hasHtmlClues: Boolean = false,
) : Puzzleable() {

    init {
        require(grid1.isNotEmpty() && grid1[0].isNotEmpty()) {
            "Grid 1 must not be empty"
        }
        val width = grid1[0].size
        val height = grid1.size
        require(grid1.all { it.size == width }) {
            "Grid 1 must be rectangular"
        }
        require(grid2.size == height && grid2.all { it.size == width }) {
            "Grid 2 dimensions must match Grid 1"
        }
        (0 until height).forEach { y ->
            (0 until width).forEach { x ->
                require(grid1[y][x].cellType.isBlack() == grid2[y][x].cellType.isBlack()) {
                    "Grid 1 and Grid 2 must have identical black square patterns at ($x, $y)"
                }
            }
        }

        var acrossCount = 0
        var downCount = 0
        Crossword.forEachNumberedCell(grid1, useBorders = false) { _, _, _, isAcross, isDown ->
            if (isAcross) acrossCount++
            if (isDown) downCount++
        }
        require(acrossClues.size == acrossCount) {
            "Incorrect number of across clues: expected $acrossCount, but was ${acrossClues.size}"
        }
        require(downClues.size == downCount) {
            "Incorrect number of down clues: expected $downCount, but was ${downClues.size}"
        }
        require(acrossClues.all { it.size == 2 }) {
            "Each across entry must have two clues"
        }
        require(downClues.all { it.size == 2 }) {
            "Each down entry must have two clues"
        }
    }

    override suspend fun createPuzzle(): Puzzle {
        val width = grid1[0].size
        val height = grid1.size

        val acrossCluesMap = mutableMapOf<Int, List<String>>()
        val downCluesMap = mutableMapOf<Int, List<String>>()
        val gridNumbers = mutableMapOf<Pair<Int, Int>, Int>()
        var acrossIdx = 0
        var downIdx = 0
        Crossword.forEachNumberedCell(grid1, useBorders = false) { x, y, clueNumber, isAcross, isDown ->
            gridNumbers[x to y] = clueNumber
            if (isAcross) {
                acrossCluesMap[clueNumber] = acrossClues[acrossIdx++]
            }
            if (isDown) {
                downCluesMap[clueNumber] = downClues[downIdx++]
            }
        }

        val puzzleWords = mutableListOf<Puzzle.Word>()
        val acrossPuzzleClues = mutableListOf<Puzzle.Clue>()
        val downPuzzleClues = mutableListOf<Puzzle.Clue>()
        var firstAcrossCells = setOf<Pair<Int, Int>>()

        Crossword.forEachClue(
            grid1,
            acrossCluesMap.mapValues { "" },
            downCluesMap.mapValues { "" },
            useBorders = false
        ) { isAcross, clueNumber, _, cells1 ->
            if (isAcross && firstAcrossCells.isEmpty()) {
                firstAcrossCells = cells1.map { it.x to it.y }.toSet()
            }
            val cells2 = cells1.map { Puzzle.Coordinate(x = it.x + width + 1, y = it.y) }
            val combinedCells = cells1 + cells2
            val wordId = if (isAcross) clueNumber else 1000 + clueNumber
            puzzleWords += Puzzle.Word(id = wordId, cells = combinedCells)
            val cluePair = (if (isAcross) acrossCluesMap else downCluesMap)[clueNumber] ?: listOf("", "")
            val targetClues = if (isAcross) acrossPuzzleClues else downPuzzleClues
            targetClues += Puzzle.Clue(
                wordId = wordId,
                number = "$clueNumber",
                text = cluePair.joinToString("\n"),
            )
        }

        fun createGridRow(sourceGrid: List<List<Puzzle.Cell>>, y: Int): List<Puzzle.Cell> =
            (0 until width).map { x ->
                val cell = sourceGrid[y][x]
                if (cell.cellType.isBlack()) {
                    cell
                } else {
                    val isFirstAcross = (x to y) in firstAcrossCells
                    cell.copy(
                        number = gridNumbers[x to y]?.toString() ?: "",
                        entry = if (isFirstAcross) cell.solution else cell.entry,
                        hint = if (isFirstAcross) true else cell.hint,
                    )
                }
            }

        val combinedGrid = (0 until height).map { y ->
            createGridRow(grid1, y) + Puzzle.Cell(cellType = Puzzle.CellType.VOID) + createGridRow(grid2, y)
        }

        val acrossTitle = if (hasHtmlClues) "<b>Across</b>" else "Across"
        val downTitle = if (hasHtmlClues) "<b>Down</b>" else "Down"
        val clueLists = listOf(
            Puzzle.ClueList(acrossTitle, acrossPuzzleClues),
            Puzzle.ClueList(downTitle, downPuzzleClues),
        )

        return Puzzle(
            title = title,
            creator = creator,
            copyright = copyright,
            description = description,
            grid = combinedGrid,
            clues = clueLists,
            words = puzzleWords.sortedBy { it.id },
            hasHtmlClues = hasHtmlClues,
        )
    }

    companion object {
        internal fun fromRawInput(
            title: String,
            creator: String,
            copyright: String,
            description: String,
            grid1: String,
            grid2: String,
            acrossClues: String,
            downClues: String,
            hasHtmlClues: Boolean = false,
        ): Twins {
            return Twins(
                title = title,
                creator = creator,
                copyright = copyright,
                description = description,
                grid1 = parseRawGrid(grid1),
                grid2 = parseRawGrid(grid2),
                acrossClues = parseRawClues(acrossClues),
                downClues = parseRawClues(downClues),
                hasHtmlClues = hasHtmlClues,
            )
        }

        private fun parseRawGrid(grid: String): List<List<Puzzle.Cell>> {
            return grid.uppercase().trimmedLines().map { row ->
                val columns = if (row.contains("\\s".toRegex())) {
                    row.split("\\s+".toRegex())
                } else {
                    row.map { "$it" }
                }
                columns.map { col ->
                    when (col) {
                        "." -> Puzzle.Cell(cellType = Puzzle.CellType.BLOCK)
                        "-" -> Puzzle.Cell(solution = "")
                        else -> Puzzle.Cell(solution = col)
                    }
                }
            }
        }

        private fun parseRawClues(clues: String): List<List<String>> {
            return clues.trimmedLines().map { line ->
                line.split("/").map { it.trim() }
            }
        }
    }
}
