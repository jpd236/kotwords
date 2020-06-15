package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.AcrossLite.Companion.toAcrossLiteBinary
import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readStringResource
import com.jeffpdavidson.kotwords.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class JpzTest {
    @Test
    fun crossword() = runTest {
        assertArrayEquals(
                readBinaryResource(JpzTest::class, "puz/test.puz"),
                Jpz(readStringResource(JpzTest::class, "jpz/test.jpz"))
                        .asCrossword().toAcrossLiteBinary()
        )
    }

    @Test
    fun crosswordWithClueGaps() = runTest {
        assertArrayEquals(
                readBinaryResource(JpzTest::class, "puz/gaps.puz"),
                Jpz(readStringResource(JpzTest::class, "jpz/gaps.jpz"))
                        .asCrossword().toAcrossLiteBinary()
        )
    }
}