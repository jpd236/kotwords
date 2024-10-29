package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readStringResource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class GuardianTest {
    @Test
    fun crossword() = runTest {
        assertTrue(
            readBinaryResource(GuardianTest::class, "puz/test-simple.puz").contentEquals(
                Guardian(
                    readStringResource(GuardianTest::class, "guardian/test-simple.json"),
                    copyright = "\u00a9 2018 Jeff Davidson",
                ).asPuzzle().asAcrossLiteBinary()
            )
        )
    }
}