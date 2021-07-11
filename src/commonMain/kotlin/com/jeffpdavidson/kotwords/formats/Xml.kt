package com.jeffpdavidson.kotwords.formats

internal interface Element : Node {
    val tag: String
    val data: String
    val text: String?
    val children: List<Node>

    fun attr(key: String): String
    fun select(selector: String): Iterable<Element>
    fun selectFirst(selector: String): Element?
}

data class TextNode(val text: String) : Node

internal interface Node

internal enum class DocumentFormat {
    HTML,
    XML
}

internal expect object Xml {
    fun parse(html: String, baseUri: String? = null, format: DocumentFormat = DocumentFormat.XML): Element
}