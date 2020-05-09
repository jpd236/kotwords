package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.AcrossLite.Companion.toAcrossLiteBinary
import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readUtf8Resource
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import kotlin.test.assertEquals

class BostonGlobeTest {
    @Test
    fun toCrossword() {
        val crossword = BostonGlobe(BostonGlobeTest::class.readUtf8Resource("bg/test-simple.html"))
                .asCrossword()
        assertEquals("EXAMPLE PUZZLE FOR KOTWORDS", crossword.title)
        assertArrayEquals(
                AcrossLite(BostonGlobeTest::class.readBinaryResource("puz/test-simple.puz"))
                        .asCrossword().toAcrossLiteBinary(),
                crossword.copy(title = "Example Puzzle for Kotwords").toAcrossLiteBinary())
    }
}