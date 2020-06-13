package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.readStringResource
import com.jeffpdavidson.kotwords.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class TwoToneTest {
    @Test
    fun jpzGeneration() = runTest {
        val twoTone = TwoTone(
                title = "Test title",
                creator = "Test creator",
                copyright = "Test copyright",
                description = "Test description",
                allSquaresAnswers = listOf("ABCDE", "FGHI"),
                allSquaresClues = listOf("Clue 1", "Clue 2"),
                oddSquaresAnswers = listOf("ACE", "GI"),
                oddSquaresClues = listOf("Odd clue 1", "Odd clue 2"),
                evenSquaresAnswers = listOf("BD", "FH"),
                evenSquaresClues = listOf("Even clue 1", "Even clue 2"))
        val puzzle = twoTone.asPuzzle(
                oddSquareBackgroundColor = "#C0C0C0",
                evenSquareBackgroundColor = "#FFFFFF",
                crosswordSolverSettings = Puzzle.CrosswordSolverSettings(
                        cursorColor = "#00b100",
                        selectedCellsColor = "#80ff80",
                        completionMessage = "All done!"))

        val expected = readStringResource(TwoToneTest::class, "two-tone.jpz")
        assertEquals(expected, puzzle.asJpzFile().toXmlString())
    }
}