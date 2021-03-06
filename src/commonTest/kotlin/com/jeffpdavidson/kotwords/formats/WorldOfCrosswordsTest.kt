package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.AcrossLite.Companion.toAcrossLiteBinary
import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readStringResource
import com.jeffpdavidson.kotwords.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class WorldOfCrosswordsTest {
    @Test
    fun basicPuzzle() = runTest {
        assertTrue(
            readBinaryResource(WorldOfCrosswordsTest::class, "puz/test-simple.puz").contentEquals(
                WorldOfCrosswords(
                    readStringResource(
                        WorldOfCrosswordsTest::class,
                        "worldofcrosswords/test-simple.json"
                    ),
                    year = 2018,
                    author = "Jeff Davidson",
                    copyright = "Jeff Davidson"
                ).asCrossword().toAcrossLiteBinary()
            )
        )
    }
}