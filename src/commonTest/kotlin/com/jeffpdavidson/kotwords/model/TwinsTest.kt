package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.formats.Ipuz
import com.jeffpdavidson.kotwords.formats.pdf.getNotoSerifFontFamily
import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readStringResource
import io.github.pdvrieze.xmlutil.testutil.assertXmlEquals
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TwinsTest {

    @Test
    fun jpzGeneration() = runTest {
        val puzzle = TWINS.asPuzzle()
        val expected = readStringResource(TwinsTest::class, "twins/twins.jpz")
        assertXmlEquals(expected, puzzle.asJpz().toXmlString())
    }

    @Test
    fun ipuzGeneration() = runTest {
        val puzzle = TWINS.asPuzzle()
        val expected = readStringResource(TwinsTest::class, "twins/twins.ipuz")
        assertEquals(expected, Ipuz.asIpuzJson(puzzle).toJsonString())
    }

    @Test
    fun pdfGeneration() = runTest {
        assertContentEquals(
            readBinaryResource(TwinsTest::class, "twins/twins.pdf"),
            TWINS.asPuzzle().asPdf(blackSquareLightnessAdjustment = 0.75, fontFamily = getNotoSerifFontFamily())
        )
    }

    @Test
    fun fromRawInput() {
        val grid1 = """
            ABCD
            EFG.
            IJKL
            .MNO
        """.trimIndent()
        val grid2 = """
            PQRS
            TUV.
            WXYZ
            .ABC
        """.trimIndent()
        val acrossClues = """
            Across 1 Clue 1 / Across 1 Clue 2
            Across 4 Clue 1 / Across 4 Clue 2
            Across 5 Clue 1 / Across 5 Clue 2
            Across 7 Clue 1 / Across 7 Clue 2
        """.trimIndent()
        val downClues = """
            Down 1 Clue 1 / Down 1 Clue 2
            Down 2 Clue 1 / Down 2 Clue 2
            Down 3 Clue 1 / Down 3 Clue 2
            Down 6 Clue 1 / Down 6 Clue 2
        """.trimIndent()

        val parsed = Twins.fromRawInput(
            title = "Test title",
            creator = "Test creator",
            copyright = "Test copyright",
            description = "Test description",
            grid1 = grid1,
            grid2 = grid2,
            acrossClues = acrossClues,
            downClues = downClues,
        )
        assertEquals(TWINS, parsed)
    }

    @Test
    fun dimensionMismatch_fails() {
        assertFailsWith<IllegalArgumentException> {
            TWINS.copy(
                grid2 = listOf(
                    listOf(Puzzle.Cell(solution = "A"), Puzzle.Cell(solution = "B")),
                )
            )
        }
    }

    @Test
    fun blackSquareMismatch_fails() {
        assertFailsWith<IllegalArgumentException> {
            TWINS.copy(
                grid2 = TWINS.grid2.mapIndexed { y, row ->
                    row.mapIndexed { x, cell ->
                        if (x == 0 && y == 0) Puzzle.Cell(cellType = Puzzle.CellType.BLOCK) else cell
                    }
                }
            )
        }
    }

    @Test
    fun clueCountMismatch_fails() {
        assertFailsWith<IllegalArgumentException> {
            TWINS.copy(acrossClues = TWINS.acrossClues.drop(1))
        }
    }

    @Test
    fun clueWithoutDelimiter_fails() {
        assertFailsWith<IllegalArgumentException> {
            TWINS.copy(acrossClues = TWINS.acrossClues.take(3) + listOf(listOf("Missing delimiter clue")))
        }
    }

    companion object {
        private val TWINS = Twins(
            title = "Test title",
            creator = "Test creator",
            copyright = "Test copyright",
            description = "Test description",
            grid1 = listOf(
                listOf(
                    Puzzle.Cell(solution = "A"),
                    Puzzle.Cell(solution = "B"),
                    Puzzle.Cell(solution = "C"),
                    Puzzle.Cell(solution = "D")
                ),
                listOf(
                    Puzzle.Cell(solution = "E"),
                    Puzzle.Cell(solution = "F"),
                    Puzzle.Cell(solution = "G"),
                    Puzzle.Cell(cellType = Puzzle.CellType.BLOCK)
                ),
                listOf(
                    Puzzle.Cell(solution = "I"),
                    Puzzle.Cell(solution = "J"),
                    Puzzle.Cell(solution = "K"),
                    Puzzle.Cell(solution = "L")
                ),
                listOf(
                    Puzzle.Cell(cellType = Puzzle.CellType.BLOCK),
                    Puzzle.Cell(solution = "M"),
                    Puzzle.Cell(solution = "N"),
                    Puzzle.Cell(solution = "O")
                ),
            ),
            grid2 = listOf(
                listOf(
                    Puzzle.Cell(solution = "P"),
                    Puzzle.Cell(solution = "Q"),
                    Puzzle.Cell(solution = "R"),
                    Puzzle.Cell(solution = "S")
                ),
                listOf(
                    Puzzle.Cell(solution = "T"),
                    Puzzle.Cell(solution = "U"),
                    Puzzle.Cell(solution = "V"),
                    Puzzle.Cell(cellType = Puzzle.CellType.BLOCK)
                ),
                listOf(
                    Puzzle.Cell(solution = "W"),
                    Puzzle.Cell(solution = "X"),
                    Puzzle.Cell(solution = "Y"),
                    Puzzle.Cell(solution = "Z")
                ),
                listOf(
                    Puzzle.Cell(cellType = Puzzle.CellType.BLOCK),
                    Puzzle.Cell(solution = "A"),
                    Puzzle.Cell(solution = "B"),
                    Puzzle.Cell(solution = "C")
                ),
            ),
            acrossClues = listOf(
                listOf("Across 1 Clue 1", "Across 1 Clue 2"),
                listOf("Across 4 Clue 1", "Across 4 Clue 2"),
                listOf("Across 5 Clue 1", "Across 5 Clue 2"),
                listOf("Across 7 Clue 1", "Across 7 Clue 2"),
            ),
            downClues = listOf(
                listOf("Down 1 Clue 1", "Down 1 Clue 2"),
                listOf("Down 2 Clue 1", "Down 2 Clue 2"),
                listOf("Down 3 Clue 1", "Down 3 Clue 2"),
                listOf("Down 6 Clue 1", "Down 6 Clue 2"),
            ),
        )
    }
}
