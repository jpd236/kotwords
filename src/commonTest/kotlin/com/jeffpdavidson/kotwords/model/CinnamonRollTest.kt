package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.readStringResource
import com.jeffpdavidson.kotwords.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CinnamonRollTest {
    @Test
    fun jpzGeneration() = runTest {
        val cinnamonRoll = CinnamonRoll(
                title = "Test title",
                creator = "Test creator",
                copyright = "Test copyright",
                description = "Test description",
                cinnamonRollAnswers = listOf("ABCDE", "FGHI"),
                cinnamonRollClues = listOf("Clue 1", "Clue 2"),
                lightSquaresAnswers = listOf("ADE", "HI"),
                lightSquaresClues = listOf("Light clue 1", "Light clue 2"),
                darkSquaresAnswers = listOf("BC", "FG"),
                darkSquaresClues = listOf("Dark clue 1", "Dark clue 2")
        )
        val puzzle = cinnamonRoll.asPuzzle(
                darkSquareBackgroundColor = "#C0C0C0",
                lightSquareBackgroundColor = "#FFFFFF",
                crosswordSolverSettings = Puzzle.CrosswordSolverSettings(
                        cursorColor = "#00b100",
                        selectedCellsColor = "#80ff80",
                        completionMessage = "All done!"
                )
        )

        val expected = readStringResource(CinnamonRollTest::class, "cinnamon-roll/cinnamon-roll.jpz")
        assertEquals(expected, puzzle.asJpzFile().toXmlString())
    }

    @Test
    fun jpzGeneration_nonSquare() = runTest {
        val cinnamonRoll = CinnamonRoll(
                title = "Test title",
                creator = "Test creator",
                copyright = "Test copyright",
                description = "Test description",
                cinnamonRollAnswers = listOf("ABCDE", "FGH"),
                cinnamonRollClues = listOf("Clue 1", "Clue 2"),
                lightSquaresAnswers = listOf("ADE", "H"),
                lightSquaresClues = listOf("Light clue 1", "Light clue 2"),
                darkSquaresAnswers = listOf("BC", "FG"),
                darkSquaresClues = listOf("Dark clue 1", "Dark clue 2")
        )
        val puzzle = cinnamonRoll.asPuzzle(
                darkSquareBackgroundColor = "#C0C0C0",
                lightSquareBackgroundColor = "#FFFFFF",
                crosswordSolverSettings = Puzzle.CrosswordSolverSettings(
                        cursorColor = "#00b100",
                        selectedCellsColor = "#80ff80",
                        completionMessage = "All done!"
                )
        )

        val expected = readStringResource(CinnamonRollTest::class, "cinnamon-roll/cinnamon-roll-nonsquare.jpz")
        assertEquals(expected, puzzle.asJpzFile().toXmlString())
    }
}