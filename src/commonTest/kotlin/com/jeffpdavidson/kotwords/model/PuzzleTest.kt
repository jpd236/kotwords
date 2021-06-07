package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.formats.Jpz
import com.jeffpdavidson.kotwords.readStringResource
import com.jeffpdavidson.kotwords.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PuzzleTest {
    @Test
    fun jpzReadAndWrite() = runTest {
        val parsedJpz = Jpz.fromXmlString(readStringResource(PuzzleTest::class, "jpz/test.jpz"))
        val convertedJpz = Puzzle.fromCrossword(parsedJpz.asCrossword()).asJpzFile()
        assertEquals(parsedJpz, convertedJpz)
    }

    @Test
    fun jpzReadAndWrite_gaps() = runTest {
        val parsedJpz = Jpz.fromXmlString(readStringResource(PuzzleTest::class, "jpz/gaps.jpz"))
        val convertedJpz = Puzzle.fromCrossword(parsedJpz.asCrossword()).asJpzFile()
        assertEquals(parsedJpz, convertedJpz)
    }

    @Test
    fun jpzReadAndWrite_solved() = runTest {
        val parsedJpz = Jpz.fromXmlString(readStringResource(PuzzleTest::class, "jpz/test.jpz"))
        val convertedJpz = Puzzle.fromCrossword(parsedJpz.asCrossword()).asJpzFile(solved = true)
        assertEquals(readStringResource(PuzzleTest::class, "jpz/test-solved.jpz"), convertedJpz.toXmlString())
    }
}