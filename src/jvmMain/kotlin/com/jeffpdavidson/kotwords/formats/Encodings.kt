package com.jeffpdavidson.kotwords.formats

import org.jsoup.parser.Parser
import java.net.URLDecoder
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.charset.UnsupportedCharsetException

internal actual object Encodings {
    private val WINDOWS_1252_CHARSET = try {
        Charset.forName("windows-1252")
    } catch (e: UnsupportedCharsetException) {
        // Unlikely to happen, but fall back to ISO-8859-1 just in case.
        StandardCharsets.ISO_8859_1
    }

    actual fun decodeCp1252(bytes: ByteArray): String {
        return String(bytes, WINDOWS_1252_CHARSET)
    }

    actual fun encodeCp1252(string: String): ByteArray {
        return string.toByteArray(WINDOWS_1252_CHARSET)
    }

    actual fun decodeUrl(url: String): String = URLDecoder.decode(url, "UTF-8")

    actual fun decodeHtmlEntities(string: String): String =
        Parser.unescapeEntities(string, /* inAttribute= */ false)

    actual fun unescape(string: String): String {
        return commonUnescape(string)
    }
}