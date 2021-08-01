package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.AcrossLite.Companion.toAcrossLiteBinary
import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readStringResource
import com.jeffpdavidson.kotwords.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JpzTest {
    @Test
    fun crossword() = runTest {
        assertTrue(
            readBinaryResource(JpzTest::class, "puz/test.puz").contentEquals(
                Jpz.fromXmlString(readStringResource(JpzTest::class, "jpz/test.jpz"))
                    .asCrossword().toAcrossLiteBinary()
            )
        )
    }

    @Test
    fun crosswordWithClueGaps() = runTest {
        assertTrue(
            readBinaryResource(JpzTest::class, "puz/gaps.puz").contentEquals(
                Jpz.fromXmlString(readStringResource(JpzTest::class, "jpz/gaps.jpz"))
                    .asCrossword().toAcrossLiteBinary()
            )
        )
    }

    @Test
    fun puzzleConversion_acrostic() = runTest { assertConversionIsEqual("acrostic/acrostic-attribution.jpz") }

    @Test
    fun puzzleConversion_aroundTheBend() = runTest { assertConversionIsEqual("around-the-bend.jpz") }

    @Test
    fun puzzleConversion_crossword() = runTest { assertConversionIsEqual("jpz/test.jpz") }

    @Test
    fun puzzleConversion_crosswordWithClueGaps() = runTest { assertConversionIsEqual("jpz/gaps.jpz") }

    @Test
    fun puzzleConversion_eightTracks() = runTest { assertConversionIsEqual("eight-tracks/annotations.jpz") }

    @Test
    fun puzzleConversion_jellyRoll() = runTest { assertConversionIsEqual("jelly-roll/jelly-roll.jpz") }

    @Test
    fun puzzleConversion_labyrinth() = runTest { assertConversionIsEqual("labyrinth.jpz") }

    @Test
    fun puzzleConversion_marchingBands() = runTest { assertConversionIsEqual("marching-bands/marching-bands.jpz") }

    @Test
    fun puzzleConversion_rowsGarden() = runTest { assertConversionIsEqual("rows-garden/rows-garden.jpz") }

    @Test
    fun puzzleConversion_snakeCharmer() = runTest { assertConversionIsEqual("snake-charmer.jpz") }

    @Test
    fun puzzleConversion_spellWeaving() = runTest { assertConversionIsEqual("spell-weaving.jpz") }

    @Test
    fun puzzleConversion_spiral() = runTest { assertConversionIsEqual("spiral/spiral.jpz") }

    @Test
    fun puzzleConversion_twistsAndTurns() = runTest { assertConversionIsEqual("twists-and-turns.jpz") }

    @Test
    fun puzzleConversion_twoTone() = runTest { assertConversionIsEqual("two-tone/two-tone.jpz") }

    private suspend fun assertConversionIsEqual(jpzPath: String) {
        val jpz = Jpz.fromXmlString(readStringResource(JpzTest::class, jpzPath))
        assertEquals(jpz, jpz.asPuzzle().asJpzFile())
    }
}