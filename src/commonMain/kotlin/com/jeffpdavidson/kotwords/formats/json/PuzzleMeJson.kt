package com.jeffpdavidson.kotwords.formats.json

import kotlinx.serialization.Serializable

internal object PuzzleMeJson {
    @Serializable
    internal data class Clue(val clue: String)

    @Serializable
    internal data class PlacedWord(
        val clue: Clue,
        val clueNum: Int,
        val acrossNotDown: Boolean,
        val nBoxes: Int = 0,
        val x: Int = 0,
        val y: Int = 0,
    )

    @Serializable
    internal data class CellInfo(
        val x: Int,
        val y: Int,
        val isCircled: Boolean,
        val isVoid: Boolean,
        val fgColor: String = "",
        val bgColor: String = "",
        val topWall: Boolean = false,
        val bottomWall: Boolean = false,
        val leftWall: Boolean = false,
        val rightWall: Boolean = false,
    )

    @Serializable
    internal data class Data(
        val title: String,
        val description: String,
        val copyright: String,
        val author: String,
        val help: String? = null,
        val box: List<List<String?>>,
        val cellInfos: List<CellInfo> = listOf(),
        val placedWords: List<PlacedWord>,
        // List of circled squares locations in the form [x, y]
        val backgroundShapeBoxes: List<List<Int>> = listOf(),
        // List of words intersecting a particular location
        val boxToPlacedWordsIdxs: List<List<List<Int>?>> = listOf(),
        // Squares which should have their solution revealed at the start
        val preRevealIdxs: List<List<Boolean>> = listOf(),
        // Clue numbers for each square. Normally inferrable but may be needed for non-traditional grids.
        val clueNums: List<List<Int>> = listOf(),
    )
}