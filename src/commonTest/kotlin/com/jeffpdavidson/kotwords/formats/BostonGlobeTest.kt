package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readStringResource
import kotlinx.coroutines.test.runTest
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

    @Test
    fun parseSubHeader_capitalTitle() {
        assertEquals(
            BostonGlobe.Companion.SubHeader("TEST TITLE", "Test Author", "\u00a9 Test Copyright"),
            BostonGlobe.parseSubHeader("TEST TITLE Test Author, Test Copyright")
        )
        assertEquals(
            BostonGlobe.Companion.SubHeader("TEST BY TITLE", "Test Author", "\u00a9 Test Copyright"),
            BostonGlobe.parseSubHeader("TEST BY TITLE Test Author, Test Copyright")
        )
        assertEquals(
            BostonGlobe.Companion.SubHeader("TEST, TITLE", "Test Author", "\u00a9 Test Copyright"),
            BostonGlobe.parseSubHeader("TEST, TITLE Test Author, Test Copyright")
        )
    }

    @Test
    fun parseSubHeader_byTitle() {
        assertEquals(
            BostonGlobe.Companion.SubHeader("Test Title", "Test Author", "\u00a9 Test Copyright"),
            BostonGlobe.parseSubHeader("Test Title by Test Author, Test Copyright")
        )
        assertEquals(
            BostonGlobe.Companion.SubHeader("Test Title", "Test Author", "\u00a9 Test Copyright"),
            BostonGlobe.parseSubHeader("Test Title By Test Author, Test Copyright")
        )
        assertEquals(
            BostonGlobe.Companion.SubHeader("Test By Title", "Test Author", "\u00a9 Test Copyright"),
            BostonGlobe.parseSubHeader("Test By Title By Test Author, Test Copyright")
        )
        assertEquals(
            BostonGlobe.Companion.SubHeader("Test, Title", "Test Author", "\u00a9 Test Copyright"),
            BostonGlobe.parseSubHeader("Test, Title By Test Author, Test Copyright")
        )
    }

    @Test
    fun parseSubHeader_noCopyright() {
        assertEquals(
            BostonGlobe.Companion.SubHeader("Test Title", "Test Author", ""),
            BostonGlobe.parseSubHeader("Test Title by Test Author")
        )
    }

    @Test
    fun parseSubHeader_noAuthorOrCopyright() {
        assertEquals(
            BostonGlobe.Companion.SubHeader("Test Title", "", ""),
            BostonGlobe.parseSubHeader("Test Title")
        )
    }
}