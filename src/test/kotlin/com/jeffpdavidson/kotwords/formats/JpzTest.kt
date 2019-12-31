package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.AcrossLite.Companion.toAcrossLiteBinary
import com.jeffpdavidson.kotwords.model.BLACK_SQUARE
import com.jeffpdavidson.kotwords.model.Square
import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readUtf8Resource
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class JpzTest {
    @Test
    fun crossword() {
        Assertions.assertArrayEquals(
                JpzTest::class.readBinaryResource("puz/test.puz"),
                Jpz(JpzTest::class.readUtf8Resource("jpz/test.jpz"))
                        .asCrossword().toAcrossLiteBinary())
    }

    @Test
    fun crosswordWithClueGaps() {
        Assertions.assertArrayEquals(
                JpzTest::class.readBinaryResource("puz/gaps.puz"),
                Jpz(JpzTest::class.readUtf8Resource("jpz/gaps.jpz"))
                        .asCrossword().toAcrossLiteBinary())
    }
}