package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.AcrossLite.Companion.toAcrossLiteBinary
import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readUtf8Resource
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class UclickXmlTest {
    @Test
    fun crossword() {
        Assertions.assertArrayEquals(
                UclickXmlTest::class.readBinaryResource("puz/test-simple.puz"),
                UclickXml(UclickXmlTest::class.readUtf8Resource("uclick/test-simple.xml"),
                        date = LocalDate.of(2018, 1, 1),
                        addDateToTitle = false)
                        .asCrossword().toAcrossLiteBinary())
    }
}
