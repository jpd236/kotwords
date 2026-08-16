package com.jeffpdavidson.kotwords.formats.json

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
internal object UclickJson {
    @Serializable
    internal data class Response(
        @JsonNames("AllAnswer") val allAnswer: String = "",
        val solution: List<String> = listOf(),
        @JsonNames("Width") val width: Int,
        @JsonNames("Height") val height: Int,
        @JsonNames("Title") val title: String,
        @JsonNames("Author") val author: String,
        @JsonNames("Date") val date: String,
        @JsonNames("AcrossClue") val acrossClue: String,
        @JsonNames("DownClue") val downClue: String,
        @JsonNames("Copyright") val copyright: String = "",
    )

    @Serializable
    internal data class Data(
        val gameData: Response,
    )

    @Serializable
    internal data class UsaTodayResponse(
        val data: Data,
    )
}