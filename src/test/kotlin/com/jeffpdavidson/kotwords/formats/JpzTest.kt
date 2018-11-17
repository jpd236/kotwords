package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.AcrossLite.Companion.toAcrossLiteBinary
import com.jeffpdavidson.kotwords.model.BLACK_SQUARE
import com.jeffpdavidson.kotwords.model.Square
import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readUtf8Resource
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class JpzTest {

    @Test
    fun crossword() {
        Assertions.assertArrayEquals(
                JpzTest::class.readBinaryResource("puz/test.puz"),
                Jpz(JpzTest::class.readUtf8Resource("jpz/test.jpz"))
                        .asCrossword().toAcrossLiteBinary())
    }

    @Test
    fun crosswordWithClueGaps() {
        Assertions.assertArrayEquals(
                JpzTest::class.readBinaryResource("puz/gaps.puz"),
                Jpz(JpzTest::class.readUtf8Resource("jpz/gaps.jpz"))
                        .asCrossword().toAcrossLiteBinary())
    }

    @Test
    fun mapGivenToSanitizedClueNumbers() {
        val rawGrid = listOf("XXX....",
                "XXX.XXX",
                "XXX.XXX",
                "XXX.XXX",
                "....XXX")
        val grid = rawGrid.map { row ->
            row.map { ch ->
                when (ch) {
                    '.' -> BLACK_SQUARE
                    else -> Square(ch)
                }
            }
        }
        // Skip 2-Down, 4-Across, and 5-Across/Down; add a fake 7-Down
        val givenSquareNumbers = mapOf(
                Pair(0, 0) to 1,
                Pair(2, 0) to 2,
                Pair(5, 1) to 3,
                Pair(6, 1) to 4,
                Pair(0, 2) to 5,
                Pair(4, 2) to 6,
                Pair(6, 2) to 7,
                Pair(0, 3) to 8,
                Pair(4, 3) to 9,
                Pair(4, 4) to 10)

        val expectedSanitizedMap = mapOf(
                1 to 1,
                2 to 3,
                3 to 6,
                4 to 7,
                5 to 8,
                6 to 9,
                8 to 10,
                9 to 11,
                10 to 12)
        assertEquals(expectedSanitizedMap,
                Jpz.mapGivenToSanitizedClueNumbers(grid, givenSquareNumbers))
    }

    @Test
    fun sanitizeClue_normalClue() {
        val givenClue = "Just a normal clue"
        val givenToSanitizedClueNumMap: Map<Int, Int> = mapOf()
        assertEquals("Just a normal clue", Jpz.sanitizeClue(givenClue, givenToSanitizedClueNumMap))
    }

    @Test
    fun sanitizeClue_simpleReplacement() {
        val givenClue = "See 25-Down"
        val givenToSanitizedClueNumMap = mapOf(25 to 27)
        assertEquals("See 27-Down", Jpz.sanitizeClue(givenClue, givenToSanitizedClueNumMap))
    }

    @Test
    fun sanitizeClue_complexReplacements() {
        val givenClue = "Where the end of 17-, 25- and 47-Across and 14- and 28-Down may be found"
        val givenToSanitizedClueNumMap = mapOf(14 to 14, 17 to 17, 25 to 26, 28 to 29, 47 to 49)
        assertEquals(
                "Where the end of 17-, 26- and 49-Across and 14- and 29-Down may be found",
                Jpz.sanitizeClue(givenClue, givenToSanitizedClueNumMap))
    }

    @Test
    fun sanitizeClue_specialCharacters() {
        val givenClue = "★Clue with a star"
        val givenToSanitizedClueNumMap: Map<Int, Int> = mapOf()
        assertEquals("*Clue with a star", Jpz.sanitizeClue(givenClue, givenToSanitizedClueNumMap))
    }
}