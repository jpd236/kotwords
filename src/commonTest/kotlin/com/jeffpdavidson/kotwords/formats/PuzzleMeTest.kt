package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.IgnoreNative
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
    fun extractPuzzleJson_newFormat() = runTest {
        assertEquals(
            readStringResource(PuzzleMeTest::class, "puzzleme/test.json")
                .replace("\r\n", "\n"),
            PuzzleMe.extractPuzzleJson(
                readStringResource(PuzzleMeTest::class, "puzzleme/test-new-rawc.html")
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
    fun decodeRawcFromOnReadyFn_withOnReadyFn() = runTest {
        val rawcParts = PuzzleMe.extractRawc(readStringResource(PuzzleMeTest::class, "puzzleme/test.html")).split(".")
        val rawc = rawcParts[0]
        val key = rawcParts[1].reversed()
        assertEquals(
            readStringResource(PuzzleMeTest::class, "puzzleme/test.json").replace("\r\n", "\n"),
            PuzzleMe.decodeRawc(rawc, """function(){function a(){var x="$key";}}""")
        )
    }

    @Test
    fun getCrosswordJsUrl() = runTest {
        assertEquals(
            "http://example.com/js/c-min.js?v=123456789",
            PuzzleMe.getCrosswordJsUrl(
                readStringResource(PuzzleMeTest::class, "puzzleme/test-new-rawc.html"),
                "http://example.com/puzzle.html"
            )
        )
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
    @IgnoreNative  // Depends on image support
    fun toPuzzle_bgImage() = runTest {
        val puzzleMe = PuzzleMe(readStringResource(PuzzleMeTest::class, "puzzleme/test-bgimage.json"))
        assertPuzzleEquals(
            JpzFile(readBinaryResource(PuzzleMeTest::class, "jpz/test-bgimage.jpz")).asPuzzle(),
            puzzleMe.asPuzzle(),
        )
    }

    @Test
    fun toCrossword_wordlens() = runTest {
        val puzzleMe = PuzzleMe(readStringResource(PuzzleMeTest::class, "puzzleme/test-wordlens.json"))
        assertPuzzleEquals(
            JpzFile(readBinaryResource(PuzzleMeTest::class, "jpz/test-formats.jpz")).asPuzzle(),
            puzzleMe.asPuzzle(),
        )
    }

    @Test
    fun toPuzzle_marchingBands() = runTest {
        val puzzleMe = PuzzleMe(readStringResource(PuzzleMeTest::class, "puzzleme/test-marchingBands.json"))
        assertPuzzleEquals(
            JpzFile(readBinaryResource(PuzzleMeTest::class, "marching-bands/marching-bands.jpz")).asPuzzle(),
            puzzleMe.asPuzzle().copy(completionMessage = "All done!"),
        )
    }

    @Test
    fun toPuzzle_rowsGarden() = runTest {
        val puzzleMe = PuzzleMe(readStringResource(PuzzleMeTest::class, "puzzleme/test-rowsGarden.json"))
        assertPuzzleEquals(
            JpzFile(readBinaryResource(PuzzleMeTest::class, "rows-garden/rows-garden.jpz")).asPuzzle(),
            puzzleMe.asPuzzle().copy(completionMessage = "All done!"),
        )
    }

    @Test
    fun toHtml_regularString() = assertEquals("regular", PuzzleMe.toHtml("regular"))

    @Test
    fun toHtml_italicString() = assertEquals("<i>italic</i>", PuzzleMe.toHtml("<i>italic</i>"))

    @Test
    fun toHtml_unsupportedDivTag() = assertEquals("div", PuzzleMe.toHtml("<div>div</div>"))

    @Test
    fun toHtml_unsupportedImgTag() = assertEquals("", PuzzleMe.toHtml("<img src=\"url\"/>"))

    @Test
    fun toHtml_mixedTags() =
        assertEquals("div<SUP>super</SUP>", PuzzleMe.toHtml("<div>div</div><SUP>super</SUP>"))

    @Test
    fun toHtml_links() =
        assertEquals(
            "test link and <b>test bold link</b>",
            PuzzleMe.toHtml("<a href=\"abc\">test link</a> and <a href=\"def\"><b>test bold link</b></a>")
        )

    @Test
    fun toHtml_htmlEntities() =
        assertEquals(
            "Test with \"Quotes\" ' &lt; &amp; &amp;",
            PuzzleMe.toHtml("Test with &#34;Quotes&#34; &#39; < & &amp;")
        )
}