package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.AcrossLite.Companion.toAcrossLiteBinary
import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readStringResource
import com.jeffpdavidson.kotwords.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class WallStreetJournalTest {
    @Test
    fun basicPuzzle() = runTest {
        assertArrayEquals(
                readBinaryResource(WallStreetJournalTest::class, "puz/test.puz"),
                WallStreetJournal(
                        readStringResource(WallStreetJournalTest::class, "wsj/test.json"),
                        includeDateInTitle = false
                )
                        .asCrossword().toAcrossLiteBinary()
        )
    }
}