package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.readBinaryResource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AcrossLiteTest {
    @Test
    fun readAndWrite_basic() = runTest {
        val data = readBinaryResource(AcrossLiteTest::class, "puz/test-simple.puz")
        assertTrue(data.contentEquals(AcrossLite(data).asPuzzle().asAcrossLiteBinary()))
    }

    @Test
    fun readAndWrite_diagramless() = runTest {
        val data = readBinaryResource(AcrossLiteTest::class, "puz/test-diagramless.puz")
        assertTrue(data.contentEquals(AcrossLite(data).asPuzzle().asAcrossLiteBinary()))
    }

    @Test
    fun readAndWrite_noSolution() = runTest {
        val data = readBinaryResource(AcrossLiteTest::class, "puz/test-no-solution.puz")
        val puzzle = AcrossLite(data).asPuzzle()
        assertFalse(puzzle.hasSolution())
        assertTrue(data.contentEquals(puzzle.asAcrossLiteBinary()))
    }

    @Test
    fun readAndWrite_notesCirclesRebusUtf8() = runTest {
        val data = readBinaryResource(AcrossLiteTest::class, "puz/test.puz")
        assertTrue(data.contentEquals(AcrossLite(data).asPuzzle().asAcrossLiteBinary()))
    }

    @Test
    fun readAndWrite_notesCirclesRebusUtf8_forceIso88591() = runTest {
        val data = readBinaryResource(AcrossLiteTest::class, "puz/test.puz")
        assertTrue(
            readBinaryResource(AcrossLiteTest::class, "puz/test-iso-8859-1.puz").contentEquals(
                AcrossLite(data).asPuzzle().asAcrossLiteBinary(writeUtf8 = false)
            )
        )
    }

    @Test
    fun writeSolved() = runTest {
        val data = readBinaryResource(AcrossLiteTest::class, "puz/test.puz")
        assertTrue(
            readBinaryResource(AcrossLiteTest::class, "puz/test-solved.puz").contentEquals(
                AcrossLite(data).asPuzzle().asAcrossLiteBinary(solved = true)
            )
        )
    }
}