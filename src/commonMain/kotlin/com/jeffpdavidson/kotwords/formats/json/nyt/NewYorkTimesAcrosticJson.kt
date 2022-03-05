package com.jeffpdavidson.kotwords.formats.json.nyt

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal object NewYorkTimesAcrosticJson {
    @Serializable
    data class PuzzleMeta(val editor: String = "")

    @Serializable
    data class Data(
        @SerialName("puzzle_data") val puzzleData: String,
        @SerialName("puzzle_meta") val puzzleMeta: PuzzleMeta = PuzzleMeta(),
    )
}