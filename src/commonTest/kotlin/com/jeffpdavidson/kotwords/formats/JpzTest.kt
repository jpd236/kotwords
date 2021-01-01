package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.AcrossLite.Companion.toAcrossLiteBinary
import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readStringResource
import com.jeffpdavidson.kotwords.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class JpzTest {
    @Test
    fun crossword() = runTest {
        assertTrue(
            readBinaryResource(JpzTest::class, "puz/test.puz").contentEquals(
                Jpz.fromXmlString(readStringResource(JpzTest::class, "jpz/test.jpz"))
                    .asCrossword().toAcrossLiteBinary()
            )
        )
    }

    @Test
    fun crosswordWithClueGaps() = runTest {
        assertTrue(
            readBinaryResource(JpzTest::class, "puz/gaps.puz").contentEquals(
                Jpz.fromXmlString(readStringResource(JpzTest::class, "jpz/gaps.jpz"))
                    .asCrossword().toAcrossLiteBinary()
            )
        )
    }
}