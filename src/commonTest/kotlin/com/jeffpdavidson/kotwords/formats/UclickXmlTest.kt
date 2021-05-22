package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.AcrossLite.Companion.toAcrossLiteBinary
import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readStringResource
import com.jeffpdavidson.kotwords.runTest
import com.soywiz.klock.Date
import kotlin.test.Test
import kotlin.test.assertTrue

class UclickXmlTest {
    @Test
    fun crossword() = runTest {
        assertTrue(
            readBinaryResource(UclickXmlTest::class, "puz/test-simple.puz").contentEquals(
                UclickXml(
                    readStringResource(UclickXmlTest::class, "uclick/test-simple.xml"),
                    date = Date.invoke(2018, 1, 1),
                    addDateToTitle = false
                ).asCrossword().toAcrossLiteBinary()
            )
        )
    }
}
