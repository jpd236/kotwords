package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.formats.CrosswordCompilerApplet
import com.jeffpdavidson.kotwords.readStringResource
import io.github.pdvrieze.xmlutil.testutil.assertXmlEquals
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

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
            evenSquaresClues = listOf("Even clue 1", "Even clue 2"),
            oddSquareBackgroundColor = "#C0C0C0",
            evenSquareBackgroundColor = "#FFFFFF",
        )
        val puzzle = twoTone.asPuzzle()

        val expected = readStringResource(TwoToneTest::class, "two-tone/two-tone.jpz")
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
    fun jpzGeneration_nonSquare() = runTest {
        val twoTone = TwoTone(
            title = "Test title",
            creator = "Test creator",
            copyright = "Test copyright",
            description = "Test description",
            allSquaresAnswers = listOf("ABCDE", "FGH"),
            allSquaresClues = listOf("Clue 1", "Clue 2"),
            oddSquaresAnswers = listOf("ACE", "G"),
            oddSquaresClues = listOf("Odd clue 1", "Odd clue 2"),
            evenSquaresAnswers = listOf("BD", "FH"),
            evenSquaresClues = listOf("Even clue 1", "Even clue 2"),
            oddSquareBackgroundColor = "#C0C0C0",
            evenSquareBackgroundColor = "#FFFFFF",
        )
        val puzzle = twoTone.asPuzzle()

        val expected = readStringResource(TwoToneTest::class, "two-tone/two-tone-nonsquare.jpz")
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
}