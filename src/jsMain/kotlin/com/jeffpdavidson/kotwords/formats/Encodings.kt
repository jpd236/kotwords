package com.jeffpdavidson.kotwords.formats

import kotlinx.browser.document
import org.w3c.dom.HTMLTextAreaElement

internal external fun decodeURIComponent(encodedURI: String): String

internal actual object Encodings {

    actual fun decodeCp1252(bytes: ByteArray): String {
        return commonDecodeCp1252(bytes)
    }

    actual fun encodeCp1252(string: String): ByteArray {
        return commonEncodeCp1252(string)
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