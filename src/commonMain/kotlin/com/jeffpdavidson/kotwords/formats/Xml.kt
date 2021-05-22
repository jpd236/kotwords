package com.jeffpdavidson.kotwords.formats

internal interface Element {
    val data: String
    val text: String?

    fun attr(key: String): String
    fun select(selector: String): Iterable<Element>
    fun selectFirst(selector: String): Element?
}

internal enum class DocumentFormat {
    HTML,
    XML
}

internal expect object Xml {
    fun parse(html: String, baseUri: String? = null, format: DocumentFormat = DocumentFormat.XML): Element
}