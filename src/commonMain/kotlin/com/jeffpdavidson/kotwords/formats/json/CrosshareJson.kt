package com.jeffpdavidson.kotwords.formats.json

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal object CrosshareJson {
    @Serializable
    internal data class Size(val rows: Int, val cols: Int)

    @Serializable
    internal data class Clue(
        val dir: Int,
        val clue: String,
        val num: Int,
    )

    @Serializable
    internal data class HtmlTag(
        val type: String,
        val value: String? = null,
        val children: List<HtmlTag> = listOf(),
    ) {
        object HtmlTagSerializer : JsonTransformingSerializer<HtmlTag>(serializer()) {
            override fun transformDeserialize(element: JsonElement): JsonElement {
                return when (element) {
                    is JsonPrimitive -> buildJsonObject {
                        put("type", "text")
                        put("value", element)
                    }

                    else -> element
                }
            }
        }
    }

    @Serializable
    internal data class Puzzle(
        val authorName: String,
        val title: String,
        val size: Size,
        val clues: List<Clue>,
        val grid: List<String>,
        val highlighted: List<Int>,
        val guestConstructor: String? = null,
        @Serializable(with = HtmlTag.HtmlTagSerializer::class)
        val constructorNotes: HtmlTag? = null,
        // Note: this is speculative. Copyright seems to be stripped when uploading .puz files.
        val copyright: String? = null,
    )

    @Serializable
    internal data class PageProps(val puzzle: Puzzle)

    @Serializable
    internal data class Props(val pageProps: PageProps)

    @Serializable
    internal data class Data(val props: Props)
}