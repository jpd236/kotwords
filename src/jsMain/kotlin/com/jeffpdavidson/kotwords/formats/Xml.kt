package com.jeffpdavidson.kotwords.formats

import org.w3c.dom.Text
import org.w3c.dom.asList
import org.w3c.dom.parsing.DOMParser

private class ElementImpl(private val jsElement: org.w3c.dom.Element) : Element {
    override val tag: String = jsElement.tagName
    override val data: String = jsElement.innerHTML
    override val text: String = jsElement.textContent!!.replace(WHITESPACE_PATTERN, " ").trim()
    override val children: List<Node> = jsElement.childNodes.asList().mapNotNull {
        when (it) {
            is Text -> TextNode(it.textContent!!)
            is org.w3c.dom.Element -> ElementImpl(it)
            else -> null
        }
    }

    override fun attr(key: String): String = jsElement.getAttribute(key) ?: ""

    override fun select(selector: String): Iterable<Element> =
        jsElement.querySelectorAll(selector).asList().map { ElementImpl(it as org.w3c.dom.Element) }

    override fun selectFirst(selector: String): Element? {
        return ElementImpl(jsElement.querySelector(selector) ?: return null)
    }

    companion object {
        private val WHITESPACE_PATTERN = "[\t\r\n ]+".toRegex()
    }
}

internal actual object Xml {
    actual fun parse(html: String, baseUri: String?, format: DocumentFormat): Element {
        val parser = DOMParser()
        val type = when (format) {
            DocumentFormat.HTML -> "text/html"
            DocumentFormat.XML -> "application/xml"
        }
        val document = parser.parseFromString(html, type)
        if (baseUri != null) {
            val baseElement = document.createElement("base")
            baseElement.setAttribute("href", baseUri)
            document.head?.appendChild(baseElement)
        }
        return ElementImpl(document.documentElement!!)
    }
}