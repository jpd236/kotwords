package com.jeffpdavidson.kotwords.formats.json

import com.squareup.moshi.Json
import se.ansman.kotshi.JsonDefaultValue
import se.ansman.kotshi.JsonSerializable

internal object WallStreetJournalJson {
    @JsonSerializable
    internal data class Gridsize(val cols: Int, val rows: Int)

    @JsonSerializable
    internal data class Clue(val number: Int, val clue: String)

    @JsonSerializable
    internal data class ClueSet(val title: String, val clues: List<Clue>)

    @JsonSerializable
    internal data class Copy(
            val title: String,
            val byline: String,
            val description: String,
            @Json(name = "date-publish") val datePublish: String,
            val publisher: String,
            val gridsize: Gridsize,
            val clues: List<ClueSet>)

    @JsonSerializable
    internal data class Style(val shapebg: String, val highlight: Boolean)

    @JsonDefaultValue
    val defaultStyle = Style(shapebg = "", highlight = false)

    @JsonSerializable
    internal data class Square(
            @Json(name = "Letter") val letter: String,
            @JsonDefaultValue val style: Style)

    @JsonSerializable
    internal data class Data(val copy: Copy, val grid: List<List<Square>>)

    @JsonSerializable
    internal data class Response(val data: Data)
}