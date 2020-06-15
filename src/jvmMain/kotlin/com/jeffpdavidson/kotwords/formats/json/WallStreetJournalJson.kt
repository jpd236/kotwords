package com.jeffpdavidson.kotwords.formats.json

import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonQualifier
import com.squareup.moshi.ToJson
import org.jsoup.parser.Parser
import se.ansman.kotshi.JsonDefaultValue
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.PrimitiveAdapters

internal object WallStreetJournalJson {
    @JsonSerializable
    internal data class Gridsize(val cols: Int, val rows: Int)

    @JsonSerializable(PrimitiveAdapters.ENABLED)
    internal data class Clue(val number: Int, @HtmlString val clue: String)

    @JsonSerializable(PrimitiveAdapters.ENABLED)
    internal data class ClueSet(@HtmlString val title: String, val clues: List<Clue>)

    @JsonSerializable(PrimitiveAdapters.ENABLED)
    internal data class Copy(
            @HtmlString val title: String,
            @HtmlString val byline: String,
            @HtmlString val description: String,
            @Json(name = "date-publish") @HtmlString val datePublish: String,
            @HtmlString val publisher: String,
            val gridsize: Gridsize,
            val clues: List<ClueSet>
    )

    @JsonSerializable
    internal data class Style(val shapebg: String, val highlight: Boolean)

    @JsonDefaultValue
    val defaultStyle = Style(shapebg = "", highlight = false)

    @JsonSerializable
    internal data class Square(
            @Json(name = "Letter") val letter: String,
            @JsonDefaultValue val style: Style
    )

    @JsonSerializable
    internal data class Data(val copy: Copy, val grid: List<List<Square>>)

    @JsonSerializable
    internal data class Response(val data: Data)

    @Retention(AnnotationRetention.RUNTIME)
    @JsonQualifier
    internal annotation class HtmlString

    internal class HtmlStringAdapter {
        @FromJson
        @HtmlString
        fun fromJson(data: String): String {
            return Parser.unescapeEntities(data, /* inAttribute= */ false)
        }

        @ToJson
        fun toJson(@Suppress("UNUSED_PARAMETER") @HtmlString data: String): String {
            throw UnsupportedOperationException("Serialization is not supported")
        }
    }
}