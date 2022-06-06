package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.AcrossLite.Companion.asAcrossLiteBinary
import com.jeffpdavidson.kotwords.model.assertPuzzleEquals
import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readStringResource
import kotlinx.coroutines.test.runTest
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
                ).asPuzzle().asAcrossLiteBinary()
            )
        )
    }

    @Test
    fun toCrossword_isCircled() = runTest {
        assertTrue(
            readBinaryResource(PuzzleMeTest::class, "puz/test.puz").contentEquals(
                PuzzleMe(
                    readStringResource(PuzzleMeTest::class, "puzzleme/test-isCircled.json")
                ).asPuzzle().asAcrossLiteBinary()
            )
        )
    }

    @Test
    fun toCrossword_shadedCells() = runTest {
        assertTrue(
            readBinaryResource(PuzzleMeTest::class, "puz/test.puz").contentEquals(
                PuzzleMe(
                    readStringResource(PuzzleMeTest::class, "puzzleme/test-shadedCells.json")
                ).asPuzzle().asAcrossLiteBinary()
            )
        )
    }

    @Test
    fun toCrossword_voids() = runTest {
        assertTrue(
            readBinaryResource(PuzzleMeTest::class, "puz/test.puz").contentEquals(
                PuzzleMe(
                    readStringResource(PuzzleMeTest::class, "puzzleme/test-void.json")
                ).asPuzzle().asAcrossLiteBinary()
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
                ).asPuzzle().asAcrossLiteBinary()
            )
        )
    }

    @Test
    fun toCrossword_italics() = runTest {
        assertTrue(
            readBinaryResource(PuzzleMeTest::class, "puzzleme/test-italics.puz").contentEquals(
                PuzzleMe(
                    readStringResource(PuzzleMeTest::class, "puzzleme/test-italics.json")
                ).asPuzzle().asAcrossLiteBinary()
            )
        )
    }

    @Test
    fun toPuzzle_bgImage() = runTest {
        val puzzleMe = PuzzleMe(readStringResource(PuzzleMeTest::class, "puzzleme/test-bgimage.json"))
        assertPuzzleEquals(
            Jpz.fromXmlString(readStringResource(PuzzleMeTest::class, "jpz/test-bgimage.jpz")).asPuzzle(),
            puzzleMe.asPuzzle(),
        )
    }

    @Test
    fun toHtml_regularString() = assertEquals("regular", PuzzleMe.toHtml("regular"))

    @Test
    fun toHtml_italicString() = assertEquals("<i>italic</i>", PuzzleMe.toHtml("<i>italic</i>"))

    @Test
    fun toHtml_unsupportedTag() = assertEquals("&lt;div>div&lt;/div>", PuzzleMe.toHtml("<div>div</div>"))

    @Test
    fun toHtml_mixedTags() =
        assertEquals("&lt;div>div&lt;/div><SUP>super</SUP>", PuzzleMe.toHtml("<div>div</div><SUP>super</SUP>"))
}