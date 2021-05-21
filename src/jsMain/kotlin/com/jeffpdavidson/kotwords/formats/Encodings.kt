package com.jeffpdavidson.kotwords.formats

import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLTextAreaElement

internal external fun decodeURIComponent(encodedURI: String): String

internal actual object Encodings {
    actual fun decodeBase64(string: String): ByteArray =
        window.atob(string).map { it.toByte() }.toByteArray()

    actual fun decodeUrl(url: String): String = decodeURIComponent(url)

    actual fun decodeHtmlEntities(string: String): String {
        val textarea = document.createElement("textarea") as HTMLTextAreaElement
        textarea.innerHTML = string
        return textarea.value
    }
}