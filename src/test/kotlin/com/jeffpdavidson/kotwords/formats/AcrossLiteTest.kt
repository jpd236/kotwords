package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.AcrossLite.Companion.toAcrossLiteBinary
import com.jeffpdavidson.kotwords.readBinaryResource
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test

class AcrossLiteTest {
    @Test
    fun readAndWrite_basic() {
        val data = AcrossLiteTest::class.readBinaryResource("puz/test-simple.puz")
        assertArrayEquals(data, AcrossLite(data).asCrossword().toAcrossLiteBinary())
    }

    @Test
    fun readAndWrite_notesCirclesAndRebus() {
        val data = AcrossLiteTest::class.readBinaryResource("puz/test.puz")
        assertArrayEquals(data, AcrossLite(data).asCrossword().toAcrossLiteBinary())
    }

    @Test
    fun writeSolved() {
        val data = AcrossLiteTest::class.readBinaryResource("puz/test.puz")
        assertArrayEquals(
                AcrossLiteTest::class.readBinaryResource("puz/test-solved.puz"),
                AcrossLite(data).asCrossword().toAcrossLiteBinary(solved = true))
    }
}
