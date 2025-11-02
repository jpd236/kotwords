package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.formats.CrosswordCompilerApplet
import com.jeffpdavidson.kotwords.readStringResource
import io.github.pdvrieze.xmlutil.testutil.assertXmlEquals
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class JellyRollTest {
    @Test
    fun jpzGeneration() = runTest {
        val jellyRoll = JellyRoll(
            title = "Test title",
            creator = "Test creator",
            copyright = "Test copyright",
            description = "Test description",
            jellyRollAnswers = listOf("ABCDE", "FGHI"),
            jellyRollClues = listOf("Clue 1", "Clue 2"),
            lightSquaresAnswers = listOf("ADE", "HI"),
            lightSquaresClues = listOf("Light clue 1", "Light clue 2"),
            darkSquaresAnswers = listOf("BC", "FG"),
            darkSquaresClues = listOf("Dark clue 1", "Dark clue 2"),
            darkSquareBackgroundColor = "#C0C0C0",
            lightSquareBackgroundColor = "#FFFFFF",
            combineJellyRollClues = false,
        )
        val puzzle = jellyRoll.asPuzzle()

        val expected = readStringResource(JellyRollTest::class, "jelly-roll/jelly-roll.jpz")
        assertXmlEquals(
            expected, puzzle.asJpz(
                appletSettings = CrosswordCompilerApplet.AppletSettings(
                    cursorColor = "#00b100",
                    selectedCellsColor = "#80ff80",
                    completion = CrosswordCompilerApplet.AppletSettings.Completion(message = "All done!"),
                )
            ).toXmlString()
        )
    }

    @Test
    fun jpzGeneration_combinedJellyRollClues() = runTest {
        val jellyRoll = JellyRoll(
            title = "Test title",
            creator = "Test creator",
            copyright = "Test copyright",
            description = "Test description",
            jellyRollAnswers = listOf("ABCDE", "FGHI"),
            jellyRollClues = listOf("Clue 1", "Clue 2"),
            lightSquaresAnswers = listOf("ADE", "HI"),
            lightSquaresClues = listOf("Light clue 1", "Light clue 2"),
            darkSquaresAnswers = listOf("BC", "FG"),
            darkSquaresClues = listOf("Dark clue 1", "Dark clue 2"),
            darkSquareBackgroundColor = "#C0C0C0",
            lightSquareBackgroundColor = "#FFFFFF",
            combineJellyRollClues = true,
        )
        val puzzle = jellyRoll.asPuzzle()

        val expected = readStringResource(JellyRollTest::class, "jelly-roll/jelly-roll-combined.jpz")
        assertXmlEquals(
            expected, puzzle.asJpz(
                appletSettings = CrosswordCompilerApplet.AppletSettings(
                    cursorColor = "#00b100",
                    selectedCellsColor = "#80ff80",
                    completion = CrosswordCompilerApplet.AppletSettings.Completion(message = "All done!"),
                )
            ).toXmlString()
        )
    }

    @Test
    fun jpzGeneration_nonSquare() = runTest {
        val jellyRoll = JellyRoll(
            title = "Test title",
            creator = "Test creator",
            copyright = "Test copyright",
            description = "Test description",
            jellyRollAnswers = listOf("ABCDE", "FGH"),
            jellyRollClues = listOf("Clue 1", "Clue 2"),
            lightSquaresAnswers = listOf("ADE", "H"),
            lightSquaresClues = listOf("Light clue 1", "Light clue 2"),
            darkSquaresAnswers = listOf("BC", "FG"),
            darkSquaresClues = listOf("Dark clue 1", "Dark clue 2"),
            darkSquareBackgroundColor = "#C0C0C0",
            lightSquareBackgroundColor = "#FFFFFF",
            combineJellyRollClues = false,
        )
        val puzzle = jellyRoll.asPuzzle()

        val expected = readStringResource(JellyRollTest::class, "jelly-roll/jelly-roll-nonsquare.jpz")
        assertXmlEquals(
            expected, puzzle.asJpz(
                appletSettings = CrosswordCompilerApplet.AppletSettings(
                    cursorColor = "#00b100",
                    selectedCellsColor = "#80ff80",
                    completion = CrosswordCompilerApplet.AppletSettings.Completion(message = "All done!"),
                )
            ).toXmlString()
        )
    }

    @Test
    fun ipuzGeneration() = runTest {
        val jellyRoll = JellyRoll(
            title = "Test title",
            creator = "Test creator",
            copyright = "Test copyright",
            description = "Test description",
            jellyRollAnswers = listOf("ABCDE", "FGHI"),
            jellyRollClues = listOf("Clue 1", "Clue 2"),
            lightSquaresAnswers = listOf("ADE", "HI"),
            lightSquaresClues = listOf("Light clue 1", "Light clue 2"),
            darkSquaresAnswers = listOf("BC", "FG"),
            darkSquaresClues = listOf("Dark clue 1", "Dark clue 2"),
            darkSquareBackgroundColor = "#C0C0C0",
            lightSquareBackgroundColor = "#FFFFFF",
            combineJellyRollClues = false,
        )
        val puzzle = jellyRoll.asPuzzle()

        val expected = readStringResource(JellyRollTest::class, "jelly-roll/jelly-roll.ipuz")
        assertEquals(expected, puzzle.asIpuzFile().decodeToString())
    }
}