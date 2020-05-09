package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.AcrossLite.Companion.toAcrossLiteBinary
import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class AcrossLiteTest {
    @Test
    fun readAndWrite_basic() = runTest {
        val data = readBinaryResource(AcrossLiteTest::class, "puz/test-simple.puz")
        val result = AcrossLite(data).asCrossword()
        assertTrue(data.contentEquals(result.toAcrossLiteBinary()))
    }

    @Test
    fun readAndWrite_notesCirclesAndRebus() = runTest {
        val data = readBinaryResource(AcrossLiteTest::class, "puz/test.puz")
        assertTrue(data.contentEquals(AcrossLite(data).asCrossword().toAcrossLiteBinary()))
    }

    @Test
    fun writeSolved() = runTest {
        val data = readBinaryResource(AcrossLiteTest::class, "puz/test.puz")
        assertTrue(
                readBinaryResource(AcrossLiteTest::class, "puz/test-solved.puz").contentEquals(
                        AcrossLite(data).asCrossword().toAcrossLiteBinary(solved = true)))
    }
}