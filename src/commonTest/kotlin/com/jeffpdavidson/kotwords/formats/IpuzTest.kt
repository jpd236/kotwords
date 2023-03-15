package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.json.JsonSerializer
import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readStringResource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IpuzTest {

    @Test
    fun readAndWrite_solved() = runTest {
        val ipuz = Ipuz(readStringResource(IpuzTest::class, "ipuz/test.ipuz"))
        val convertedIpuz = ipuz.asIpuzFile(solved = true).decodeToString()
        assertEquals(readStringResource(IpuzTest::class, "ipuz/test-solved.ipuz"), convertedIpuz)
    }

    @Test
    fun crossword() = runTest {
        assertTrue(
            readBinaryResource(IpuzTest::class, "puz/test.puz").contentEquals(
                Ipuz(readStringResource(IpuzTest::class, "ipuz/test.ipuz")).asPuzzle().asAcrossLiteBinary()
            )
        )
    }

    @Test
    fun crosswordWithClueGaps() = runTest {
        assertTrue(
            readBinaryResource(IpuzTest::class, "puz/gaps.puz").contentEquals(
                Ipuz(readStringResource(IpuzTest::class, "ipuz/gaps.ipuz")).asPuzzle().asAcrossLiteBinary()
            )
        )
    }

    @Test
    fun puzzleConversion_crossword() = runTest { assertConversionIsEqual("ipuz/test.ipuz") }

    @Test
    fun puzzleConversion_crosswordWithClueGaps() = runTest { assertConversionIsEqual("ipuz/gaps.ipuz") }

    @Test
    fun puzzleConversion_crosswordWithBgImage() = runTest { assertConversionIsEqual("ipuz/test-bgimage.ipuz") }

    @Test
    fun puzzleConversion_diagramlessCrossword() = runTest { assertConversionIsEqual("ipuz/test-diagramless.ipuz") }

    @Test
    fun puzzleConversion_aroundTheBend() = runTest { assertConversionIsEqual("around-the-bend/around-the-bend.ipuz") }

    @Test
    fun puzzleConversion_eightTracks() = runTest { assertConversionIsEqual("eight-tracks/annotations.ipuz") }

    @Test
    fun puzzleConversion_jellyRoll() = runTest { assertConversionIsEqual("jelly-roll/jelly-roll.ipuz") }

    @Test
    fun puzzleConversion_labyrinth() = runTest { assertConversionIsEqual("labyrinth/labyrinth.ipuz") }

    @Test
    fun puzzleConversion_marchingBands() = runTest { assertConversionIsEqual("marching-bands/marching-bands.ipuz") }

    @Test
    fun puzzleConversion_rowsGarden() = runTest { assertConversionIsEqual("rows-garden/rows-garden.ipuz") }

    @Test
    fun puzzleConversion_snakeCharmer() = runTest { assertConversionIsEqual("snake-charmer/snake-charmer.ipuz") }

    @Test
    fun puzzleConversion_spellWeaving() = runTest { assertConversionIsEqual("spell-weaving/spell-weaving.ipuz") }

    @Test
    fun puzzleConversion_spiral() = runTest { assertConversionIsEqual("spiral/spiral.ipuz") }

    @Test
    fun puzzleConversion_twistsAndTurns() = runTest {
        assertConversionIsEqual("twists-and-turns/twists-and-turns.ipuz")
    }

    @Test
    fun puzzleConversion_twoTone() = runTest { assertConversionIsEqual("two-tone/two-tone.ipuz") }

    private suspend fun assertConversionIsEqual(ipuzPath: String) {
        val ipuzJson = readStringResource(IpuzTest::class, ipuzPath)
        val convertedIpuz = Ipuz.asIpuzJson(Ipuz(ipuzJson).asPuzzle())
        assertEquals(JsonSerializer.fromJson(ipuzJson), convertedIpuz)
    }
}