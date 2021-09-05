package com.jeffpdavidson.kotwords.formats

import kotlinx.browser.document
import org.w3c.dom.HTMLTextAreaElement

internal external fun decodeURIComponent(encodedURI: String): String

internal actual object Encodings {
    /** Unicode values corresponding to bytes 0x80-0x9F in CP1252. */
    private val CP1252_DECODE_TABLE = listOf(
        '\u20ac', '\u0081', '\u201a', '\u0192', '\u201e', '\u2026', '\u2020', '\u2021',
        '\u02c6', '\u2030', '\u0160', '\u2039', '\u0152', '\u008d', '\u017d', '\u008f',
        '\u0090', '\u2018', '\u2019', '\u201c', '\u201d', '\u2022', '\u2013', '\u2014',
        '\u02dc', '\u2122', '\u0161', '\u203a', '\u0153', '\u009d', '\u017e', '\u0178',
    )

    /** Map from supported CP1252 unicode values to their byte value in CP1252. */
    private val CP1252_ENCODE_TABLE = CP1252_DECODE_TABLE.withIndex().associate { it.value to it.index }

    actual fun decodeCp1252(bytes: ByteArray): String {
        return bytes.map { byte ->
            val intValue = byte.toInt() and 0xFF
            if (intValue in 0x00..0x7F || intValue in 0xA0..0xFF) {
                // ISO-8859-1 characters map directly.
                intValue.toChar()
            } else {
                // Between 0x80 and 0x9F - look up the value in the table.
                CP1252_DECODE_TABLE[intValue - 0x80]
            }
        }.joinToString("")
    }

    actual fun encodeCp1252(string: String): ByteArray {
        return string.map { ch ->
            if (ch.code in 0x00..0x7F || ch.code in 0xA0..0xFF) {
                // ISO-8859-1 characters map directly.
                ch.code.toByte()
            } else {
                val cp1252Byte = CP1252_ENCODE_TABLE[ch]
                if (cp1252Byte != null) {
                    // One of the supported CP1252 characters.
                    (0x80 + cp1252Byte).toByte()
                } else {
                    // Unsupported value - use default '?' character.
                    '?'.code.toByte()
                }
            }
        }.toByteArray()
    }

    actual fun decodeUrl(url: String): String = decodeURIComponent(url)

    actual fun decodeHtmlEntities(string: String): String {
        val textarea = document.createElement("textarea") as HTMLTextAreaElement
        textarea.innerHTML = string
        return textarea.value
    }

    actual fun unescape(string: String): String = jsunescape(string)
}

@JsName("unescape")
private external fun jsunescape(string: String): String