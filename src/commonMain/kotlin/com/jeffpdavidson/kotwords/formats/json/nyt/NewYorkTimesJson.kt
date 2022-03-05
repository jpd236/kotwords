package com.jeffpdavidson.kotwords.formats.json.nyt

import kotlinx.serialization.Serializable

internal interface NewYorkTimesJson {

    @Serializable
    data class ClueList(
        val clues: List<Int>,
        val name: String,
    )

    @Serializable
    data class Platforms(
        val web: Boolean = false,
    )

    @Serializable
    data class Note(
        val text: String,
        val platforms: Platforms,
    )

    @Serializable
    data class MoreAnswers(
        val valid: List<String> = listOf()
    )

    @Serializable
    data class Cell(
        /** Cell type. 1 == regular, 2 == circled, 3 == shaded, 4 == external (void), 0/default == block. */
        val type: Int = 0,
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
    data class XmlElement(
        val name: String = "",
        val attributes: Map<String, String> = mapOf(),
        val children: List<XmlElement> = listOf(),
    )

    val publicationDate: String
    val publishStream: String
    val height: Int
    val width: Int
    val cells: List<Cell>
    val notes: List<Note>?
    val title: String
    val constructors: List<String>
    val editor: String
    val copyright: String
    val clueLists: List<ClueList>
    val clues: List<Clue>
    val beforeStartOverlay: String?
    val board: XmlElement
}