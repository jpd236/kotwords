package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.formats.CrosswordCompilerApplet
import com.jeffpdavidson.kotwords.readStringResource
import com.jeffpdavidson.kotwords.util.trimmedLines
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class HeartsAndArrowsTest {
    @Test
    fun jpzGeneration() = runTest {
        val heartsAndArrows = HeartsAndArrows(
            title = "Test title",
            creator = "Test creator",
            copyright = "Test copyright",
            description = "Test description",
            solutionGrid = listOf(
                "..AA......BB.".toList(),
                "..AAACC...BBB".toList(),
                "DDAAACCCEEBBB".toList(),
                "DDD..CCCEEE..".toList(),
                "DDD.....EEE..".toList(),
            ),
            arrows = listOf(
                listOf(RowsGarden.Entry("Row 1 Clue", "AABB")),
                listOf(RowsGarden.Entry("Row 2 Clue 1", "AAAC"), RowsGarden.Entry("Row 2 Clue 2", "CBBB")),
                listOf(RowsGarden.Entry("Row 3 Clue 1", "DDAAAC"), RowsGarden.Entry("Row 2 Clue 2", "CCEEBBB")),
                listOf(RowsGarden.Entry("Row 4 Clue 1", "DDD"), RowsGarden.Entry("Row 2 Clue 2", "CCCEEE")),
                listOf(RowsGarden.Entry("Row 5 Clue", "DDDEEE")),
            ),
            light = listOf(
                RowsGarden.Entry("Light 1 Clue", "BBBBBBBB"),
                RowsGarden.Entry("Light 2 Clue", "CCCCCCCC"),
                RowsGarden.Entry("Light 3 Clue", "DDDDDDDD"),
            ),
            medium = listOf(
                RowsGarden.Entry("Medium 1 Clue", "EEEEEEEE"),
            ),
            dark = listOf(
                RowsGarden.Entry("Dark 1 Clue", "AAAAAAAA"),
            ),
        )
        val puzzle = heartsAndArrows.asPuzzle()

        val expected = readStringResource(HeartsAndArrowsTest::class, "hearts-and-arrows/hearts-and-arrows.jpz")
        assertEquals(expected, puzzle.asJpz().toXmlString())
    }
}