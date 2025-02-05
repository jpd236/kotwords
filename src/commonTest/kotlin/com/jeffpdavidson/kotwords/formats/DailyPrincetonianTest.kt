package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readStringResource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DailyPrincetonianTest {
    @Test
    fun crossword() = runTest {
        // Strip out unsupported features - rebuses, description, copyright, and HTML clues.
        val testJpz = JpzFile(readBinaryResource(DailyPrincetonianTest::class, "jpz/test.jpz")).asPuzzle()
        val expectedGrid = testJpz.grid.mapIndexed { y, row ->
            row.mapIndexed { x, cell ->
                if (x == 2 && y == 2) cell.copy(solution = "K") else cell
            }
        }
        assertEquals(
            testJpz.copy(
                description = "",
                copyright = "",
                grid = expectedGrid,
                clues = testJpz.clues.map { clueList ->
                    clueList.copy(
                        title = if (clueList.title == "<b>Across</b>") "Across" else "Down"
                    )
                },
                hasHtmlClues = false,
            ),
            DailyPrincetonian(
                readStringResource(DailyPrincetonianTest::class, "princetonian/test-crossword.json"),
                readStringResource(DailyPrincetonianTest::class, "princetonian/test-authors.json"),
                readStringResource(DailyPrincetonianTest::class, "princetonian/test-clues.json")
            ).asPuzzle()
        )
    }
}