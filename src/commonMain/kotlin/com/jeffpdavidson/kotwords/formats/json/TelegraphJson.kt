package com.jeffpdavidson.kotwords.formats.json

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal object TelegraphJson {

    @Serializable
    internal data class Response(
        val json: Data,
    )

    @Serializable
    internal data class Data(
        val copy: Copy,
        val grid: List<List<Square>> = emptyList(),
        val meta: Meta? = null,
    )

    @Serializable
    internal data class Copy(
        val title: String = "",
        val description: String = "",
        val setter: String = "",
        val byline: String = "",
        val publisher: String = "",
        val words: List<Word> = emptyList(),
        val clues: List<ClueSection> = emptyList(),
    )

    @Serializable
    internal data class Word(
        val id: Int,
        val x: String,
        val y: String,
        val solution: String? = null,
    )

    @Serializable
    internal data class ClueSection(
        val name: String = "",
        val title: String = "",
        val clues: List<Clue> = emptyList(),
    )

    @Serializable
    internal data class Clue(
        val word: Int,
        val number: Int,
        val clue: String,
        val format: String = "",
    )

    @Serializable
    internal data class Square(
        @SerialName("SquareID") val squareId: Int? = null,
        @SerialName("Number") val number: String = "",
        @SerialName("Blank") val blank: String = "",
        @SerialName("Letter") val letter: String = "",
        @SerialName("Shaded") val shaded: Boolean = false,
    )

    @Serializable
    internal data class Meta(
        val variant: String? = null,
        val author: String? = null,
        val description: String? = null,
    )
}
