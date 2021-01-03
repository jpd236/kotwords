package com.jeffpdavidson.kotwords.formats

internal expect object Encodings {
    fun decodeBase64(string: String): ByteArray

    fun decodeUrl(url: String): String

    fun decodeHtmlEntities(string: String): String
}