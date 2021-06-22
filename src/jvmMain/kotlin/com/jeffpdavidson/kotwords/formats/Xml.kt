package com.jeffpdavidson.kotwords.formats

import org.jsoup.Jsoup
import org.jsoup.parser.Parser

private open class ElementImpl(private val jsoupElement: org.jsoup.nodes.Element) : Element {
    override val tag: String = jsoupElement.tagName().toUpperCase()
    override val data: String = jsoupElement.data()
    override val text: String = jsoupElement.text()
    override val children: List<Node> = jsoupElement.childNodes().mapNotNull { node ->
            when (node) {
                is org.jsoup.nodes.TextNode -> TextNode(node.text())
                is org.jsoup.nodes.Element -> ElementImpl(node)
                else -> null
            }
        }

    override fun attr(key: String): String = jsoupElement.attr(key)

    override fun select(selector: String): Iterable<Element> =
        jsoupElement.select(selector).map { ElementImpl(it) }

    override fun selectFirst(selector: String): Element? {
        return ElementImpl(jsoupElement.selectFirst(selector) ?: return null)
    }
}

internal actual object Xml {
    actual fun parse(html: String, baseUri: String?, format: DocumentFormat): Element {
        val parser = when (format) {
            DocumentFormat.HTML -> Parser.htmlParser()
            DocumentFormat.XML -> Parser.xmlParser()
        }
        return ElementImpl(Jsoup.parse(html, baseUri ?: "", parser))
    }
}