package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.formats.PdfComparator.assertPdfEquals
import com.jeffpdavidson.kotwords.formats.getNotoSerifFontFamily
import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readStringResource
import com.jeffpdavidson.kotwords.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

// TODO: Expand test coverage
class TwistsAndTurnsTest {
    @Test
    fun jpzGeneration() = runTest {
        val expected = readStringResource(TwistsAndTurnsTest::class, "twists-and-turns.jpz")
        assertEquals(expected, puzzle.asPuzzle().asJpzFile().toXmlString())
    }

    @Test
    fun pdfGeneration_unsortedTwists() = runTest {
        val expected = readBinaryResource(TwistsAndTurnsTest::class, "twists-and-turns/unsorted-twists.pdf")
        assertPdfEquals(
            expected, puzzle.asPdf(
                blackSquareLightnessAdjustment = 0.5f,
                fontFamily = getNotoSerifFontFamily(),
                sortTwists = false
            )
        )
    }

    @Test
    fun pdfGeneration_sortedTwists() = runTest {
        val expected = readBinaryResource(TwistsAndTurnsTest::class, "twists-and-turns/sorted-twists.pdf")
        assertPdfEquals(
            expected, puzzle.asPdf(
                blackSquareLightnessAdjustment = 0.5f,
                fontFamily = getNotoSerifFontFamily(),
                sortTwists = true
            )
        )
    }

    companion object {
        private val puzzle = TwistsAndTurns(
            title = "Test title",
            creator = "Test creator",
            copyright = "Test copyright",
            description = "Test description",
            width = 6,
            height = 6,
            twistBoxSize = 3,
            turnsAnswers = listOf("ABCDE", "FGHIJ", "KLM", "NOPQR", "STUVWXY", "ZABCD", "EFGHIJ"),
            turnsClues = listOf("Turn 1", "Turn 2", "Turn 3", "Turn 4", "Turn 5", "Turn 6", "Turn 7"),
            twistsClues = listOf("D Twist 1", "C Twist 2", "B Twist 3", "A Twist 4"),
            lightTwistsColor = "#FFFFFF",
            darkTwistsColor = "#999999",
            crosswordSolverSettings = Puzzle.CrosswordSolverSettings("#00b100", "#80ff80", "All done!"),
        )
    }
}