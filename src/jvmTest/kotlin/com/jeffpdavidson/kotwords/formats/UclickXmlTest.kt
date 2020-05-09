package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.AcrossLite.Companion.toAcrossLiteBinary
import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readStringResource
import com.jeffpdavidson.kotwords.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import java.time.LocalDate

class UclickXmlTest {
    @Test
    fun crossword() = runTest {
        assertArrayEquals(
                readBinaryResource(UclickXmlTest::class, "puz/test-simple.puz"),
                UclickXml(readStringResource(UclickXmlTest::class, "uclick/test-simple.xml"),
                        date = LocalDate.of(2018, 1, 1),
                        addDateToTitle = false)
                        .asCrossword().toAcrossLiteBinary())
    }
}
