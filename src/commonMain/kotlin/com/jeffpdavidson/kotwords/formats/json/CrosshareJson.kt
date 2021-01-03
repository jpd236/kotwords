package com.jeffpdavidson.kotwords.formats.json

import kotlinx.serialization.Serializable

internal object CrosshareJson {
    @Serializable
    internal data class Size(val rows: Int, val cols: Int)

    @Serializable
    internal data class Clue(
        val dir: Int,
        val clue: String,
        val num: Int,
    )

    @Serializable
    internal data class Puzzle(
        val authorName: String,
        val title: String,
        // Note: this is speculative. Copyright seems to be stripped when uploading .puz files.
        val copyright: String = "",
        val size: Size,
        val clues: List<Clue>,
        val grid: List<String>,
        val highlighted: List<Int>,
        val constructorNotes: String?,
    )

    @Serializable
    internal data class PageProps(val puzzle: Puzzle)

    @Serializable
    internal data class Props(val pageProps: PageProps)

    @Serializable
    internal data class Data(val props: Props)
}