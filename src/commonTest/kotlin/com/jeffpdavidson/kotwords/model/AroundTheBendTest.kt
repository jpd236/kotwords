package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.formats.CrosswordCompilerApplet
import com.jeffpdavidson.kotwords.readStringResource
import io.github.pdvrieze.xmlutil.testutil.assertXmlEquals
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

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
                "JKL"
            ),
            clues = listOf(
                "Clue 1 - ABCDFE",
                "Clue 2 - EFIHG",
                "Clue 3 - GHILKJ",
                "Clue 4 - JKLDCBA"
            )
        )
        val puzzle = aroundTheBend.asPuzzle()

        val expected = readStringResource(AroundTheBendTest::class, "around-the-bend/around-the-bend.jpz")
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