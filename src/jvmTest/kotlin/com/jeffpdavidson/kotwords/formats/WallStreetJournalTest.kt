package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.AcrossLite.Companion.toAcrossLiteBinary
import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readUtf8Resource
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class WallStreetJournalTest {
    @Test
    fun basicPuzzle() {
        assertArrayEquals(
                WallStreetJournalTest::class.readBinaryResource("puz/test.puz"),
                WallStreetJournal(WallStreetJournalTest::class.readUtf8Resource("wsj/test.json"),
                        includeDateInTitle = false)
                        .asCrossword().toAcrossLiteBinary())
    }
}