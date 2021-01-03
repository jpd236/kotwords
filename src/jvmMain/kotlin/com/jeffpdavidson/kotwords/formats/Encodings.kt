package com.jeffpdavidson.kotwords.formats

import org.jsoup.parser.Parser
import java.net.URLDecoder
import java.util.Base64

internal actual object Encodings {
    actual fun decodeBase64(string: String): ByteArray = Base64.getDecoder().decode(string)

    actual fun decodeUrl(url: String): String = URLDecoder.decode(url, "UTF-8")

    actual fun decodeHtmlEntities(string: String): String =
        Parser.unescapeEntities(string, /* inAttribute= */ false)
}