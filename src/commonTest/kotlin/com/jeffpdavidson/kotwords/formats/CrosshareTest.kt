package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.AcrossLite.Companion.toAcrossLiteBinary
import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readStringResource
import com.jeffpdavidson.kotwords.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class CrosshareTest {
    @Test
    fun asCrossword() = runTest {
        assertTrue(
            readBinaryResource(CrosshareTest::class, "puz/test.puz").contentEquals(
                Crosshare(readStringResource(CrosshareTest::class, "crosshare.json"))
                    .asCrossword().toAcrossLiteBinary()
            )
        )
    }
}