package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.formats.JpzFile
import com.jeffpdavidson.kotwords.formats.pdf.getNotoSerifFontFamily
import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readStringResource
import io.github.pdvrieze.xmlutil.testutil.assertXmlEquals
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals

class CascadesTest {

    @Test
    fun jpzGeneration() = runTest {
        val puzzle = CASCADES.asPuzzle()
        val expected = readStringResource(CascadesTest::class, "cascades/cascades.jpz")
        assertXmlEquals(expected, puzzle.asJpz().toXmlString())
    }

    @Test
    fun jpzGeneration_withoutRowNumbers() = runTest {
        val puzzle = CASCADES.copy(includeRowNumbers = false).asPuzzle()
        val expected = readStringResource(CascadesTest::class, "cascades/cascades-without-rows.jpz")
        assertXmlEquals(expected, puzzle.asJpz().toXmlString())
    }

    @Test
    fun pdfGeneration() = runTest {
        assertContentEquals(
            readBinaryResource(CascadesTest::class, "cascades/cascades.pdf"),
            JpzFile(readBinaryResource(CascadesTest::class, "cascades/cascades.jpz"))
                .asPuzzle().asPdf(blackSquareLightnessAdjustment = 0.75, fontFamily = getNotoSerifFontFamily())
        )
    }

    companion object {
        val CASCADES = Cascades(
            title = "Test title",
            creator = "Test creator",
            copyright = "Test copyright",
            description = "Test description",
            grid = listOf(
                listOf("A", "B", "C", "D", "."),
                listOf("E", "F", "G", "H", "I"),
                listOf("J", "K", "L", "M", "N"),
                listOf(".", "O", "P", "Q", "R"),
            ),
            rowClues = listOf(
                listOf("Row clue 1A", "Row clue 1B"),
                listOf("Row clue 2A", "Row clue 2B"),
                listOf("Row clue 3A", "Row clue 3B"),
                listOf("Row clue 4A", "Row clue 4B"),
            ),
            cascadeClues = listOf(
                listOf("Cascade clue A"),
                listOf("Cascade clue B"),
                listOf("Cascade clue C"),
            ),
            includeRowNumbers = true,
            lightCascadeColor = "#FFFFFF",
            darkCascadeColor = "#C0C0C0",
        )
    }
}
