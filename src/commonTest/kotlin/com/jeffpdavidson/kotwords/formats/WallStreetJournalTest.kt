package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.AcrossLite.Companion.toAcrossLiteBinary
import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readStringResource
import com.jeffpdavidson.kotwords.runTest
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
                )
                    .asCrossword().toAcrossLiteBinary()
            )
        )
    }
}