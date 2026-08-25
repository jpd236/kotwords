package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.readStringResource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class TelegraphTest {

    @Test
    fun crossword() = runTest {
        assertEquals(
            Jpz.fromXmlString(readStringResource(TelegraphTest::class, "telegraph/test.jpz")),
            Telegraph(
                readStringResource(TelegraphTest::class, "telegraph/test.json"),
                copyright = "© 2018 Jeff Davidson",
            ).asJpz(),
        )
    }

    @Test
    fun crossword_noSolution() = runTest {
        assertEquals(
            Jpz.fromXmlString(readStringResource(TelegraphTest::class, "telegraph/test-no-solution.jpz")),
            Telegraph(
                readStringResource(TelegraphTest::class, "telegraph/test-no-solution.json"),
                copyright = "© 2018 Jeff Davidson",
            ).asJpz(),
        )
    }
}
