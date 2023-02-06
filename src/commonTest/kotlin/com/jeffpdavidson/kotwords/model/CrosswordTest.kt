package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.formats.JpzFile
import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readStringResource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CrosswordTest {
    @Test
    fun asJpz() = runTest {
        val crossword = Crossword(
            title = "Example Puzzle for Kotwords",
            creator = "Jeff Davidson",
            copyright = "© 2018 Jeff Davidson",
            description = "Notepad text goes here.",
            grid = listOf(
                listOf(
                    Puzzle.Cell("A"),
                    Puzzle.Cell("B"),
                    Puzzle.Cell("C"),
                    Puzzle.Cell("D"),
                    Puzzle.Cell(cellType = Puzzle.CellType.BLOCK),
                ),
                listOf(
                    Puzzle.Cell("E"),
                    Puzzle.Cell("F"),
                    Puzzle.Cell("G"),
                    Puzzle.Cell("H"),
                    Puzzle.Cell(cellType = Puzzle.CellType.BLOCK),
                ),
                listOf(
                    Puzzle.Cell("I", backgroundShape = Puzzle.BackgroundShape.CIRCLE),
                    Puzzle.Cell("J"),
                    Puzzle.Cell("XYZ"),
                    Puzzle.Cell("L"),
                    Puzzle.Cell("M", backgroundShape = Puzzle.BackgroundShape.CIRCLE),
                ),
                listOf(
                    Puzzle.Cell(cellType = Puzzle.CellType.BLOCK),
                    Puzzle.Cell("N"),
                    Puzzle.Cell("O"),
                    Puzzle.Cell("P"),
                    Puzzle.Cell("Q"),
                ),
                listOf(
                    Puzzle.Cell(cellType = Puzzle.CellType.BLOCK),
                    Puzzle.Cell("1"),
                    Puzzle.Cell("2"),
                    Puzzle.Cell("$"),
                    Puzzle.Cell("@"),
                ),
            ),
            acrossClues = mapOf(
                1 to "First across clue with special characters αβγδε",
                5 to "Second across clue",
                6 to "Third across clue",
                8 to "Fourth across clue",
                9 to "Fifth across clue",
            ),
            downClues = mapOf(
                1 to "First down clue",
                2 to "Second down clue",
                3 to "Third down clue",
                4 to "Fourth down clue",
                7 to "Fifth down clue",
            ),
            hasHtmlClues = true,
        )

        assertEquals(
            JpzFile(readBinaryResource(CrosswordTest::class, "jpz/test.jpz")).asPuzzle(),
            crossword.asPuzzle(),
        )
    }

    @Test
    fun asJpz_barredGrid() = runTest {
        val crossword = Crossword(
            title = "Example Puzzle for Kotwords",
            creator = "Jeff Davidson",
            copyright = "© 2018 Jeff Davidson",
            description = "Notepad text goes here.",
            grid = listOf(
                listOf(
                    Puzzle.Cell("A"),
                    Puzzle.Cell("B"),
                    Puzzle.Cell("C"),
                    Puzzle.Cell("D"),
                    Puzzle.Cell("E", borderDirections = setOf(Puzzle.BorderDirection.LEFT)),
                ),
                listOf(
                    Puzzle.Cell("F", borderDirections = setOf(Puzzle.BorderDirection.TOP)),
                    Puzzle.Cell("G"),
                    Puzzle.Cell("H"),
                    Puzzle.Cell("I"),
                    Puzzle.Cell("J"),
                ),
                listOf(
                    Puzzle.Cell("K"),
                    Puzzle.Cell("L", borderDirections = setOf(Puzzle.BorderDirection.LEFT)),
                    Puzzle.Cell(cellType = Puzzle.CellType.BLOCK),
                    Puzzle.Cell("M", borderDirections = setOf(Puzzle.BorderDirection.LEFT)),
                    Puzzle.Cell("N", borderDirections = setOf(Puzzle.BorderDirection.LEFT)),
                ),
                listOf(
                    Puzzle.Cell("O"),
                    Puzzle.Cell("P"),
                    Puzzle.Cell("Q"),
                    Puzzle.Cell("R"),
                    Puzzle.Cell("S"),
                ),
                listOf(
                    Puzzle.Cell("T"),
                    Puzzle.Cell("U", borderDirections = setOf(Puzzle.BorderDirection.LEFT)),
                    Puzzle.Cell("V"),
                    Puzzle.Cell("W"),
                    Puzzle.Cell("X", borderDirections = setOf(Puzzle.BorderDirection.TOP)),
                ),
            ),
            acrossClues = mapOf(
                1 to "First across clue",
                6 to "Second across clue",
                7 to "Third across clue",
                9 to "Fourth across clue",
            ),
            downClues = mapOf(
                2 to "First down clue",
                3 to "Second down clue",
                4 to "Third down clue",
                5 to "Fourth down clue",
                6 to "Fifth down clue",
                8 to "Sixth down clue",
            ),
            hasHtmlClues = true,
        )

        assertEquals(
            readStringResource(CrosswordTest::class, "jpz/test-barred.jpz"),
            crossword.asPuzzle().asJpz().toXmlString(),
        )
    }

    @Test
    fun forEachNumberedCell_blackSquares() {
        val grid = listOf(
            listOf(Puzzle.Cell("A"), Puzzle.Cell("B"), Puzzle.Cell(cellType = Puzzle.CellType.BLOCK)),
            listOf(Puzzle.Cell("C"), Puzzle.Cell("D"), Puzzle.Cell("E")),
            listOf(Puzzle.Cell(cellType = Puzzle.CellType.BLOCK), Puzzle.Cell("F"), Puzzle.Cell("G")),
        )
        assertGridHasNumbers(
            grid,
            expectedAcrossCells = listOf(0 to 0 to 1, 0 to 1 to 3, 1 to 2 to 5),
            expectedDownCells = listOf(0 to 0 to 1, 1 to 0 to 2, 2 to 1 to 4),
        )
    }

    @Test
    fun forEachNumberedCell_bars() {
        val grid = listOf(
            listOf(
                Puzzle.Cell("A"),
                Puzzle.Cell("B"),
                Puzzle.Cell("C", borderDirections = setOf(Puzzle.BorderDirection.LEFT)),
            ),
            listOf(
                Puzzle.Cell("D", borderDirections = setOf(Puzzle.BorderDirection.TOP)),
                Puzzle.Cell("E"),
                Puzzle.Cell("F", borderDirections = setOf(Puzzle.BorderDirection.BOTTOM)),
            ),
            listOf(
                Puzzle.Cell("G", borderDirections = setOf(Puzzle.BorderDirection.RIGHT)),
                Puzzle.Cell("H"),
                Puzzle.Cell("I"),
            ),
        )
        assertGridHasNumbers(
            grid,
            expectedAcrossCells = listOf(0 to 0 to 1, 0 to 1 to 4, 1 to 2 to 5),
            expectedDownCells = listOf(1 to 0 to 2, 2 to 0 to 3, 0 to 1 to 4),
        )
    }

    @Test
    fun forEachNumberedCell_barsAndBlackSquares() {
        val grid = listOf(
            listOf(
                Puzzle.Cell("A"),
                Puzzle.Cell("B"),
                Puzzle.Cell("C", borderDirections = setOf(Puzzle.BorderDirection.LEFT)),
            ),
            listOf(
                Puzzle.Cell("D", borderDirections = setOf(Puzzle.BorderDirection.TOP)),
                Puzzle.Cell(cellType = Puzzle.CellType.BLOCK),
                Puzzle.Cell("E", borderDirections = setOf(Puzzle.BorderDirection.BOTTOM)),
            ),
            listOf(
                Puzzle.Cell("F", borderDirections = setOf(Puzzle.BorderDirection.RIGHT)),
                Puzzle.Cell("G"),
                Puzzle.Cell("H"),
            ),
        )
        assertGridHasNumbers(
            grid,
            expectedAcrossCells = listOf(0 to 0 to 1, 1 to 2 to 4),
            expectedDownCells = listOf(2 to 0 to 2, 0 to 1 to 3),
        )
    }

    private fun assertGridHasNumbers(
        grid: List<List<Puzzle.Cell>>,
        expectedAcrossCells: List<Pair<Pair<Int, Int>, Int>>,
        expectedDownCells: List<Pair<Pair<Int, Int>, Int>>,
    ) {
        val acrossCells = mutableListOf<Pair<Pair<Int, Int>, Int>>()
        val downCells = mutableListOf<Pair<Pair<Int, Int>, Int>>()
        Crossword.forEachNumberedCell(grid) { x, y, clueNumber, isAcross, isDown ->
            if (isAcross) acrossCells += x to y to clueNumber
            if (isDown) downCells += x to y to clueNumber
        }
        assertEquals(expectedAcrossCells, acrossCells)
        assertEquals(expectedDownCells, downCells)
    }
}