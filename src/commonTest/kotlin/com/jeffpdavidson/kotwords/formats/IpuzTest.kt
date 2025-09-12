package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.json.JsonSerializer
import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readStringResource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IpuzTest {

    @Test
    fun readAndWrite_solved() = runTest {
        val ipuz = Ipuz(readStringResource(IpuzTest::class, "ipuz/test.ipuz"))
        val convertedIpuz = ipuz.asIpuzFile(solved = true).decodeToString()
        assertEquals(readStringResource(IpuzTest::class, "ipuz/test-solved.ipuz"), convertedIpuz)
    }

    @Test
    fun readAndWrite_zeroIndexed() = runTest {
        val ipuz = Ipuz(readStringResource(IpuzTest::class, "ipuz/test-zero-indexed.ipuz"))
        val convertedIpuz = ipuz.asPuzzle().asIpuzFile().decodeToString()
        assertEquals(readStringResource(IpuzTest::class, "ipuz/test.ipuz"), convertedIpuz)
    }

    @Test
    fun readAndWrite_noSolution() = runTest {
        val ipuzFile = readStringResource(IpuzTest::class, "ipuz/test-no-solution.ipuz")
        val ipuz = Ipuz(ipuzFile)
        val puzzle = ipuz.asPuzzle()
        assertFalse(puzzle.hasSolution())
        val convertedIpuz = puzzle.asIpuzFile().decodeToString()
        assertEquals(ipuzFile, convertedIpuz)
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
    fun crosswordWithDiagonalClue() = runTest {
        assertEquals(
            JpzFile(readBinaryResource(JpzTest::class, "jpz/test-diagonal.jpz")).getPuzzleable(),
            Ipuz(readStringResource(IpuzTest::class, "ipuz/test-diagonal.ipuz")).asPuzzle().asJpz()
        )
    }

    @Test
    fun puzzleConversion_crossword() = runTest { assertConversionIsEqual("ipuz/test.ipuz") }

    @Test
    fun puzzleConversion_crosswordWithClueGaps() = runTest { assertConversionIsEqual("ipuz/gaps.ipuz") }

    @Test
    fun puzzleConversion_crosswordWithDiagonalClue() = runTest { assertConversionIsEqual("ipuz/test-diagonal.ipuz") }

    @Test
    fun puzzleConversion_crosswordWithBgImage() = runTest { assertConversionIsEqual("ipuz/test-bgimage.ipuz") }

    @Test
    fun puzzleConversion_diagramlessCrossword() = runTest { assertConversionIsEqual("ipuz/test-diagramless.ipuz") }

    @Test
    fun puzzleConversion_aroundTheBend() = runTest { assertConversionIsEqual("around-the-bend/around-the-bend.ipuz") }

    @Test
    fun puzzleConversion_coded() = runTest { assertConversionIsEqual("coded/coded.ipuz") }

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