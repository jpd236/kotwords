package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readUtf8Resource
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class UclickTest {
    @Test
    fun crossword() {
        Assertions.assertArrayEquals(
                UclickTest::class.readBinaryResource("puz/test-simple.puz"),
                Uclick(UclickTest::class.readUtf8Resource("uclick/test-simple.json"),
                        copyright = "Jeff Davidson",
                        addDateToTitle = false).asCrossword().toAcrossLiteBinary())
    }
}