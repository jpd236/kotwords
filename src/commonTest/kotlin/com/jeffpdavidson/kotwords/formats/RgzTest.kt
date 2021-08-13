package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.model.RowsGarden
import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class RgzTest {
    @Test
    fun parseRg() = runTest {
        val parsed = Rgz.fromRgzFile(readBinaryResource(RowsGardenTest::class, "rows-garden/test.rg")).asRowsGarden()
        assertEquals(TEST_DATA.copy(notes = null), parsed)
    }

    @Test
    fun parseRgz() = runTest {
        val parsed = Rgz.fromRgzFile(readBinaryResource(RowsGardenTest::class, "rows-garden/test.rgz")).asRowsGarden()
        assertEquals(TEST_DATA, parsed)
    }

    companion object {
        private val TEST_DATA = RowsGarden(
            title = "Test Title",
            author = "Test Author",
            copyright = "(c) 2020 Kotwords",
            notes = "Test Notes",
            rows = listOf(
                listOf(RowsGarden.Entry("Row 1", "AAAAAAAAA")),
                listOf(
                    RowsGarden.Entry("Row 2 Clue 1 (space)", "AAAAA AAAAA"),
                    RowsGarden.Entry("Row 2 Clue 2", "AAAAAAAAAAA")
                ),
                listOf(
                    RowsGarden.Entry("Row 3 Clue 1", "AAAAAAAAAA"),
                    RowsGarden.Entry("Row 3 Clue 2 (hyphen)", "AAA-AAAAAAAA")
                ),
                listOf(
                    RowsGarden.Entry(
                        "Row 4 Clue 1 (spaces and hyphen)", "AAA AA AA-AAA"
                    ),
                    RowsGarden.Entry("Row 4 Clue 2", "AAAAAAAAAAA")
                ),
                listOf(
                    RowsGarden.Entry("Row 5 *Clue 1* (italics)", "AAAAAAAAAA"),
                    RowsGarden.Entry("\"Row 5\" Clue 2 (quotes)", "AAAAAAAAAAA")
                ),
                listOf(
                    RowsGarden.Entry("Row 6 Clue 1", "AAAAAAAAAA"),
                    RowsGarden.Entry(
                        "Row 6 Clue 2 " +
                                ":{}[],&*#?|-<>=!%@ (special characters)", "AAAAAAAAAAA"
                    )
                ),
                listOf(
                    RowsGarden.Entry("Row 7 Clue 1", "AAAAAAAAAA"),
                    RowsGarden.Entry("Row 7 Clue 2", "AAAAAAAAAAA")
                ),
                listOf(
                    RowsGarden.Entry("Row 8 Clue 1", "AAAAAAAAAA"),
                    RowsGarden.Entry("Row 8 Clue 2", "AAAAAAAAAAA")
                ),
                listOf(
                    RowsGarden.Entry("Row 9 Clue 1", "AAAAAAAAAA"),
                    RowsGarden.Entry("Row 9 Clue 2", "AAAAAAAAAAA")
                ),
                listOf(
                    RowsGarden.Entry("Row 10 Clue 1", "AAAAAAAAAA"),
                    RowsGarden.Entry("Row 10 Clue 2", "AAAAAAAAAAA")
                ),
                listOf(
                    RowsGarden.Entry("Row 11 Clue 1", "AAAAAAAAAA"),
                    RowsGarden.Entry("Row 11 Clue 2", "AAAAAAAAAAA")
                ),
                listOf(RowsGarden.Entry("Row 12", "AAAAAAAAA"))
            ),
            light = listOf(
                RowsGarden.Entry("Light 1", "AAAAAA"),
                RowsGarden.Entry("Light 2 (space)", "AAA AAA"),
                RowsGarden.Entry("Light 3 (hyphen)", "AAA-AAA"),
                RowsGarden.Entry("Light 4", "AAAAAA"),
                RowsGarden.Entry("Light 5", "AAAAAA"),
                RowsGarden.Entry("Light 6", "AAAAAA"),
                RowsGarden.Entry("Light 7", "AAAAAA"),
                RowsGarden.Entry("Light 8", "AAAAAA"),
                RowsGarden.Entry("Light 9", "AAAAAA"),
                RowsGarden.Entry("Light 10", "AAAAAA"),
                RowsGarden.Entry("Light 11", "AAAAAA"),
                RowsGarden.Entry("Light 12", "AAAAAA"),
                RowsGarden.Entry("Light 13", "AAAAAA"),
                RowsGarden.Entry("Light 14", "AAAAAA")
            ),
            medium = listOf(
                RowsGarden.Entry("Medium 1", "AAAAAA"),
                RowsGarden.Entry("Medium 2", "AAAAAA"),
                RowsGarden.Entry("Medium 3", "AAAAAA"),
                RowsGarden.Entry("Medium 4", "AAAAAA"),
                RowsGarden.Entry("Medium 5", "AAAAAA"),
                RowsGarden.Entry("Medium 6", "AAAAAA"),
                RowsGarden.Entry("Medium 7", "AAAAAA"),
                RowsGarden.Entry("Medium 8", "AAAAAA"),
                RowsGarden.Entry("Medium 9", "AAAAAA"),
                RowsGarden.Entry("Medium 10", "AAAAAA"),
                RowsGarden.Entry("Medium 11", "AAAAAA"),
                RowsGarden.Entry("Medium 12", "AAAAAA"),
                RowsGarden.Entry("Medium 13", "AAAAAA"),
                RowsGarden.Entry("Medium 14", "AAAAAA")
            ),
            dark = listOf(
                RowsGarden.Entry("Dark 1", "AAAAAA"),
                RowsGarden.Entry("Dark 2", "AAAAAA"),
                RowsGarden.Entry("Dark 3", "AAAAAA"),
                RowsGarden.Entry("Dark 4", "AAAAAA"),
                RowsGarden.Entry("Dark 5", "AAAAAA"),
                RowsGarden.Entry("Dark 6", "AAAAAA"),
                RowsGarden.Entry("Dark 7", "AAAAAA"),
                RowsGarden.Entry("Dark 8", "AAAAAA"),
                RowsGarden.Entry("Dark 9", "AAAAAA"),
                RowsGarden.Entry("Dark 10", "AAAAAA")
            )
        )
    }
}