package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.formats.CrosswordCompilerApplet
import com.jeffpdavidson.kotwords.readStringResource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class LabyrinthTest {
    @Test
    fun jpzGeneration() = runTest {
        val labyrinth = Labyrinth(
            title = "Test title",
            creator = "Test creator",
            copyright = "Test copyright",
            description = "Test description",
            grid = listOf(
                listOf('L', 'A', 'R', 'I'),
                listOf('H', 'B', 'Y', 'N'),
                listOf('O', 'O', 'H', 'T')
            ),
            gridKey = listOf(
                listOf(1, 2, 5, 6),
                listOf(12, 3, 4, 7),
                listOf(11, 10, 9, 8)
            ),
            rowClues = listOf(listOf("R1"), listOf("R2C1", "R2C2"), listOf("R3")),
            windingClues = listOf("This puzzle", "Your reaction to this puzzle"),
            alphabetizeWindingClues = false,
        )
        val puzzle = labyrinth.asPuzzle()

        val expected = readStringResource(LabyrinthTest::class, "labyrinth.jpz")
        assertEquals(
            expected, puzzle.asJpz(
                appletSettings = CrosswordCompilerApplet.AppletSettings(
                    cursorColor = "#00b100",
                    selectedCellsColor = "#80ff80",
                    completion = CrosswordCompilerApplet.AppletSettings.Completion(message = "All done!"),
                )
            ).toXmlString()
        )
    }
}