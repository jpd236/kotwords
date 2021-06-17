package com.jeffpdavidson.kotwords.formats

internal expect object Encodings {
    fun encodeBase64(bytes: ByteArray): String
    fun decodeBase64(string: String): ByteArray

    fun decodeUrl(url: String): String

    fun decodeHtmlEntities(string: String): String
}