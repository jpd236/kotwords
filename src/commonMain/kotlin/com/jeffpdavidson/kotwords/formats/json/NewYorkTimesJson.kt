package com.jeffpdavidson.kotwords.formats.json

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive

internal object NewYorkTimesJson {
    @Serializable
    data class ClueList(
        val clues: List<Int>,
        val name: String,
    )

    @Serializable
    data class Platforms(
        val web: Boolean,
    )

    @Serializable
    data class Note(
        val text: String,
        val platforms: Platforms,
    )

    @Serializable
    data class Meta(
        val constructors: List<String>,
        val copyright: String,
        val editor: String = "",
        val publicationDate: String,
        val notes: List<Note>? = listOf(),
        val title: String = "",
        val publishStream: String = "",
    )

    @Serializable
    data class Dimensions(
        val rowCount: Int,
        val columnCount: Int,
    )

    @Serializable(with = UrlValue.UrlValueSerializer::class)
    sealed class UrlValue {
        @Serializable(with = StringValue.StringValueSerializer::class)
        data class StringValue(val value: String) : UrlValue() {
            object StringValueSerializer : KSerializer<StringValue> {
                override val descriptor: SerialDescriptor = buildClassSerialDescriptor("StringValue")

                override fun deserialize(decoder: Decoder): StringValue {
                    require(decoder is JsonDecoder)
                    val element = decoder.decodeJsonElement()
                    return StringValue(element.jsonPrimitive.content)
                }

                override fun serialize(encoder: Encoder, value: StringValue) = throw UnsupportedOperationException()
            }
        }

        @Serializable(with = BooleanValue.BooleanValueSerializer::class)
        data class BooleanValue(val boolean: Boolean) : UrlValue() {
            object BooleanValueSerializer : KSerializer<BooleanValue> {
                override val descriptor: SerialDescriptor = buildClassSerialDescriptor("BooleanValue")

                override fun deserialize(decoder: Decoder): BooleanValue {
                    require(decoder is JsonDecoder)
                    val element = decoder.decodeJsonElement()
                    return BooleanValue(element.jsonPrimitive.boolean)
                }

                override fun serialize(encoder: Encoder, value: BooleanValue) = throw UnsupportedOperationException()
            }
        }

        object UrlValueSerializer : JsonContentPolymorphicSerializer<UrlValue>(UrlValue::class) {
            override fun selectDeserializer(element: JsonElement): KSerializer<out UrlValue> {
                require(element is JsonPrimitive)
                return if (element.isString) {
                    StringValue.serializer()
                } else {
                    BooleanValue.serializer()
                }
            }
        }
    }

    @Serializable
    data class Overlays(
        val beforeStart: UrlValue = UrlValue.BooleanValue(false),
    )

    @Serializable
    data class MoreAnswers(
        val valid: List<String> = listOf()
    )

    @Serializable
    data class Cell(
        /** Cell type. 1 == regular, 2 == circled, 3 == shaded, 4 == external (void), 0/default == block. */
        val type: Int,
        val answer: String = "",
        val label: String = "",
        val moreAnswers: MoreAnswers = MoreAnswers(),
    )

    @Serializable
    data class Clue(
        val cells: List<Int>,
        val label: String,
        val text: String,
    )

    @Serializable
    data class XmlElement(
        val name: String = "",
        val attributes: Map<String, String> = mapOf(),
        val children: List<XmlElement> = listOf(),
    )

    @Serializable
    data class GamePageData(
        val clueLists: List<ClueList>,
        val meta: Meta,
        val dimensions: Dimensions,
        val cells: List<Cell>,
        val clues: List<Clue>,
        val overlays: Overlays = Overlays(),
        val board: XmlElement = XmlElement(),
    )

    @Serializable
    data class Data(val gamePageData: GamePageData)
}