package com.jeffpdavidson.kotwords.formats.json

import kotlinx.serialization.Serializable

internal object PuzzlrJson {
    @Serializable
    internal data class Cell(
        val answer: String,
        val clueNumber: Int? = null,
        val isBlack: Boolean = false,
        val isCircled: Boolean = false,
    )

    @Serializable
    internal data class Clue(
        val number: Int,
        val text: String,
        val direction: String,
        val answer: String? = null,
        val startRow: Int,
        val startCol: Int,
        val length: Int,
    )

    @Serializable
    internal data class Clues(
        val across: List<Clue> = listOf(),
        val down: List<Clue> = listOf(),
    )

    @Serializable
    internal data class PuzzleData(
        val title: String,
        val author: String,
        val date: String,
        val description: String? = null,
        val width: Int,
        val height: Int,
        val grid: List<List<Cell>>,
        val clues: Clues,
        val contestMode: Boolean = false,
        val contestClueText: String? = null,
    )

    @Serializable
    internal data class ResultData(
        val data: PuzzleData,
    )

    @Serializable
    internal data class Result(
        val data: ResultData,
    )

    @Serializable
    internal data class Response(
        val result: Result,
    )
}
