package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.AcrossLite.Companion.toAcrossLiteBinary
import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readUtf8Resource
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class UclickJsonTest {
    @Test
    fun crossword() {
        Assertions.assertArrayEquals(
                UclickJsonTest::class.readBinaryResource("puz/test-simple.puz"),
                UclickJson(UclickJsonTest::class.readUtf8Resource("uclick/test-simple.json"),
                        copyright = "Jeff Davidson",
                        addDateToTitle = false).asCrossword().toAcrossLiteBinary())
    }
}