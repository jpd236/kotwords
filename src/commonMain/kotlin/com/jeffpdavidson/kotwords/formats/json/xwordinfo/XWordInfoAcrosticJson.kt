package com.jeffpdavidson.kotwords.formats.json.xwordinfo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal object XWordInfoAcrosticJson {
    @Serializable
    data class Response(
        val date: String,
        val copyright: String,
        val answerKey: String,
        val quote: String? = null,
        val clues: List<String>,
        val clueData: List<String>,
        val cols: Int,
    )

    @Serializable
    data class EncodedResponse(@SerialName("Data") val data: String)
}