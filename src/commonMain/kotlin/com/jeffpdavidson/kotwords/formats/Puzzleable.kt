package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.model.Puzzle

/** Exception thrown when puzzles are in an invalid format. */
open class InvalidFormatException(message: String) : Exception(message)

/** Interface for data that can be parsed as a [Puzzle]. */
interface Puzzleable {
    /** Parse and return the data as a [Puzzle]. */
    fun asPuzzle(): Puzzle
}