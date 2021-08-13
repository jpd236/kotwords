package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.formats.CrosswordCompilerApplet
import com.jeffpdavidson.kotwords.formats.Jpz.Companion.asJpzFile
import com.jeffpdavidson.kotwords.readStringResource
import com.jeffpdavidson.kotwords.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SnakeCharmerTest {
    @Test
    fun jpzGeneration() = runTest {
        val snakeCharmer = SnakeCharmer(
            title = "Test title",
            creator = "Test creator",
            copyright = "Test copyright",
            description = "Test description",
            answers = listOf("ABCD", "EFG", "HABCD", "EFGH"),
            clues = listOf("Clue 1", "Clue 2", "Clue 3", "Clue 4"),
            gridCoordinates = listOf(0 to 0, 1 to 0, 2 to 0, 2 to 1, 2 to 2, 1 to 2, 0 to 2, 0 to 1)
        )
        val puzzle = snakeCharmer.asPuzzle()

        val expected = readStringResource(SnakeCharmerTest::class, "snake-charmer.jpz")
        assertEquals(
            expected, puzzle.asJpzFile(
                appletSettings = CrosswordCompilerApplet.AppletSettings(
                    cursorColor = "#00b100",
                    selectedCellsColor = "#80ff80",
                    completion = CrosswordCompilerApplet.AppletSettings.Completion(message = "All done!"),
                )
            ).toXmlString()
        )
    }
}