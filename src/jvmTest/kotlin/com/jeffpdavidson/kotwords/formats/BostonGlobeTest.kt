package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.AcrossLite.Companion.toAcrossLiteBinary
import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readUtf8Resource
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BostonGlobeTest {
    @Test
    fun toCrossword() {
        val crossword = BostonGlobe(BostonGlobeTest::class.readUtf8Resource("bg/test-simple.html"))
                .asCrossword()
        assertEquals("EXAMPLE PUZZLE FOR KOTWORDS", crossword.title)
        Assertions.assertArrayEquals(
                AcrossLite(BostonGlobeTest::class.readBinaryResource("puz/test-simple.puz"))
                        .asCrossword().toAcrossLiteBinary(),
                crossword.copy(title = "Example Puzzle for Kotwords").toAcrossLiteBinary())
    }
}