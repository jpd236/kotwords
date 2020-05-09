package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.readUtf8Resource
import kotlin.js.Promise
import kotlin.test.Test
import kotlin.test.assertEquals

// TODO: Expand test coverage
class TwistsAndTurnsTest {
    @Test
    fun jpzGeneration(): Promise<Any> {
        val puzzle = TwistsAndTurns.fromRawInput(
                "Test title",
                "Test creator",
                "Test copyright",
                "Test description",
                "2",
                "2",
                "1",
                "AB CD",
                "Turn 1\nTurn 2",
                "Twist 1\nTwist 2\nTwist 3\nTwist 4",
                "#ffffff",
                "#888888",
                CrosswordSolverSettings("#00b100", "#80ff80", "All done!"))

        return readUtf8Resource("twists-and-turns.jpz").then { expected ->
            assertEquals(expected, puzzle.asJpz().asXmlString())
        }
    }
}