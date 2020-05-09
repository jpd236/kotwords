package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.AcrossLite.Companion.toAcrossLiteBinary
import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readUtf8Resource
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class JpzTest {
    @Test
    fun crossword() {
        assertArrayEquals(
                JpzTest::class.readBinaryResource("puz/test.puz"),
                Jpz(JpzTest::class.readUtf8Resource("jpz/test.jpz"))
                        .asCrossword().toAcrossLiteBinary())
    }

    @Test
    fun crosswordWithClueGaps() {
        assertArrayEquals(
                JpzTest::class.readBinaryResource("puz/gaps.puz"),
                Jpz(JpzTest::class.readUtf8Resource("jpz/gaps.jpz"))
                        .asCrossword().toAcrossLiteBinary())
    }
}