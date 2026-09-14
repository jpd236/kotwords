package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.formats.Ipuz
import com.jeffpdavidson.kotwords.formats.JpzFile
import com.jeffpdavidson.kotwords.formats.pdf.getNotoSerifFontFamily
import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readStringResource
import io.github.pdvrieze.xmlutil.testutil.assertXmlEquals
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GoingTooFarTest {

    @Test
    fun jpzGeneration() = runTest {
        val puzzle = GOING_TOO_FAR.asPuzzle()
        val expected = readStringResource(GoingTooFarTest::class, "going-too-far/going-too-far.jpz")
        assertXmlEquals(expected, puzzle.asJpz().toXmlString())
    }

    @Test
    fun ipuzGeneration() = runTest {
        val puzzle = GOING_TOO_FAR.asPuzzle()
        val expected = readStringResource(GoingTooFarTest::class, "going-too-far/going-too-far.ipuz")
        assertEquals(expected, Ipuz.asIpuzJson(puzzle).toJsonString())
    }

    @Test
    fun pdfGeneration() = runTest {
        assertContentEquals(
            readBinaryResource(GoingTooFarTest::class, "going-too-far/going-too-far.pdf"),
            JpzFile(readBinaryResource(GoingTooFarTest::class, "going-too-far/going-too-far.jpz"))
                .asPuzzle().asPdf(blackSquareLightnessAdjustment = 0.75, fontFamily = getNotoSerifFontFamily())
        )
    }

    @Test
    fun invalidQuoteLength_fails() {
        assertFailsWith<IllegalArgumentException> {
            GOING_TOO_FAR.copy(quote = "TOO LONG")
        }
    }

    companion object {
        private val CROSSWORD = Crossword(
            title = "Test title",
            creator = "Test creator",
            copyright = "Test copyright",
            description = "Test description",
            grid = listOf(
                listOf(
                    Puzzle.Cell(solution = "A"),
                    Puzzle.Cell(solution = "B"),
                    Puzzle.Cell(cellType = Puzzle.CellType.BLOCK)
                ),
                listOf(
                    Puzzle.Cell(solution = "C"),
                    Puzzle.Cell(solution = "D"),
                    Puzzle.Cell(solution = "E")
                ),
                listOf(
                    Puzzle.Cell(cellType = Puzzle.CellType.BLOCK),
                    Puzzle.Cell(solution = "F"),
                    Puzzle.Cell(solution = "G")
                ),
            ),
            acrossClues = mapOf(
                1 to "Across 1",
                3 to "Across 3",
                5 to "Across 5",
            ),
            downClues = mapOf(
                1 to "Down 1",
                2 to "Down 2",
                4 to "Down 4",
            ),
        )

        val GOING_TOO_FAR = GoingTooFar(
            crossword = CROSSWORD,
            quote = "HI",
        )
    }
}
