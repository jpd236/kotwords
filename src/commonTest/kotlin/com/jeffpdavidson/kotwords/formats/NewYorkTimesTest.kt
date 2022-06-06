package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.Jpz.Companion.asJpzFile
import com.jeffpdavidson.kotwords.model.assertPuzzleEquals
import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readStringResource
import kotlinx.coroutines.test.runTest
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
        val json = readStringResource(NewYorkTimesTest::class, "nyt/test-bgimage.json")
        val nyt = NewYorkTimes.fromPluribusJson(json)
        assertEquals(6.0, nyt.getBorderWidth(506)!!, 0.01)
    }

    @Test
    fun toPuzzle() = runTest {
        val json = readStringResource(NewYorkTimesTest::class, "nyt/test.json")
        val puzzle = NewYorkTimes.fromPluribusJson(json).asPuzzle()
        assertEquals(readStringResource(NewYorkTimesTest::class, "nyt/test.jpz"), puzzle.asJpzFile().toXmlString())
    }

    @Test
    fun toPuzzle_api() = runTest {
        val json = readStringResource(NewYorkTimesTest::class, "nyt/test-api.json")
        val puzzle = NewYorkTimes.fromApiJson(json, "daily").asPuzzle()
        assertEquals(readStringResource(NewYorkTimesTest::class, "nyt/test.jpz"), puzzle.asJpzFile().toXmlString())
    }

    @Test
    fun toPuzzle_bgImage_noFetcher() = runTest {
        val json = readStringResource(NewYorkTimesTest::class, "nyt/test-bgimage.json")
        val puzzle = NewYorkTimes.fromPluribusJson(json).asPuzzle()
        assertEquals(
            readStringResource(NewYorkTimesTest::class, "nyt/test-bgimage-nofetcher.jpz"),
            puzzle.asJpzFile().toXmlString()
        )
    }

    @Test
    fun toPuzzle_bgImage_api_noFetcher() = runTest {
        val json = readStringResource(NewYorkTimesTest::class, "nyt/test-bgimage-api.json")
        val puzzle = NewYorkTimes.fromApiJson(json, "daily").asPuzzle()
        assertEquals(
            readStringResource(NewYorkTimesTest::class, "nyt/test-bgimage-nofetcher.jpz"),
            puzzle.asJpzFile().toXmlString()
        )
    }

    @Test
    fun toPuzzle_bgImage_api() = runTest {
        val puzzle = NewYorkTimes.fromApiJson(
            readStringResource(NewYorkTimesTest::class, "nyt/test-bgimage-api.json"),
            "daily",
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