package com.jeffpdavidson.kotwords.formats

import org.jsoup.parser.Parser
import java.net.URLDecoder
import java.util.Base64

internal actual object Encodings {
    private val UNESCAPE_PATTERN = "%u([0-9A-Fa-f]{4})|%([0-9A-Fa-f]{2})".toRegex()

    actual fun encodeBase64(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)
    actual fun decodeBase64(string: String): ByteArray = Base64.getDecoder().decode(string)

    actual fun decodeUrl(url: String): String = URLDecoder.decode(url, "UTF-8")

    actual fun decodeHtmlEntities(string: String): String =
        Parser.unescapeEntities(string, /* inAttribute= */ false)

    actual fun unescape(string: String): String {
        return UNESCAPE_PATTERN.replace(string) { result ->
            val code = result.groupValues[1].ifEmpty { result.groupValues[2] }
            code.toInt(16).toChar().toString()
        }
    }
}