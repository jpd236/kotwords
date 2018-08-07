package com.jeffpdavidson.kotwords.formats.json

import com.squareup.moshi.Json
import se.ansman.kotshi.JsonDefaultValueString
import se.ansman.kotshi.JsonSerializable

internal object UclickJson {
    @JsonSerializable
    internal data class Response(
            @Json(name = "AllAnswer") val allAnswer: String,
            @Json(name = "Width") val width: Int,
            @Json(name = "Height") val height: Int,
            @Json(name = "Title") val title: String,
            @Json(name = "Author") val author: String,
            @Json(name = "Date") val date: String,
            @Json(name = "AcrossClue") val acrossClue: String,
            @Json(name = "DownClue") val downClue: String,
            @Json(name = "Copyright") @JsonDefaultValueString(value = "") val copyright: String)
}