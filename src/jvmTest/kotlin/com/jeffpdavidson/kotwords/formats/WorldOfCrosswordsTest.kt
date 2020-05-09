package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.AcrossLite.Companion.toAcrossLiteBinary
import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readStringResource
import com.jeffpdavidson.kotwords.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import java.time.LocalDate

class WorldOfCrosswordsTest {
    @Test
    fun basicPuzzle() = runTest {
        assertArrayEquals(
                readBinaryResource(WorldOfCrosswordsTest::class, "puz/test-simple.puz"),
                WorldOfCrosswords(
                        readStringResource(WorldOfCrosswordsTest::class,
                                "worldofcrosswords/test-simple.json"),
                        date = LocalDate.of(2018, 4, 1),
                        author = "Jeff Davidson",
                        copyright = "Jeff Davidson"
                ).asCrossword().toAcrossLiteBinary())
    }
}