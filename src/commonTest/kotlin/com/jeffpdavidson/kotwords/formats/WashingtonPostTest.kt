package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readStringResource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class WashingtonPostTest {
    @Test
    fun basicPuzzle() = runTest {
        assertTrue(
            readBinaryResource(WallStreetJournalTest::class, "puz/test.puz").contentEquals(
                WashingtonPost(
                    readStringResource(WashingtonPostTest::class, "wapo/test.json"),
                ).asPuzzle().asAcrossLiteBinary()
            )
        )
    }
}