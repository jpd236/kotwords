package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readStringResource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CrosswordrTest {
    @Test
    fun crossword() = runTest {
        assertEquals(
            JpzFile(readBinaryResource(CrosswordrTest::class, "jpz/test.jpz")).asPuzzle().copy(copyright = ""),
            Crosswordr(readStringResource(CrosswordrTest::class, "crosswordr/test.json")).asPuzzle()
        )
    }
}