package com.jeffpdavidson.kotwords.formats

import org.w3c.dom.asList
import org.w3c.dom.parsing.DOMParser

private class ElementImpl(private val jsElement: org.w3c.dom.Element) : Element {
    override val data: String
        get() = jsElement.innerHTML
    override val text: String
        get() = jsElement.textContent!!.replace(WHITESPACE_PATTERN, " ").trim()

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

internal actual object Html {
    actual fun parse(html: String, baseUri: String?): Element {
        val parser = DOMParser()
        val document = parser.parseFromString(html, "text/html")
        if (baseUri != null) {
            val baseElement = document.createElement("base")
            baseElement.setAttribute("href", baseUri)
            document.head?.appendChild(baseElement)
        }
        return ElementImpl(document.documentElement!!)
    }
}