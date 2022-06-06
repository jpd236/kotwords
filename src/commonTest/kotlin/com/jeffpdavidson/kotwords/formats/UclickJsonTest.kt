package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.AcrossLite.Companion.asAcrossLiteBinary
import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readStringResource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UclickJsonTest {
    @Test
    fun crossword() = runTest {
        assertTrue(
            readBinaryResource(UclickJsonTest::class, "puz/test-simple.puz").contentEquals(
                UclickJson(
                    readStringResource(UclickJsonTest::class, "uclick/test-simple.json"),
                    copyright = "Jeff Davidson",
                    addDateToTitle = false
                ).asPuzzle().asAcrossLiteBinary()
            )
        )
    }

    @Test
    fun urlDecoding() = runTest {
        val puzzle = UclickJson(readStringResource(UclickJsonTest::class, "uclick/test-urldecode.json")).asPuzzle()
        assertEquals("Author A & Author B", puzzle.creator)
        assertEquals("\"Example Puzzle\" - Monday, January 1, 2018", puzzle.title)
    }
}