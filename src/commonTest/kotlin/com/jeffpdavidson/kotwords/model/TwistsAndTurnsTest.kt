package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.formats.CrosswordCompilerApplet
import com.jeffpdavidson.kotwords.formats.pdf.GridCorner
import com.jeffpdavidson.kotwords.formats.pdf.getNotoSerifFontFamily
import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readStringResource
import io.github.pdvrieze.xmlutil.testutil.assertXmlEquals
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals

// TODO: Expand test coverage
class TwistsAndTurnsTest {
    @Test
    fun jpzGeneration() = runTest {
        val expected = readStringResource(TwistsAndTurnsTest::class, "twists-and-turns/twists-and-turns.jpz")
        assertXmlEquals(
            expected, puzzle.asPuzzle().asJpz(
                appletSettings = CrosswordCompilerApplet.AppletSettings(
                    cursorColor = "#00b100",
                    selectedCellsColor = "#80ff80",
                    completion = CrosswordCompilerApplet.AppletSettings.Completion(message = "All done!"),
                )
            ).toXmlString()
        )
    }

    @Test
    fun pdfGeneration_unsortedTwists() = runTest {
        val expected = readBinaryResource(TwistsAndTurnsTest::class, "twists-and-turns/unsorted-twists.pdf")
        assertContentEquals(
            expected, puzzle.copy(
                separateLightAndDarkTwists = true,
                numberTwists = false,
                sortTwists = false,
            ).asPdf(
                blackSquareLightnessAdjustment = 0.5,
                fontFamily = getNotoSerifFontFamily(),
            )
        )
    }

    @Test
    fun pdfGeneration_bottomRight() = runTest {
        val expected = readBinaryResource(TwistsAndTurnsTest::class, "twists-and-turns/unsorted-twists.pdf")
        assertContentEquals(
            expected, puzzle.copy(
                separateLightAndDarkTwists = true,
                numberTwists = false,
                sortTwists = false,
            ).asPdf(
                blackSquareLightnessAdjustment = 0.5,
                fontFamily = getNotoSerifFontFamily(),
                gridCorner = GridCorner.BOTTOM_RIGHT,
            )
        )
    }

    @Test
    fun pdfGeneration_bottomLeft() = runTest {
        val expected = readBinaryResource(TwistsAndTurnsTest::class, "twists-and-turns/unsorted-twists-bottomLeft.pdf")
        assertContentEquals(
            expected, puzzle.copy(
                separateLightAndDarkTwists = true,
                numberTwists = false,
                sortTwists = false,
            ).asPdf(
                blackSquareLightnessAdjustment = 0.5,
                fontFamily = getNotoSerifFontFamily(),
                gridCorner = GridCorner.BOTTOM_LEFT,
            )
        )
    }

    @Test
    fun pdfGeneration_topRight() = runTest {
        val expected = readBinaryResource(TwistsAndTurnsTest::class, "twists-and-turns/unsorted-twists-topRight.pdf")
        assertContentEquals(
            expected, puzzle.copy(
                separateLightAndDarkTwists = true,
                numberTwists = false,
                sortTwists = false,
            ).asPdf(
                blackSquareLightnessAdjustment = 0.5,
                fontFamily = getNotoSerifFontFamily(),
                gridCorner = GridCorner.TOP_RIGHT,
            )
        )
    }

    @Test
    fun pdfGeneration_topLeft() = runTest {
        val expected = readBinaryResource(TwistsAndTurnsTest::class, "twists-and-turns/unsorted-twists-topLeft.pdf")
        assertContentEquals(
            expected, puzzle.copy(
                separateLightAndDarkTwists = true,
                numberTwists = false,
                sortTwists = false,
            ).asPdf(
                blackSquareLightnessAdjustment = 0.5,
                fontFamily = getNotoSerifFontFamily(),
                gridCorner = GridCorner.TOP_LEFT,
            )
        )
    }

    @Test
    fun pdfGeneration_sortedTwists() = runTest {
        val expected = readBinaryResource(TwistsAndTurnsTest::class, "twists-and-turns/sorted-twists.pdf")
        assertContentEquals(
            expected, puzzle.copy(
                separateLightAndDarkTwists = true,
                numberTwists = false,
                sortTwists = true
            ).asPdf(
                blackSquareLightnessAdjustment = 0.5,
                fontFamily = getNotoSerifFontFamily(),
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
        )
    }
}