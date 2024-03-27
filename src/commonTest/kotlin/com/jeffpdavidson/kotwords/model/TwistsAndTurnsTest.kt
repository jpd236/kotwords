package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.IgnoreNative
import com.jeffpdavidson.kotwords.formats.CrosswordCompilerApplet
import com.jeffpdavidson.kotwords.formats.ImageComparator.assertPdfEquals
import com.jeffpdavidson.kotwords.formats.getNotoSerifFontFamily
import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readStringResource
import io.github.pdvrieze.xmlutil.testutil.assertXmlEquals
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

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
    @IgnoreNative  // Depends on PDF support
    fun pdfGeneration_unsortedTwists() = runTest {
        val expected = readBinaryResource(TwistsAndTurnsTest::class, "twists-and-turns/unsorted-twists.pdf")
        assertPdfEquals(
            expected, puzzle.copy(
                separateLightAndDarkTwists = true,
                numberTwists = false,
                sortTwists = false,
            ).asPdf(
                blackSquareLightnessAdjustment = 0.5f,
                fontFamily = getNotoSerifFontFamily(),
            )
        )
    }

    @Test
    @IgnoreNative  // Depends on PDF support
    fun pdfGeneration_sortedTwists() = runTest {
        val expected = readBinaryResource(TwistsAndTurnsTest::class, "twists-and-turns/sorted-twists.pdf")
        assertPdfEquals(
            expected, puzzle.copy(
                separateLightAndDarkTwists = true,
                numberTwists = false,
                sortTwists = true
            ).asPdf(
                blackSquareLightnessAdjustment = 0.5f,
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