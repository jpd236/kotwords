package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.readStringResource
import com.jeffpdavidson.kotwords.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SpiralTest {
    @Test
    fun jpzGeneration() = runTest {
        val spiral = Spiral(
                title = "Test title",
                creator = "Test creator",
                copyright = "Test copyright",
                description = "Test description",
                inwardAnswers = listOf("ABCDE", "FGHI"),
                inwardClues = listOf("Clue 1", "Clue 2"),
                outwardAnswers = listOf("IHG", "FED", "CBA"),
                outwardClues = listOf("Clue 1", "Clue 2", "Clue 3"))
        val puzzle = spiral.asPuzzle(
                crosswordSolverSettings = Puzzle.CrosswordSolverSettings(
                        cursorColor = "#00b100",
                        selectedCellsColor = "#80ff80",
                        completionMessage = "All done!"))

        val expected = readStringResource(SpiralTest::class, "spiral.jpz")
        assertEquals(expected, puzzle.asJpzFile().toXmlString())
    }
}