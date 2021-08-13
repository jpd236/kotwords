package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.formats.CrosswordCompilerApplet
import com.jeffpdavidson.kotwords.formats.Jpz.Companion.asJpzFile
import com.jeffpdavidson.kotwords.readStringResource
import com.jeffpdavidson.kotwords.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MarchingBandsTest {
    @Test
    fun jpzGeneration() = runTest {
        val puzzle = MARCHING_BANDS.asPuzzle()
        val expected = readStringResource(MarchingBandsTest::class, "marching-bands/marching-bands.jpz")
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
    fun jpzGeneration_withoutRowNumbers() = runTest {
        val puzzle = MARCHING_BANDS.copy(includeRowNumbers = false).asPuzzle()
        val expected = readStringResource(MarchingBandsTest::class, "marching-bands/marching-bands-without-rows.jpz")
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

    companion object {
        private val MARCHING_BANDS = MarchingBands(
            title = "Test title",
            creator = "Test creator",
            copyright = "Test copyright",
            description = "Test description",
            grid = listOf(
                listOf('A', 'B', 'C', 'D', 'E'),
                listOf('F', 'G', 'H', 'I', 'J'),
                listOf('K', 'L', null, 'M', 'N'),
                listOf('O', 'P', 'Q', 'R', 'S'),
                listOf('T', 'U', 'V', 'W', 'X')
            ),
            bandClues = listOf(
                listOf("Band A1", "Band A2", "Band A3"),
                listOf("Band B1", "Band B2")
            ),
            rowClues = listOf(
                listOf("Row 1A", "Row 1B"),
                listOf("Row 2A", "Row 2B"),
                listOf("Row 3A", "Row 3B"),
                listOf("Row 4A", "Row 4B"),
                listOf("Row 5A", "Row 5B")
            ),
            includeRowNumbers = true,
            lightBandColor = "#FFFFFF",
            darkBandColor = "#C0C0C0",
        )
    }
}