package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.Jpz.Companion.asJpzFile
import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readStringResource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class RowsGardenTest {

    @Test
    fun convertToJpz() = runTest {
        val puzzle = Rgz.fromRgzFile(readBinaryResource(RowsGardenTest::class, "rows-garden/test.rgz")).asRowsGarden(
            lightBloomColor = "#FFFFFF",
            mediumBloomColor = "#C3C8FA",
            darkBloomColor = "#5765F7",
            addWordCount = true,
            addHyphenated = true
        ).asPuzzle()

        val expected = readStringResource(RowsGardenTest::class, "rows-garden/rows-garden.jpz")
        assertEquals(
            expected, puzzle.asJpzFile(
                appletSettings = CrosswordCompilerApplet.AppletSettings(
                    cursorColor = "#00b100",
                    selectedCellsColor = "#80ff80",
                    completion = CrosswordCompilerApplet.AppletSettings.Completion(message = "All done!")
                )
            ).toXmlString()
        )
    }

    @Test
    fun convertToJpz_mini() = runTest {
        val puzzle = Rgz.fromRgzFile(readBinaryResource(RowsGardenTest::class, "rows-garden/test-mini.rg"))
            .asRowsGarden(
                lightBloomColor = "#FFFFFF",
                mediumBloomColor = "#C3C8FA",
                darkBloomColor = "#5765F7",
                addWordCount = true,
                addHyphenated = true,
            ).asPuzzle()

        val expected = readStringResource(RowsGardenTest::class, "rows-garden/rows-garden-mini.jpz")
        assertEquals(
            expected, puzzle.asJpzFile(
                appletSettings = CrosswordCompilerApplet.AppletSettings(
                    cursorColor = "#00b100",
                    selectedCellsColor = "#80ff80",
                    completion = CrosswordCompilerApplet.AppletSettings.Completion(message = "All done!")
                )
            ).toXmlString()
        )
    }
}