package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.readStringResource
import com.jeffpdavidson.kotwords.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SpellWeavingTest {
    @Test
    fun jpzGeneration() = runTest {
        val spellWeaving = SpellWeaving(
                title = "Test title",
                creator = "Test creator",
                copyright = "Test copyright",
                description = "Test description",
                answers = listOf("ABCD", "EFG", "BHICFJ"),
                clues = listOf("Clue 1", "Clue 2", "Clue 3"))
        val puzzle = spellWeaving.asPuzzle(
                crosswordSolverSettings = Puzzle.CrosswordSolverSettings(
                        cursorColor = "#00b100",
                        selectedCellsColor = "#80ff80",
                        completionMessage = "All done!"))

        val expected = readStringResource(SpellWeavingTest::class, "spell-weaving.jpz")
        assertEquals(expected, puzzle.asJpzFile().toXmlString())
    }
}