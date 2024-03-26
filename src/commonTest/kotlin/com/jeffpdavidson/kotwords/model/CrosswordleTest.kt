package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.readStringResource
import io.github.pdvrieze.xmlutil.testutil.assertXmlEquals
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class CrosswordleTest {
    @Test
    fun jpzGeneration() = runTest {
        val crosswordle = Crosswordle(
            title = "Test title",
            creator = "Test creator",
            copyright = "Test copyright",
            description = "Test description",
            grid = listOf(
                listOf('Z', 'Y', 'X', 'W', 'V'),
                listOf('E', 'H', 'Z', 'Z', 'Z'),
                listOf('L', 'E', 'L', 'Z', 'L'),
                listOf('L', 'L', 'Z', 'Z', 'L'),
                listOf('L', 'O', 'H', 'E', 'L'),
            ),
            answer = "HELLO",
            acrossClues = listOf(
                "Across clue 1",
                "Across clue 2",
                "Across clue 3",
                "Across clue 4",
                "Across clue 5",
            ),
            downClues = listOf(
                "Down clue 1",
                "Down clue 2",
                "Down clue 3",
                "Down clue 4",
                "Down clue 5",
            )
        )
        val puzzle = crosswordle.asPuzzle()

        val expected = readStringResource(CrosswordleTest::class, "crosswordle.jpz")
        assertXmlEquals(expected, puzzle.asJpz().toXmlString())
    }
}