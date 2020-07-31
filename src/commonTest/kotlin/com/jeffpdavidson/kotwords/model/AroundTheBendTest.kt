package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.readStringResource
import com.jeffpdavidson.kotwords.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AroundTheBendTest {
    @Test
    fun jpzGeneration() = runTest {
        val aroundTheBend = AroundTheBend(
            title = "Test title",
            creator = "Test creator",
            copyright = "Test copyright",
            description = "Test description",
            rows = listOf(
                "ABCD",
                "EF",
                "GHI",
                "JKL"),
            clues = listOf(
                "Clue 1 - ABCDFE",
                "Clue 2 - EFIHG",
                "Clue 3 - GHILKJ",
                "Clue 4 - JKLDCBA")
        )
        val puzzle = aroundTheBend.asPuzzle(
            crosswordSolverSettings = Puzzle.CrosswordSolverSettings(
                cursorColor = "#00b100",
                selectedCellsColor = "#80ff80",
                completionMessage = "All done!"
            )
        )

        val expected = readStringResource(AroundTheBendTest::class, "around-the-bend.jpz")
        assertEquals(expected, puzzle.asJpzFile().toXmlString())
    }
}