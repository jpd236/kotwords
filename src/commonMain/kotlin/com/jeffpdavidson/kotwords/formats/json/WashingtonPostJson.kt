package com.jeffpdavidson.kotwords.formats.json

import kotlinx.serialization.Serializable

internal object WashingtonPostJson {
    @Serializable
    internal data class Cell(
        val answer: String = "",
        val number: String = "",
        val type: String = "",
        val circle: Boolean = false,
        val background: String = "",
        val bars: Int = 0,
    )

    @Serializable
    internal data class Word(
        val direction: String,
        val clue: String,
    )

    @Serializable
    internal data class Puzzle(
        val title: String = "",
        val width: Int,
        val creator: String = "",
        val copyright: String = "",
        val description: String = "",
        val cells: List<Cell>,
        val words: List<Word>,
    )
}