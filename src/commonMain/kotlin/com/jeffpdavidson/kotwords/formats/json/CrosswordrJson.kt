package com.jeffpdavidson.kotwords.formats.json

import kotlinx.serialization.Serializable

internal object CrosswordrJson {
    @Serializable
    internal data class Response(val data: Data) {
        @Serializable
        internal data class Data(val puzzleV2: PuzzleV2) {
            @Serializable
            internal data class PuzzleV2(val puzzle: Puzzle) {
                @Serializable
                internal data class Puzzle(
                    val content: Content,
                    val width: Int,
                    val height: Int,
                    val title: String? = null,
                    val description: String? = null,
                    val editedBy: String? = null,
                    val byline: String? = null,
                    val postSolveNote: String? = null,
                ) {
                    @Serializable
                    internal data class Content(
                        val cells: List<Cell>,
                        val clues: Clues,
                    ) {
                        @Serializable
                        internal data class Cell(
                            val circled: Boolean = false,
                            val isBlack: Boolean = false,
                            val solution: String? = null,
                            val start: Int? = null,
                            val backgroundColor: String? = null,
                        )

                        @Serializable
                        internal data class Clues(
                            val across: ClueList,
                            val down: ClueList,
                        ) {
                            @Serializable
                            internal data class ClueList(
                                val data: List<Clue>,
                            ) {
                                @Serializable
                                internal data class Clue(
                                    val cells: List<Int>,
                                    val clue: String,
                                    val index: Int,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}