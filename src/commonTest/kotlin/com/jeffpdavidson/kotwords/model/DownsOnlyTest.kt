package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.formats.AcrossLite
import com.jeffpdavidson.kotwords.model.DownsOnly.getDirectionToClearForDownsOnly
import com.jeffpdavidson.kotwords.readBinaryResource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DownsOnlyTest {
    @Test
    fun getDirectionToClearForDownsOnly() = runTest {
        val puzzle = AcrossLite(readBinaryResource(DownsOnlyTest::class, "puz/test.puz")).asPuzzle()
        assertEquals(DownsOnly.ClueDirection.DOWN, puzzle.getDirectionToClearForDownsOnly())
    }
}