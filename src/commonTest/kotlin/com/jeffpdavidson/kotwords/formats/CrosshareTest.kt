package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.model.assertPuzzleEquals
import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readStringResource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CrosshareTest {
    @Test
    fun asCrossword() = runTest {
        assertTrue(
            readBinaryResource(CrosshareTest::class, "puz/test.puz").contentEquals(
                Crosshare(readStringResource(CrosshareTest::class, "crosshare/test.json"))
                    .asPuzzle().asAcrossLiteBinary()
            )
        )
    }

    @Test
    fun asCrossword_html() = runTest {
        assertPuzzleEquals(
            JpzFile(readBinaryResource(PuzzleMeTest::class, "jpz/test-html.jpz")).asPuzzle(),
            Crosshare(readStringResource(CrosshareTest::class, "crosshare/test-html.json")).asPuzzle(),
        )
    }

    @Test
    fun asCrossword_barred() = runTest {
        assertEquals(
            Jpz.fromXmlString(readStringResource(PuzzleMeTest::class, "jpz/test-barred.jpz")),
            Crosshare(readStringResource(CrosshareTest::class, "crosshare/test-barred.json")).asJpz(),
        )
    }
}