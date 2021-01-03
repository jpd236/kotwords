package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.AcrossLite.Companion.toAcrossLiteBinary
import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readStringResource
import com.jeffpdavidson.kotwords.runTest
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
                ).asCrossword().toAcrossLiteBinary()
            )
        )
    }

    @Test
    fun urlDecoding() = runTest {
        val crossword =
            UclickJson(readStringResource(UclickJsonTest::class, "uclick/test-urldecode.json"))
                .asCrossword()
        assertEquals("Author A & Author B", crossword.author)
        assertEquals("\"Example Puzzle\" - Monday, January 1, 2018", crossword.title)
    }
}