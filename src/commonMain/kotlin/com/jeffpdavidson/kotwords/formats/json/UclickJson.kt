package com.jeffpdavidson.kotwords.formats.json

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal object UclickJson {
    @Serializable
    internal data class Response(
        @SerialName("AllAnswer") val allAnswer: String,
        @SerialName("Width") val width: Int,
        @SerialName("Height") val height: Int,
        @SerialName("Title") val title: String,
        @SerialName("Author") val author: String,
        @SerialName("Date") val date: String,
        @SerialName("AcrossClue") val acrossClue: String,
        @SerialName("DownClue") val downClue: String,
        @SerialName("Copyright") val copyright: String = "",
    )
}