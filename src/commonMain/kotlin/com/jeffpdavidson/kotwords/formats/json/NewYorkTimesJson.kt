package com.jeffpdavidson.kotwords.formats.json

import kotlinx.serialization.Serializable

internal object NewYorkTimesJson {
    @Serializable
    data class ClueList(
        val clues: List<Int>,
        val name: String,
    )

    @Serializable
    data class Platforms(
        val web: Boolean,
    )

    @Serializable
    data class Note(
        val text: String,
        val platforms: Platforms,
    )

    @Serializable
    data class Meta(
        val constructors: List<String>,
        val copyright: String,
        val editor: String = "",
        val publicationDate: String,
        val notes: List<Note>? = listOf(),
        val title: String = "",
        val publishStream: String = "",
    )

    @Serializable
    data class Dimensions(
        val rowCount: Int,
        val columnCount: Int,
    )

    @Serializable
    data class MoreAnswers(
        val valid: List<String> = listOf()
    )

    @Serializable
    data class Cell(
        /** Cell type. 1 == regular, 2 == circled, 3 == shaded, 4 == external (void), 0/default == block. */
        val type: Int,
        val answer: String = "",
        val label: String = "",
        val moreAnswers: MoreAnswers = MoreAnswers(),
    )

    @Serializable
    data class Clue(
        val cells: List<Int>,
        val label: String,
        val text: String,
    )


    @Serializable
    data class GamePageData(
        val clueLists: List<ClueList>,
        val meta: Meta,
        val dimensions: Dimensions,
        val cells: List<Cell>,
        val clues: List<Clue>
    )

    @Serializable
    data class Data(val gamePageData: GamePageData)
}