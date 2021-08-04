package com.jeffpdavidson.kotwords.formats

import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLTextAreaElement

internal external fun decodeURIComponent(encodedURI: String): String

internal actual object Encodings {
    private const val BASE64_DICTIONARY = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

    // Based off:
    // https://github.com/jershell/kbase64/blob/master/src/commonMain/kotlin/com/github/jershell/kbase64/encodeBase64.kt
    actual fun encodeBase64(bytes: ByteArray): String {
        val output = StringBuilder()
        var padding = 0
        var position = 0
        while (position < bytes.size) {
            var b = bytes[position].toInt() and 0xFF shl 16 and 0xFFFFFF
            if (position + 1 < bytes.size) b = b or (bytes[position + 1].toInt() and 0xFF shl 8) else padding++
            if (position + 2 < bytes.size) b = b or (bytes[position + 2].toInt() and 0xFF) else padding++
            for (i in 0 until 4 - padding) {
                val c = b and 0xFC0000 shr 18
                output.append(BASE64_DICTIONARY[c])
                b = b shl 6
            }
            position += 3
        }
        for (i in 0 until padding) {
            output.append('=')
        }
        return output.toString()
    }

    actual fun decodeBase64(string: String): ByteArray =
        window.atob(string).map { it.code.toByte() }.toByteArray()

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