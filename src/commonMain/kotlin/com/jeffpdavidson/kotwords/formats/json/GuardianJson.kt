package com.jeffpdavidson.kotwords.formats.json

import kotlinx.serialization.Serializable

internal object GuardianJson {

    @Serializable
    internal data class Entry(
        val number: Int,
        val clue: String,
        val direction: String,
        val length: Int,
        val position: Position,
        val solution: String = "",
    ) {
        @Serializable
        internal data class Position(
            val x: Int,
            val y: Int,
        )
    }

    @Serializable
    internal data class Data(
        val name: String,
        val creator: Creator = Creator(),
        val dimensions: Dimensions,
        val instructions: String = "",
        val entries: List<Entry>,
    ) {
        @Serializable
        internal data class Dimensions(
            val cols: Int,
            val rows: Int,
        )

        @Serializable
        internal data class Creator(
            val name: String = "",
        )
    }
}