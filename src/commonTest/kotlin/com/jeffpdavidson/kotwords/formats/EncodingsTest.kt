package com.jeffpdavidson.kotwords.formats

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EncodingsTest {
    @Test
    fun cp1252_asciiChars() {
        checkConversion("ABCDE12345", byteArrayOf(0x41, 0x42, 0x43, 0x44, 0x45, 0x31, 0x32, 0x33, 0x34, 0x35))
    }

    @Test
    fun cp1252_iso88591Chars() {
        checkConversion("©®Ýÿ", byteArrayOf(0xA9.toByte(), 0xAE.toByte(), 0xDD.toByte(), 0xFF.toByte()))
    }

    @Test
    fun cp1252_cp1252Chars() {
        checkConversion("€‡Ÿ™", byteArrayOf(0x80.toByte(), 0x87.toByte(), 0x9F.toByte(), 0x99.toByte()))
    }

    @Test
    fun encodeCp1252_unsupportedChars() {
        assertTrue(Encodings.encodeCp1252("ΑΒΓΔΕ").contentEquals(byteArrayOf(0x3F, 0x3F, 0x3F, 0x3F, 0x3F)))
    }

    private fun checkConversion(string: String, bytes: ByteArray) {
        assertTrue(Encodings.encodeCp1252(string).contentEquals(bytes))
        assertEquals(string, Encodings.decodeCp1252(bytes))
    }
}