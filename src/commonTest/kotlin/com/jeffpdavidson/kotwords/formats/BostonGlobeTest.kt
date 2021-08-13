package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.AcrossLite.Companion.asAcrossLiteBinary
import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readStringResource
import com.jeffpdavidson.kotwords.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BostonGlobeTest {
    @Test
    fun toCrossword() = runTest {
        val puzzle = BostonGlobe(readStringResource(BostonGlobeTest::class, "bg/test-simple.html")).asPuzzle()
        assertEquals("EXAMPLE PUZZLE FOR KOTWORDS", puzzle.title)
        assertTrue(
            AcrossLite(readBinaryResource(BostonGlobeTest::class, "puz/test-simple.puz"))
                .asPuzzle().asAcrossLiteBinary().contentEquals(
                    puzzle.copy(title = "Example Puzzle for Kotwords").asAcrossLiteBinary()
                )
        )
    }
}