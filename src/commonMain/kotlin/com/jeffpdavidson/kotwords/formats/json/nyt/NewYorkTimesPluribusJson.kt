package com.jeffpdavidson.kotwords.formats.json.nyt

import com.jeffpdavidson.kotwords.formats.json.JsonSerializer
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

/** JSON for NewYorkTimes puzzles, as stored in the encoded "pluribus" variable embedded in HTML. */
internal object NewYorkTimesPluribusJson {

    @Serializable
    data class Meta(
        val constructors: List<String>,
        val copyright: String,
        val editor: String = "",
        val publicationDate: String,
        val notes: List<NewYorkTimesJson.Note>? = listOf(),
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
    data class GamePageData(
        val clueLists: List<NewYorkTimesJson.ClueList>,
        val meta: Meta,
        val dimensions: Dimensions,
        val cells: List<NewYorkTimesJson.Cell>,
        val clues: List<NewYorkTimesJson.Clue>,
        val overlays: Overlays = Overlays(),
        val board: NewYorkTimesJson.XmlElement = NewYorkTimesJson.XmlElement(),
    )

    @Serializable
    data class Data(val gamePageData: GamePageData)

    fun parse(json: String): NewYorkTimesJson {
        val data = JsonSerializer.fromJson<Data>(json).gamePageData
        return object : NewYorkTimesJson {
            override val publicationDate: String = data.meta.publicationDate
            override val publishStream: String = data.meta.publishStream
            override val height: Int = data.dimensions.rowCount
            override val width: Int = data.dimensions.columnCount
            override val cells: List<NewYorkTimesJson.Cell> = data.cells
            override val notes: List<NewYorkTimesJson.Note>? = data.meta.notes
            override val title: String = data.meta.title
            override val constructors: List<String> = data.meta.constructors
            override val editor: String = data.meta.editor
            override val copyright: String = data.meta.copyright
            override val clueLists: List<NewYorkTimesJson.ClueList> = data.clueLists
            override val clues: List<NewYorkTimesJson.Clue> = data.clues
            override val board: NewYorkTimesJson.XmlElement = data.board

            override val beforeStartOverlay: String? = if (data.overlays.beforeStart is UrlValue.StringValue) {
                data.overlays.beforeStart.value.ifEmpty { null }
            } else {
                null
            }
        }
    }
}