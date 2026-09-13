package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.formats.Ipuz
import com.jeffpdavidson.kotwords.formats.JpzFile
import com.jeffpdavidson.kotwords.formats.pdf.getNotoSerifFontFamily
import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readStringResource
import io.github.pdvrieze.xmlutil.testutil.assertXmlEquals
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ShapeshifterTest {

    @Test
    fun jpzGeneration() = runTest {
        val puzzle = SHAPESHIFTER.asPuzzle()
        val expected = readStringResource(ShapeshifterTest::class, "shapeshifter/shapeshifter.jpz")
        assertXmlEquals(expected, puzzle.asJpz().toXmlString())
    }

    @Test
    fun ipuzGeneration() = runTest {
        val puzzle = SHAPESHIFTER.asPuzzle()
        val expected = readStringResource(ShapeshifterTest::class, "shapeshifter/shapeshifter.ipuz")
        assertEquals(expected, Ipuz.asIpuzJson(puzzle).toJsonString())
    }

    @Test
    fun ipuzGeneration_unlabeledClues() = runTest {
        val puzzle = SHAPESHIFTER.copy(labelClues = false).asPuzzle()
        val expected = readStringResource(ShapeshifterTest::class, "shapeshifter/shapeshifter-unlabeled.ipuz")
        assertEquals(expected, Ipuz.asIpuzJson(puzzle).toJsonString())
    }

    @Test
    fun pdfGeneration() = runTest {
        assertContentEquals(
            readBinaryResource(ShapeshifterTest::class, "shapeshifter/shapeshifter.pdf"),
            JpzFile(readBinaryResource(ShapeshifterTest::class, "shapeshifter/shapeshifter.jpz"))
                .asPuzzle().asPdf(blackSquareLightnessAdjustment = 0.75, fontFamily = getNotoSerifFontFamily())
        )
    }

    @Test
    fun invalidLongsAnswers_fails() {
        assertFailsWith<IllegalArgumentException> {
            SHAPESHIFTER.copy(
                longsAnswers = listOf(
                    "DABC",
                    "GHEX", // Mismatch
                    "JKLI",
                )
            )
        }
    }

    companion object {
        val SHAPESHIFTER = Shapeshifter(
            title = "Test title",
            creator = "Test creator",
            copyright = "Test copyright",
            description = "Test description",
            shortsAnswers = listOf(
                "ABC",
                "DEF",
                "GHI",
                "JKL",
            ),
            shortsClues = listOf(
                "Short clue 1",
                "Short clue 2",
                "Short clue 3",
                "Short clue 4",
            ),
            longsClues = listOf(
                "Long clue 1",
                "Long clue 2",
                "Long clue 3",
            ),
            longsAnswers = listOf(
                "DABC",
                "GHEF",
                "JKLI",
            ),
        )
    }
}
