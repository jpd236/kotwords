package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.model.Crossword

/** Exception thrown when puzzles are in an invalid format. */
open class InvalidFormatException(message: String) : Exception(message)

/** Interface for data that can be parsed as a [Crossword]. */
interface Crosswordable {
    /** Parse and return the data as a [Crossword]. */
    fun asCrossword(): Crossword
}