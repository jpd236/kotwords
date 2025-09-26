package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readStringResource
import kotlinx.coroutines.test.runTest
import kotlin.test.assertEquals
import kotlin.test.Test

class XdTest {

    @Test
    fun crossword() = runTest {
        assertEquals(
            JpzFile(readBinaryResource(XdTest::class, "jpz/test.jpz")).asPuzzle(),
            Xd(readStringResource(XdTest::class, "xd/test.xd")).asPuzzle()
        )
    }

    @Test
    fun crossword_barred() = runTest {
        assertEquals(
            JpzFile(readBinaryResource(XdTest::class, "jpz/test-barred.jpz")).asPuzzle().copy(completionMessage = ""),
            Xd(readStringResource(XdTest::class, "xd/test-barred.xd")).asPuzzle()
        )
    }

    @Test
    fun crossword_formatting() = runTest {
        assertEquals(
            JpzFile(readBinaryResource(XdTest::class, "jpz/test-html.jpz")).asPuzzle(),
            Xd(readStringResource(XdTest::class, "xd/test-formatting.xd")).asPuzzle()
        )
    }

    @Test
    fun crossword_shaded() = runTest {
        assertEquals(
            JpzFile(readBinaryResource(XdTest::class, "jpz/test-shaded.jpz")).asPuzzle(),
            Xd(readStringResource(XdTest::class, "xd/test-shaded.xd")).asPuzzle()
        )
    }

    @Test
    fun crossword_prefilled() = runTest {
        assertEquals(
            JpzFile(readBinaryResource(XdTest::class, "jpz/test-prefilled.jpz")).asPuzzle(),
            Xd(readStringResource(XdTest::class, "xd/test-prefilled.xd")).asPuzzle()
        )
    }
}