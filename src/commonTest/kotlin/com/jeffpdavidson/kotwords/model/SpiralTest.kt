package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.formats.CrosswordCompilerApplet
import com.jeffpdavidson.kotwords.readStringResource
import io.github.pdvrieze.xmlutil.testutil.assertXmlEquals
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

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
        assertXmlEquals(
            expected, puzzle.asJpz(
                appletSettings = CrosswordCompilerApplet.AppletSettings(
                    cursorColor = "#00b100",
                    selectedCellsColor = "#80ff80",
                    completion = CrosswordCompilerApplet.AppletSettings.Completion(message = "All done!"),
                )
            ).toXmlString()
        )
    }

    @Test
    fun jpzGeneration_nonPerfectSquare() = runTest {
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
        assertXmlEquals(
            expected, puzzle.asJpz(
                appletSettings = CrosswordCompilerApplet.AppletSettings(
                    cursorColor = "#00b100",
                    selectedCellsColor = "#80ff80",
                    completion = CrosswordCompilerApplet.AppletSettings.Completion(message = "All done!"),
                )
            ).toXmlString()
        )
    }

    @Test
    fun jpzGeneration_rectangle() = runTest {
        val spiral = Spiral(
            title = "Test title",
            creator = "Test creator",
            copyright = "Test copyright",
            description = "Test description",
            inwardAnswers = listOf("ABCDE", "FGH"),
            inwardClues = listOf("Clue 1", "Clue 2"),
            outwardAnswers = listOf("HG", "FED", "CBA"),
            outwardClues = listOf("Clue 1", "Clue 2", "Clue 3"),
            dimensions = 4 to 3,
        )
        val puzzle = spiral.asPuzzle()

        val expected = readStringResource(SpiralTest::class, "spiral/spiral-rectangular.jpz")
        assertXmlEquals(
            expected, puzzle.asJpz(
                appletSettings = CrosswordCompilerApplet.AppletSettings(
                    cursorColor = "#00b100",
                    selectedCellsColor = "#80ff80",
                    completion = CrosswordCompilerApplet.AppletSettings.Completion(message = "All done!"),
                )
            ).toXmlString()
        )
    }

    @Test
    fun jpzGeneration_chunked() = runTest {
        val spiral = Spiral(
            title = "Test title",
            creator = "Test creator",
            copyright = "Test copyright",
            description = "Test description",
            inwardAnswers = listOf("ABCDEFGHI", "JKLMNOP", "QRSTUVWXYZ"),
            inwardClues = listOf("Clue 1", "Clue 2", "Clue 3"),
            outwardAnswers = listOf("VWXYZTU", "QRSNOPJKLM", "FGHIDEABC"),
            outwardClues = listOf("Clue 1", "Clue 2", "Clue 3"),
            inwardCellsInput = listOf("ABC", "DE", "FGHI", "JKLM", "NOP", "QRS", "TU", "VWXYZ"),
        )
        val puzzle = spiral.asPuzzle()

        val expected = readStringResource(SpiralTest::class, "spiral/spiral-chunked.jpz")
        assertXmlEquals(expected, puzzle.asJpz().toXmlString())
    }
}