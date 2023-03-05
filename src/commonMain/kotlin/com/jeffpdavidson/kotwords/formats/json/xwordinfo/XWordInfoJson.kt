package com.jeffpdavidson.kotwords.formats.json.xwordinfo

import kotlinx.serialization.Serializable

internal object XWordInfoJson {
    @Serializable
    data class Size(
        val rows: Int,
        val cols: Int,
    )

    @Serializable
    data class Clues(
        val across: List<String>,
        val down: List<String>,
    )

    @Serializable
    data class Response(
        val title: String,
        val author: String,
        val editor: String? = null,
        val copyright: String,
        val notepad: String? = null,
        val size: Size,
        // We ignore gridnums here because there's no obvious/consistent way to infer what each word would be if not
        // standard.
        val grid: List<String>,
        val type: String? = null,
        val clues: Clues,
        val circles: List<Int>? = null,
    )
}