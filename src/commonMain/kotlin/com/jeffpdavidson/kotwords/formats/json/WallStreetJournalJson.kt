package com.jeffpdavidson.kotwords.formats.json

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal object WallStreetJournalJson {
    @Serializable
    internal data class Gridsize(val cols: Int, val rows: Int)

    @Serializable
    internal data class Clue(
        val clue: String,
        val number: Int = 0
    )

    @Serializable
    internal data class ClueSet(val title: String, val clues: List<Clue>)

    @Serializable
    internal data class Copy(
        val title: String,
        val byline: String,
        val description: String,
        @SerialName("date-publish") val datePublish: String,
        val publisher: String,
        val gridsize: Gridsize,
        val clues: List<ClueSet> = listOf(),
    )

    @Serializable
    internal data class Style(val shapebg: String, val highlight: Boolean)

    @Serializable
    internal data class Square(
        @SerialName("Letter") val letter: String,
        val style: Style = Style(shapebg = "", highlight = false)
    )

    @Serializable
    internal data class Data(val copy: Copy, val grid: List<List<Square>>)

    @Serializable
    internal data class CrosswordJson(val data: Data)

    @Serializable
    internal data class Settings(
        val solution: String,
        val clues: List<Clue>,
    )

    @Serializable
    internal data class AcrosticJson(
        val copy: Copy,
        val settings: Settings,
        val celldata: String,
        val cluedata: String,
    )
}