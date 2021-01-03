package com.jeffpdavidson.kotwords.formats

import org.jsoup.Jsoup

private open class ElementImpl(private val jsoupElement: org.jsoup.nodes.Element) : Element {
    override val data: String
        get() = jsoupElement.data()
    override val text: String
        get() = jsoupElement.text()

    override fun attr(key: String): String = jsoupElement.attr(key)

    override fun select(selector: String): Iterable<Element> =
        jsoupElement.select(selector).map { ElementImpl(it) }

    override fun selectFirst(selector: String): Element? {
        return ElementImpl(jsoupElement.selectFirst(selector) ?: return null)
    }
}

internal actual object Html {
    actual fun parse(html: String, baseUri: String?): Element =
        ElementImpl(Jsoup.parse(html, baseUri ?: ""))
}