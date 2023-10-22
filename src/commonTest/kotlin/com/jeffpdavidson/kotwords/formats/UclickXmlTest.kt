package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readStringResource
import korlibs.time.Date
import kotlinx.coroutines.test.runTest
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
                ).asPuzzle().asAcrossLiteBinary()
            )
        )
    }
}
