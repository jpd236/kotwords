package com.jeffpdavidson.kotwords.formats

import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

/** Helper methods for parsing XML files. */
internal object Xml {
    fun parseDocument(xml: String): Element {
        val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        return documentBuilder.parse(InputSource(StringReader(xml))).documentElement
    }

    fun Element.getElementListByTagName(tagName: String): List<Element> {
        return NodeStandardList(getElementsByTagName(tagName)).map { it as Element }
    }

    fun Element.getChildElementList(): List<Element> {
        return NodeStandardList(childNodes)
            .filter { it.nodeType == Node.ELEMENT_NODE }
            .map { it as Element }
    }

    fun Element.getElementByTagName(tagName: String): Element {
        return getElementsByTagName(tagName).item(0) as Element
    }
}

private class NodeStandardList(private val list: NodeList) : AbstractList<Node>(), RandomAccess {
    override fun get(index: Int): Node {
        return list.item(index)
    }

    override val size = list.length
}