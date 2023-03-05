package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readStringResource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class XWordInfoTest {
    @Test
    fun crossword() = runTest {
        assertTrue(
            readBinaryResource(XWordInfoTest::class, "puz/test.puz").contentEquals(
                XWordInfo(
                    readStringResource(XWordInfoTest::class, "xwordinfo/test.json")
                ).asPuzzle().asAcrossLiteBinary()
            )
        )
    }
}