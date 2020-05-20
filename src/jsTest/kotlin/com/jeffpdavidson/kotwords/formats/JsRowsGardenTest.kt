package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.model.CrosswordSolverSettings
import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readStringResource
import com.jeffpdavidson.kotwords.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class JsRowsGardenTest {
    @Test
    fun parseRgz() = runTest {
        val rgz = RowsGarden.parse(readBinaryResource(JsRowsGardenTest::class, "test.rgz"))
        val result = JsRowsGarden(rgz).asJpz(
                lightBloomColor = "#FFFFFF",
                mediumBloomColor = "#C3C8FA",
                darkBloomColor = "#5765F7",
                addWordCount = true,
                addHyphenated = true,
                crosswordSolverSettings = CrosswordSolverSettings("#00b100", "#80ff80", "All done!"))

        val expected = readStringResource(JsRowsGardenTest::class, "rows-garden.jpz")
        assertEquals(expected, result.asXmlString())
    }
}