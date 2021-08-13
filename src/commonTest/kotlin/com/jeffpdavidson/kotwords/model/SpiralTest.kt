package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.formats.CrosswordCompilerApplet
import com.jeffpdavidson.kotwords.formats.Jpz.Companion.asJpzFile
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
            outwardClues = listOf("Clue 1", "Clue 2", "Clue 3")
        )
        val puzzle = spiral.asPuzzle()

        val expected = readStringResource(SpiralTest::class, "spiral/spiral.jpz")
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

    @Test
    fun jpzGeneration_nonSquare() = runTest {
        val spiral = Spiral(
            title = "Test title",
            creator = "Test creator",
            copyright = "Test copyright",
            description = "Test description",
            inwardAnswers = listOf("ABCDE", "FGH"),
            inwardClues = listOf("Clue 1", "Clue 2"),
            outwardAnswers = listOf("HG", "FED", "CBA"),
            outwardClues = listOf("Clue 1", "Clue 2", "Clue 3")
        )
        val puzzle = spiral.asPuzzle()

        val expected = readStringResource(SpiralTest::class, "spiral/spiral-nonsquare.jpz")
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