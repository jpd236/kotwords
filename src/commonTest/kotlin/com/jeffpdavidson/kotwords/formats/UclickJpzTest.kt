package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.readBinaryResource
import com.jeffpdavidson.kotwords.readStringResource
import korlibs.time.Date
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
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

    @Test
    fun decode_normalClue() {
        assertEquals("Normal clue", UclickJpz.decodeClue("Normal clue"))
    }

    @Test
    fun decode_clueWithUrlEscaping() {
        assertEquals("URL escaped clue", UclickJpz.decodeClue("URL%20escaped%20clue"))
    }

    @Test
    fun decode_clueWithInvalidUrlEscaping() {
        assertEquals("Clue with % sign", UclickJpz.decodeClue("Clue with % sign"))
    }

    @Test
    fun decode_clueWithAnnotation() {
        assertEquals("Clue with annotation", UclickJpz.decodeClue("Clue with annotation @@ Annotation"))
    }

    @Test
    fun decode_clueWithAlternate() {
        assertEquals("Clue with alternate", UclickJpz.decodeClue("Clue with alternate || Alternate"))
    }
}