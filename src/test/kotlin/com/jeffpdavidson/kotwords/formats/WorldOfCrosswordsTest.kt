package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readUtf8Resource
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class WorldOfCrosswordsTest {
    @Test
    fun basicPuzzle() {
        assertArrayEquals(
                WorldOfCrosswordsTest::class.readBinaryResource("puz/test-simple.puz"),
                WorldOfCrosswords(
                        WorldOfCrosswordsTest::class.readUtf8Resource(
                                "worldofcrosswords/test-simple.json"),
                        date = LocalDate.of(2018, 4, 1),
                        author = "Jeff Davidson",
                        copyright = "Jeff Davidson"
                ).asCrossword().toAcrossLiteBinary())
    }
}