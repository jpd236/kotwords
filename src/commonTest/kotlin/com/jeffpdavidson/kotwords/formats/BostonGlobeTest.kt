package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.AcrossLite.Companion.toAcrossLiteBinary
import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readStringResource
import com.jeffpdavidson.kotwords.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BostonGlobeTest {
    @Test
    fun toCrossword() = runTest {
        val crossword = BostonGlobe(readStringResource(BostonGlobeTest::class, "bg/test-simple.html"))
            .asCrossword()
        assertEquals("EXAMPLE PUZZLE FOR KOTWORDS", crossword.title)
        assertTrue(
            AcrossLite(readBinaryResource(BostonGlobeTest::class, "puz/test-simple.puz"))
                .asCrossword().toAcrossLiteBinary().contentEquals(
                    crossword.copy(title = "Example Puzzle for Kotwords").toAcrossLiteBinary()
                )
        )
    }
}