package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.formats.AcrossLite
import com.jeffpdavidson.kotwords.readBinaryResource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DownsOnlyTest {
    @Test
    fun getDirectionToClearForDownsOnly() {
        val crossword = AcrossLite(
                DownsOnlyTest::class.readBinaryResource("puz/test.puz")).asCrossword()
        assertEquals(ClueDirection.DOWN, crossword.getDirectionToClearForDownsOnly())
    }
}