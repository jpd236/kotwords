package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.IgnoreNative
import com.jeffpdavidson.kotwords.formats.CrosswordCompilerApplet
import com.jeffpdavidson.kotwords.formats.ImageComparator
import com.jeffpdavidson.kotwords.formats.ImageComparator.assertPdfEquals
import com.jeffpdavidson.kotwords.formats.JpzFile
import com.jeffpdavidson.kotwords.formats.PdfTest
import com.jeffpdavidson.kotwords.formats.getNotoSerifFontFamily
import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readStringResource
import io.github.pdvrieze.xmlutil.testutil.assertXmlEquals
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class MarchingBandsTest {
    @Test
    fun jpzGeneration() = runTest {
        val puzzle = MARCHING_BANDS.asPuzzle()
        val expected = readStringResource(MarchingBandsTest::class, "marching-bands/marching-bands.jpz")
        assertXmlEquals(
            expected, puzzle.asJpz(
                appletSettings = CrosswordCompilerApplet.AppletSettings(
                    cursorColor = "#00b100",
                    selectedCellsColor = "#80ff80",
                    completion = CrosswordCompilerApplet.AppletSettings.Completion(message = "All done!"),
                )
            ).toXmlString()
        )
    }

    @Test
    fun jpzGeneration_withoutRowNumbers() = runTest {
        val puzzle = MARCHING_BANDS.copy(includeRowNumbers = false).asPuzzle()
        val expected = readStringResource(MarchingBandsTest::class, "marching-bands/marching-bands-without-rows.jpz")
        assertXmlEquals(
            expected, puzzle.asJpz(
                appletSettings = CrosswordCompilerApplet.AppletSettings(
                    cursorColor = "#00b100",
                    selectedCellsColor = "#80ff80",
                    completion = CrosswordCompilerApplet.AppletSettings.Completion(message = "All done!"),
                )
            ).toXmlString()
        )
    }

    @Test
    @IgnoreNative  // Depends on PDF support
    fun pdfGeneration() = runTest {
        assertPdfEquals(
            readBinaryResource(ImageComparator::class, "marching-bands/marching-bands.pdf"),
            JpzFile(readBinaryResource(PdfTest::class, "marching-bands/marching-bands.jpz"))
                .asPuzzle().asPdf(blackSquareLightnessAdjustment = 0.75f, fontFamily = getNotoSerifFontFamily())
        )
    }

    companion object {
        private val MARCHING_BANDS = MarchingBands(
            title = "Test title",
            creator = "Test creator",
            copyright = "Test copyright",
            description = "Test description",
            grid = listOf(
                listOf('A', 'B', 'C', 'D', 'E'),
                listOf('F', 'G', 'H', 'I', 'J'),
                listOf('K', 'L', null, 'M', 'N'),
                listOf('O', 'P', 'Q', 'R', 'S'),
                listOf('T', 'U', 'V', 'W', 'X')
            ),
            bandClues = listOf(
                listOf("Band A1", "Band A2", "Band A3"),
                listOf("Band B1", "Band B2")
            ),
            rowClues = listOf(
                listOf("Row 1A", "Row 1B"),
                listOf("Row 2A", "Row 2B"),
                listOf("Row 3A", "Row 3B"),
                listOf("Row 4A", "Row 4B"),
                listOf("Row 5A", "Row 5B")
            ),
            includeRowNumbers = true,
            lightBandColor = "#FFFFFF",
            darkBandColor = "#C0C0C0",
        )
    }
}