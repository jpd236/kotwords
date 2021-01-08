package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.AcrossLite.Companion.toAcrossLiteBinary
import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readStringResource
import com.jeffpdavidson.kotwords.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
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
        assertTrue(
            readBinaryResource(PuzzleMeTest::class, "puz/test.puz").contentEquals(
                PuzzleMe(
                    readStringResource(PuzzleMeTest::class, "puzzleme/test.json")
                ).asCrossword()
                    .toAcrossLiteBinary()
            )
        )
    }

    @Test
    fun toCrossword_isCircled() = runTest {
        assertTrue(
            readBinaryResource(PuzzleMeTest::class, "puz/test.puz").contentEquals(
                PuzzleMe(
                    readStringResource(PuzzleMeTest::class, "puzzleme/test-isCircled.json")
                ).asCrossword()
                    .toAcrossLiteBinary()
            )
        )
    }

    @Test
    fun toCrossword_shadedCells() = runTest {
        assertTrue(
            readBinaryResource(PuzzleMeTest::class, "puz/test.puz").contentEquals(
                PuzzleMe(
                    readStringResource(PuzzleMeTest::class, "puzzleme/test-shadedCells.json")
                ).asCrossword()
                    .toAcrossLiteBinary()
            )
        )
    }

    @Test
    fun toCrossword_voids() = runTest {
        assertTrue(
            readBinaryResource(PuzzleMeTest::class, "puz/test.puz").contentEquals(
                PuzzleMe(
                    readStringResource(PuzzleMeTest::class, "puzzleme/test-void.json")
                ).asCrossword()
                    .toAcrossLiteBinary()
            )
        )
    }

    @Test
    fun toCrossword_hiddenSquares() = runTest {
        assertTrue(
            readBinaryResource(PuzzleMeTest::class, "puz/test.puz").contentEquals(
                PuzzleMe(
                    readStringResource(
                        PuzzleMeTest::class,
                        "puzzleme/test-hiddenSquares.json"
                    )
                ).asCrossword()
                    .toAcrossLiteBinary()
            )
        )
    }

    @Test
    fun toCrossword_italics() = runTest {
        assertTrue(
            readBinaryResource(PuzzleMeTest::class, "puzzleme/test-italics.puz").contentEquals(
                PuzzleMe(
                    readStringResource(PuzzleMeTest::class, "puzzleme/test-italics.json")
                ).asCrossword()
                    .toAcrossLiteBinary()
            )
        )
    }
}