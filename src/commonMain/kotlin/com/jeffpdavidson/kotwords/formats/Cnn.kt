package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.json.CnnJson
import com.jeffpdavidson.kotwords.formats.json.JsonSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.XmlWriter
import nl.adaptivity.xmlutil.core.impl.multiplatform.StringWriter
import nl.adaptivity.xmlutil.newGenericWriter
import nl.adaptivity.xmlutil.xmlStreaming

/** Container for a puzzle in the CNN JSON format. */
class Cnn(private val json: String) : DelegatingPuzzleable() {
    override suspend fun getPuzzleable(): Puzzleable {
        // CNN JSON consists of:
        // - A "data" object which is a JPZ <rectangular-puzzle> tag systemically converted to JSON.
        // - A "metaData" object with some additional metadata.
        // We convert the JSON back to XML, wrapping in the necessary root tag, and then parse as a regular JPZ.
        val cnnJson = JsonSerializer.fromJson<CnnJson.Data>(json)
        val jpzXml = StringBuilder()
        val xmlWriter = xmlStreaming.newGenericWriter(jpzXml, xmlDeclMode = XmlDeclMode.Charset)
        xmlWriter.startDocument(version = "1.0", encoding = "UTF-8")
        xmlWriter.startTag(namespace = Jpz.CC_NS, localName = "crossword-compiler", prefix = "")
        xmlWriter.writeJsonObject("rectangular-puzzle", cnnJson.data)
        xmlWriter.endTag(namespace = Jpz.CC_NS, localName = "crossword-compiler", prefix = "")
        xmlWriter.endDocument()
        xmlWriter.flush()
        val jpz = Jpz.fromXmlString(jpzXml.toString(), stripFormats = true)
        return CrosswordCompilerApplet(
            appletSettings = CrosswordCompilerApplet.AppletSettings(
                title = Jpz.Html(
                    data = Jpz.htmlToSnippet(
                        listOfNotNull(
                            cnnJson.metaData.title?.ifEmpty { null },
                            cnnJson.metaData.date?.ifEmpty { null },
                        ).joinToString(" - ")
                    )
                )
            ),
            rectangularPuzzle = jpz.rectangularPuzzle,
        )
    }

    private fun XmlWriter.writeJsonObject(tagName: String, jsonObject: JsonObject) {
        startTag(namespace = null, localName = tagName, prefix = "")
        jsonObject.get("$")?.let { attributes ->
            attributes.jsonObject.forEach { (name, value) ->
                attribute(namespace = null, name = name, prefix = "", value = value.jsonPrimitive.content)
            }
        }
        jsonObject.get("_")?.let { text ->
            text(text.jsonPrimitive.content)
        }
        jsonObject.filterKeys { it != "$" && it != "_" }.forEach { (name, value) ->
            when (value) {
                is JsonObject -> writeJsonObject(name, value)
                is JsonArray -> {
                    value.forEach {
                        when (it) {
                            is JsonObject -> writeJsonObject(name, it.jsonObject)
                            is JsonPrimitive -> {
                                startTag(namespace = null, localName = name, prefix = "")
                                text(it.content)
                                endTag(namespace = null, localName = name, prefix = "")
                            }

                            else -> InvalidFormatException("Unsupported JSON array content: $it")
                        }
                    }
                }

                else -> InvalidFormatException("Unsupported JSON: $value")
            }
        }
        endTag(namespace = null, localName = tagName, prefix = "")
    }
}