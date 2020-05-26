package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.readStringResource
import com.jeffpdavidson.kotwords.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

// TODO: Expand test coverage
class TwistsAndTurnsTest {
    @Test
    fun jpzGeneration() = runTest {
        val puzzle = TwistsAndTurns(
                "Test title",
                "Test creator",
                "Test copyright",
                "Test description",
                2,
                2,
                1,
                listOf("AB", "CD"),
                listOf("Turn 1", "Turn 2"),
                listOf("Twist 1", "Twist 2", "Twist 3", "Twist 4"),
                "#FFFFFF",
                "#999999",
                Puzzle.CrosswordSolverSettings("#00b100", "#80ff80", "All done!"))

        val expected = readStringResource(TwistsAndTurnsTest::class, "twists-and-turns.jpz")
        assertEquals(expected, puzzle.asPuzzle().asJpzFile().toXmlString())
    }
}