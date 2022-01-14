package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.Jpz.Companion.asJpzFile
import com.jeffpdavidson.kotwords.model.assertPuzzleEquals
import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readStringResource
import com.jeffpdavidson.kotwords.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class NewYorkTimesTest {
    @Test
    fun extractPuzzleJson() = runTest {
        assertEquals(
            readStringResource(NewYorkTimesTest::class, "nyt/test.json").replace("\r\n", "\n"),
            NewYorkTimes.extractPuzzleJson(readStringResource(NewYorkTimesTest::class, "nyt/test.html"))
        )
    }

    @Test
    fun extractPuzzleJson_invalid() {
        try {
            NewYorkTimes.extractPuzzleJson("nothing to see here <script>empty</script>")
            fail()
        } catch (e: InvalidFormatException) {
            // expected
        }
    }

    @Test
    fun getBorderWidth() = runTest {
        val nyt = NewYorkTimes(readStringResource(NewYorkTimesTest::class, "nyt/test-bgimage.json"))
        assertEquals(6.0, nyt.getBorderWidth(506)!!, 0.01)
    }

    @Test
    fun toPuzzle() = runTest {
        val puzzle = NewYorkTimes(readStringResource(NewYorkTimesTest::class, "nyt/test.json")).asPuzzle()
        assertEquals(readStringResource(NewYorkTimesTest::class, "nyt/test.jpz"), puzzle.asJpzFile().toXmlString())
    }

    @Test
    fun toPuzzle_bgImage_noFetcher() = runTest {
        val puzzle = NewYorkTimes(readStringResource(NewYorkTimesTest::class, "nyt/test-bgimage.json")).asPuzzle()
        assertEquals(
            readStringResource(NewYorkTimesTest::class, "nyt/test-bgimage-nofetcher.jpz"),
            puzzle.asJpzFile().toXmlString()
        )
    }

    @Test
    fun toPuzzle_bgImage() = runTest {
        val puzzle = NewYorkTimes(
            readStringResource(NewYorkTimesTest::class, "nyt/test-bgimage.json"),
            httpGetter = { url ->
                assertEquals("https://fake.url/bgimage.png", url)
                readBinaryResource(NewYorkTimesTest::class, "nyt/bgimage.png")
            },
        ).asPuzzle()
        assertPuzzleEquals(
            Jpz.fromXmlString(readStringResource(NewYorkTimesTest::class, "nyt/test-bgimage.jpz")).asPuzzle(),
            puzzle,
        )
    }
}