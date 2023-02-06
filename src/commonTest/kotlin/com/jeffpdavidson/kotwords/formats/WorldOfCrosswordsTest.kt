package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readStringResource
import kotlinx.coroutines.test.runTest
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
                ).asPuzzle().asAcrossLiteBinary()
            )
        )
    }
}