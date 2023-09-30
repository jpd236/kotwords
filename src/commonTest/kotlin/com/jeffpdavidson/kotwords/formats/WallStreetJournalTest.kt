package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readStringResource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class WallStreetJournalTest {
    @Test
    fun basicPuzzle() = runTest {
        assertTrue(
            readBinaryResource(WallStreetJournalTest::class, "puz/test.puz").contentEquals(
                WallStreetJournal(
                    readStringResource(WallStreetJournalTest::class, "wsj/test.json"),
                    includeDateInTitle = false
                ).asPuzzle().asAcrossLiteBinary()
            )
        )
    }

    @Test
    fun noSolution() = runTest {
        assertTrue(
            readBinaryResource(WallStreetJournalTest::class, "puz/test-no-solution.puz").contentEquals(
                WallStreetJournal(
                    readStringResource(WallStreetJournalTest::class, "wsj/test-no-solution.json"),
                    includeDateInTitle = false
                ).asPuzzle().asAcrossLiteBinary()
            )
        )
    }
}