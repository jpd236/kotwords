package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readStringResource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PzzlTest {
    @Test
    fun crossword() = runTest {
        assertEquals(
            AcrossLite(readBinaryResource(PzzlTest::class, "puz/test.puz")).asPuzzle().copy(copyright = ""),
            Pzzl(readStringResource(PzzlTest::class, "pzzl/test.txt")).asPuzzle()
        )
    }
}