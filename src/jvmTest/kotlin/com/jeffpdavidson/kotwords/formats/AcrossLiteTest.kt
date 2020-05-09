package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.AcrossLite.Companion.toAcrossLiteBinary
import com.jeffpdavidson.kotwords.model.Crossword
import com.jeffpdavidson.kotwords.readBinaryResource
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class AcrossLiteTest {
    @Test
    fun readAndWrite_basic() {
        val data = AcrossLiteTest::class.readBinaryResource("puz/test-simple.puz")
        val result = AcrossLite(data).asCrossword()
        assertArrayEquals(data, result.toAcrossLiteBinary())
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
