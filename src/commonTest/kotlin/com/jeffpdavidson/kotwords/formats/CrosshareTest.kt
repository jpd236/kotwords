package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readStringResource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class CrosshareTest {
    @Test
    fun asCrossword() = runTest {
        assertTrue(
            readBinaryResource(CrosshareTest::class, "puz/test.puz").contentEquals(
                Crosshare(readStringResource(CrosshareTest::class, "crosshare/test.json"))
                    .asPuzzle().asAcrossLiteBinary()
            )
        )
    }

    @Test
    fun asCrossword_htmlDescription() = runTest {
        assertTrue(
            readBinaryResource(CrosshareTest::class, "puz/test.puz").contentEquals(
                Crosshare(readStringResource(CrosshareTest::class, "crosshare/test-htmlDescription.json"))
                    .asPuzzle().asAcrossLiteBinary()
            )
        )
    }
}