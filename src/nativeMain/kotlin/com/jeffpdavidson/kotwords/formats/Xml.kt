package com.jeffpdavidson.kotwords.formats

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.parser.Parser

// TODO: Determine if this could reasonably replace the JS/JVM implementations as well.
private open class ElementImpl(private val ksoupElement: com.fleeksoft.ksoup.nodes.Element) : Element {
    override val tag: String = ksoupElement.tagName().uppercase()
    override val data: String = ksoupElement.data()
    override val text: String = ksoupElement.text()
    override val children: List<Node> = ksoupElement.childNodes().mapNotNull { node ->
        when (node) {
            is com.fleeksoft.ksoup.nodes.TextNode -> TextNode(node.text())
            is com.fleeksoft.ksoup.nodes.Element -> ElementImpl(node)
            else -> null
        }
    }

    override fun attr(key: String): String = ksoupElement.attr(key)

    override fun select(selector: String): Iterable<Element> =
        ksoupElement.select(selector).map { ElementImpl(it) }

    override fun selectFirst(selector: String): Element? {
        return ElementImpl(ksoupElement.selectFirst(selector) ?: return null)
    }
}

internal actual object Xml {
    actual fun parse(html: String, baseUri: String?, format: DocumentFormat): Element {
        val parser = when (format) {
            DocumentFormat.HTML -> Parser.htmlParser()
            DocumentFormat.XML -> Parser.xmlParser()
        }
        return ElementImpl(Ksoup.parse(html, baseUri ?: "", parser))
    }
}