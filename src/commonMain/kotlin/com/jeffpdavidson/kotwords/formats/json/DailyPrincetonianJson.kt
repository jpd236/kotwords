package com.jeffpdavidson.kotwords.formats.json

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal object DailyPrincetonianJson {
    @Serializable
    data class Crossword(
        val title: String,
    )

    @Serializable
    data class Author(
        @SerialName("first_name") val firstName: String,
        @SerialName("last_name") val lastName: String,
    )

    @Serializable
    data class Clue(
        val answer: String,
        val clue: String,
        @SerialName("is_across") val isAcross: Boolean,
        val x: Int,
        val y: Int,
    )
}