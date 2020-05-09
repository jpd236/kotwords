package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.AcrossLite.Companion.toAcrossLiteBinary
import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readUtf8Resource
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class PuzzleMeTest {
    @Test
    fun extractPuzzleJson() {
        assertEquals(
                PuzzleMeTest::class.readUtf8Resource("puzzleme/test.json").replace("\r\n", "\n"),
                PuzzleMe.extractPuzzleJson(
                        PuzzleMeTest::class.readUtf8Resource("puzzleme/test.html")))
    }

    @Test
    fun extractPuzzleJson_invalid() {
        try {
            PuzzleMe.extractPuzzleJson("nothing to see here <script>empty</script>")
            fail()
        } catch (e: InvalidFormatException) {
            // expected
        }
    }

    @Test
    fun toCrossword() {
        assertArrayEquals(
                PuzzleMeTest::class.readBinaryResource("puz/test.puz"),
                PuzzleMe.toCrossword(
                        PuzzleMeTest::class.readUtf8Resource("puzzleme/test.json"))
                        .toAcrossLiteBinary())
    }

    @Test
    fun toCrossword_isCircled() {
        assertArrayEquals(
                PuzzleMeTest::class.readBinaryResource("puz/test.puz"),
                PuzzleMe.toCrossword(
                        PuzzleMeTest::class.readUtf8Resource("puzzleme/test-isCircled.json"))
                        .toAcrossLiteBinary())
    }

    @Test
    fun toCrossword_shadedCells() {
        assertArrayEquals(
                PuzzleMeTest::class.readBinaryResource("puz/test.puz"),
                PuzzleMe.toCrossword(
                        PuzzleMeTest::class.readUtf8Resource("puzzleme/test-shadedCells.json"))
                        .toAcrossLiteBinary())
    }

    @Test
    fun toCrossword_voids() {
        assertArrayEquals(
                PuzzleMeTest::class.readBinaryResource("puz/test.puz"),
                PuzzleMe.toCrossword(
                        PuzzleMeTest::class.readUtf8Resource("puzzleme/test-void.json"))
                        .toAcrossLiteBinary())
    }

    @Test
    fun toCrossword_hiddenSquares() {
        assertArrayEquals(
                PuzzleMeTest::class.readBinaryResource("puz/test.puz"),
                PuzzleMe.toCrossword(
                        PuzzleMeTest::class.readUtf8Resource("puzzleme/test-hiddenSquares.json"))
                        .toAcrossLiteBinary())
    }

    @Test
    fun toCrossword_italics() {
        assertArrayEquals(
                PuzzleMeTest::class.readBinaryResource("puzzleme/test-italics.puz"),
                PuzzleMe.toCrossword(
                        PuzzleMeTest::class.readUtf8Resource("puzzleme/test-italics.json"))
                        .toAcrossLiteBinary())
    }
}