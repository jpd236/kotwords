package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.readStringResource
import com.jeffpdavidson.kotwords.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MarchingBandsTest {
    @Test
    fun jpzGeneration() = runTest {
        val puzzle = MARCHING_BANDS.asPuzzle(
                includeRowNumbers = true,
                lightBandColor = "#FFFFFF",
                darkBandColor = "#C0C0C0",
                crosswordSolverSettings = Puzzle.CrosswordSolverSettings(
                        cursorColor = "#00b100",
                        selectedCellsColor = "#80ff80",
                        completionMessage = "All done!"
                )
        )

        val expected = readStringResource(MarchingBandsTest::class, "marching-bands/marching-bands.jpz")
        assertEquals(expected, puzzle.asJpzFile().toXmlString())
    }

    @Test
    fun jpzGeneration_withoutRowNumbers() = runTest {
        val puzzle = MARCHING_BANDS.asPuzzle(
                includeRowNumbers = false,
                lightBandColor = "#FFFFFF",
                darkBandColor = "#C0C0C0",
                crosswordSolverSettings = Puzzle.CrosswordSolverSettings(
                        cursorColor = "#00b100",
                        selectedCellsColor = "#80ff80",
                        completionMessage = "All done!"
                )
        )

        val expected = readStringResource(MarchingBandsTest::class, "marching-bands/marching-bands-without-rows.jpz")
        assertEquals(expected, puzzle.asJpzFile().toXmlString())
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
                )
        )
    }
}