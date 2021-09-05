package com.jeffpdavidson.kotwords.formats

internal expect object Encodings {
    /** Decode the given Cp1252 data as a string. */
    fun decodeCp1252(bytes: ByteArray): String
    /** Encode the given string as Cp1252 bytes. Unsupported characters are replaced with '?'. */
    fun encodeCp1252(string: String): ByteArray

    fun decodeUrl(url: String): String

    fun decodeHtmlEntities(string: String): String

    fun unescape(string: String): String
}