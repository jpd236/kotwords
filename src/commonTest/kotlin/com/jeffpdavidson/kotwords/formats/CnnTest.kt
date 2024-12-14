package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readStringResource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CnnTest {
    @Test
    fun crossword() = runTest {
        assertEquals(
            JpzFile(readBinaryResource(CnnTest::class, "jpz/test.jpz")).asPuzzle(),
            Cnn(readStringResource(CnnTest::class, "cnn/test.json")).asPuzzle().copy(completionMessage = "")
        )
    }
}