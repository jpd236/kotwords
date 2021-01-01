package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.readStringResource
import com.jeffpdavidson.kotwords.runTest
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
            darkSquaresClues = listOf("Dark clue 1", "Dark clue 2")
        )
        val puzzle = jellyRoll.asPuzzle(
            darkSquareBackgroundColor = "#C0C0C0",
            lightSquareBackgroundColor = "#FFFFFF",
            combineJellyRollClues = false,
            crosswordSolverSettings = Puzzle.CrosswordSolverSettings(
                cursorColor = "#00b100",
                selectedCellsColor = "#80ff80",
                completionMessage = "All done!"
            )
        )

        val expected = readStringResource(JellyRollTest::class, "jelly-roll/jelly-roll.jpz")
        assertEquals(expected, puzzle.asJpzFile().toXmlString())
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
            darkSquaresClues = listOf("Dark clue 1", "Dark clue 2")
        )
        val puzzle = jellyRoll.asPuzzle(
            darkSquareBackgroundColor = "#C0C0C0",
            lightSquareBackgroundColor = "#FFFFFF",
            combineJellyRollClues = true,
            crosswordSolverSettings = Puzzle.CrosswordSolverSettings(
                cursorColor = "#00b100",
                selectedCellsColor = "#80ff80",
                completionMessage = "All done!"
            )
        )

        val expected = readStringResource(JellyRollTest::class, "jelly-roll/jelly-roll-combined.jpz")
        assertEquals(expected, puzzle.asJpzFile().toXmlString())
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
            darkSquaresClues = listOf("Dark clue 1", "Dark clue 2")
        )
        val puzzle = jellyRoll.asPuzzle(
            darkSquareBackgroundColor = "#C0C0C0",
            lightSquareBackgroundColor = "#FFFFFF",
            combineJellyRollClues = false,
            crosswordSolverSettings = Puzzle.CrosswordSolverSettings(
                cursorColor = "#00b100",
                selectedCellsColor = "#80ff80",
                completionMessage = "All done!"
            )
        )

        val expected = readStringResource(JellyRollTest::class, "jelly-roll/jelly-roll-nonsquare.jpz")
        assertEquals(expected, puzzle.asJpzFile().toXmlString())
    }
}