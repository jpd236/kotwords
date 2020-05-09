package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.AcrossLite.Companion.toAcrossLiteBinary
import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readStringResource
import com.jeffpdavidson.kotwords.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import kotlin.test.assertEquals

class BostonGlobeTest {
    @Test
    fun toCrossword() = runTest {
        val crossword = BostonGlobe(readStringResource(BostonGlobeTest::class, "bg/test-simple.html"))
                .asCrossword()
        assertEquals("EXAMPLE PUZZLE FOR KOTWORDS", crossword.title)
        assertArrayEquals(
                AcrossLite(readBinaryResource(BostonGlobeTest::class, "puz/test-simple.puz"))
                        .asCrossword().toAcrossLiteBinary(),
                crossword.copy(title = "Example Puzzle for Kotwords").toAcrossLiteBinary())
    }
}