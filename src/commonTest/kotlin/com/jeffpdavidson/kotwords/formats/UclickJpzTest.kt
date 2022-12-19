package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.AcrossLite.Companion.asAcrossLiteBinary
import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readStringResource
import com.soywiz.klock.Date
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class UclickJpzTest {
    @Test
    fun crossword() = runTest {
        assertTrue(
            readBinaryResource(UclickJpzTest::class, "puz/test.puz").contentEquals(
                UclickJpz(
                    readStringResource(UclickJpzTest::class, "uclick/test.jpz"),
                    date = Date.invoke(2018, 1, 1),
                    addDateToTitle = false
                ).asPuzzle().asAcrossLiteBinary()
            )
        )
    }
}