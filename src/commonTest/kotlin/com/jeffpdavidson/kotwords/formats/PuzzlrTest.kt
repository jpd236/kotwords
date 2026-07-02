package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readStringResource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class PuzzlrTest {
    @Test
    fun basicPuzzle() = runTest {
        assertTrue(
            readBinaryResource(PuzzlrTest::class, "puz/test.puz").contentEquals(
                Puzzlr(
                    readStringResource(PuzzlrTest::class, "puzzlr/test.json")
                ).asPuzzle().asAcrossLiteBinary()
            )
        )
    }

    @Test
    fun noSolution() = runTest {
        assertTrue(
            readBinaryResource(PuzzlrTest::class, "puz/test-no-solution.puz").contentEquals(
                Puzzlr(
                    readStringResource(PuzzlrTest::class, "puzzlr/test-no-solution.json")
                ).asPuzzle().asAcrossLiteBinary()
            )
        )
    }
}
