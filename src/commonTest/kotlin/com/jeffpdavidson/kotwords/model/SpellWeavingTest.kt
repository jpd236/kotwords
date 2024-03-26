package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.formats.CrosswordCompilerApplet
import com.jeffpdavidson.kotwords.readStringResource
import io.github.pdvrieze.xmlutil.testutil.assertXmlEquals
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class SpellWeavingTest {
    @Test
    fun jpzGeneration() = runTest {
        val spellWeaving = SpellWeaving(
            title = "Test title",
            creator = "Test creator",
            copyright = "Test copyright",
            description = "Test description",
            answers = listOf("ABCD", "EFG", "BHICFJ"),
            clues = listOf("Clue 1", "Clue 2", "Clue 3")
        )
        val puzzle = spellWeaving.asPuzzle()

        val expected = readStringResource(SpellWeavingTest::class, "spell-weaving/spell-weaving.jpz")
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