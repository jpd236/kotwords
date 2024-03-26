package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.formats.Ipuz
import com.jeffpdavidson.kotwords.readStringResource
import io.github.pdvrieze.xmlutil.testutil.assertXmlEquals
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class HeartsAndArrowsTest {
    @Test
    fun jpzGeneration() = runTest {
        val puzzle = PUZZLE.asPuzzle()
        val expected = readStringResource(HeartsAndArrowsTest::class, "hearts-and-arrows/hearts-and-arrows.jpz")
        assertXmlEquals(expected, puzzle.asJpz().toXmlString())
    }

    @Test
    fun ipuzGeneration() = runTest {
        val puzzle = PUZZLE.asPuzzle()
        val expected = readStringResource(HeartsAndArrowsTest::class, "hearts-and-arrows/hearts-and-arrows.ipuz")
        assertEquals(expected, Ipuz.asIpuzJson(puzzle).toJsonString())
    }

    @Test
    fun ipuzGeneration_unlabeledHearts() = runTest {
        val puzzle = PUZZLE.copy(labelHearts = false).asPuzzle()
        val expected = readStringResource(
            HeartsAndArrowsTest::class,
            "hearts-and-arrows/hearts-and-arrows-unlabeled.ipuz"
        )
        assertEquals(expected, Ipuz.asIpuzJson(puzzle).toJsonString())
    }

    companion object {
        private val PUZZLE = HeartsAndArrows(
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
    }
}