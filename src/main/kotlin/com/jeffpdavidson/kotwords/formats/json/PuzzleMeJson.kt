package com.jeffpdavidson.kotwords.formats.json

import se.ansman.kotshi.JsonDefaultValue
import se.ansman.kotshi.JsonSerializable

internal object PuzzleMeJson {
    @JsonSerializable
    internal data class Clue(val clue: String)

    @JsonSerializable
    internal data class PlacedWord(val clue: Clue, val clueNum: Int, val acrossNotDown: Boolean)

    @JsonSerializable
    internal data class CellInfo(val x: Int, val y: Int, val isCircled: Boolean)

    @JsonSerializable
    internal data class Data(
            val title: String,
            val description: String,
            val copyright: String,
            val author: String,
            val box: List<List<String>>,
            @JsonDefaultValue val cellInfos: List<CellInfo>,
            val placedWords: List<PlacedWord>,
            // List of circled squares locations in the form [x, y]
            @JsonDefaultValue val backgroundShapeBoxes: List<List<Int>>)
}