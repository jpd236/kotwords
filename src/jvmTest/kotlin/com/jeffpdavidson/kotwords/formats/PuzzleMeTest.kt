package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.AcrossLite.Companion.toAcrossLiteBinary
import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readStringResource
import com.jeffpdavidson.kotwords.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class PuzzleMeTest {
    @Test
    fun extractPuzzleJson() = runTest {
        assertEquals(
            readStringResource(PuzzleMeTest::class, "puzzleme/test.json")
                .replace("\r\n", "\n"),
            PuzzleMe.extractPuzzleJson(
                readStringResource(PuzzleMeTest::class, "puzzleme/test.html")
            )
        )
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
    fun toCrossword() = runTest {
        assertArrayEquals(
            readBinaryResource(PuzzleMeTest::class, "puz/test.puz"),
            PuzzleMe.toCrossword(
                readStringResource(PuzzleMeTest::class, "puzzleme/test.json")
            )
                .toAcrossLiteBinary()
        )
    }

    @Test
    fun toCrossword_isCircled() = runTest {
        assertArrayEquals(
            readBinaryResource(PuzzleMeTest::class, "puz/test.puz"),
            PuzzleMe.toCrossword(
                readStringResource(PuzzleMeTest::class, "puzzleme/test-isCircled.json")
            )
                .toAcrossLiteBinary()
        )
    }

    @Test
    fun toCrossword_shadedCells() = runTest {
        assertArrayEquals(
            readBinaryResource(PuzzleMeTest::class, "puz/test.puz"),
            PuzzleMe.toCrossword(
                readStringResource(PuzzleMeTest::class, "puzzleme/test-shadedCells.json")
            )
                .toAcrossLiteBinary()
        )
    }

    @Test
    fun toCrossword_voids() = runTest {
        assertArrayEquals(
            readBinaryResource(PuzzleMeTest::class, "puz/test.puz"),
            PuzzleMe.toCrossword(
                readStringResource(PuzzleMeTest::class, "puzzleme/test-void.json")
            )
                .toAcrossLiteBinary()
        )
    }

    @Test
    fun toCrossword_hiddenSquares() = runTest {
        assertArrayEquals(
            readBinaryResource(PuzzleMeTest::class, "puz/test.puz"),
            PuzzleMe.toCrossword(
                readStringResource(
                    PuzzleMeTest::class,
                    "puzzleme/test-hiddenSquares.json"
                )
            )
                .toAcrossLiteBinary()
        )
    }

    @Test
    fun toCrossword_italics() = runTest {
        assertArrayEquals(
            readBinaryResource(PuzzleMeTest::class, "puzzleme/test-italics.puz"),
            PuzzleMe.toCrossword(
                readStringResource(PuzzleMeTest::class, "puzzleme/test-italics.json")
            )
                .toAcrossLiteBinary()
        )
    }
}