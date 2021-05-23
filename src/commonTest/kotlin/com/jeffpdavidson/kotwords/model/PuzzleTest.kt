package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.formats.CrosswordCompiler
import com.jeffpdavidson.kotwords.formats.Jpz
import com.jeffpdavidson.kotwords.readStringResource
import com.jeffpdavidson.kotwords.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PuzzleTest {
    @Test
    fun jpzReadAndWrite() = runTest {
        val parsedJpz = Jpz.fromXmlString(readStringResource(PuzzleTest::class, "jpz/test.jpz"))
        val convertedJpz = Puzzle.fromCrossword(parsedJpz.asCrossword()).asJpzFile()
        assertEquals(parsedJpz, convertedJpz)
    }

    @Test
    fun jpzReadAndWrite_gaps() = runTest {
        val parsedJpz = Jpz.fromXmlString(readStringResource(PuzzleTest::class, "jpz/gaps.jpz"))
        val convertedJpz = Puzzle.fromCrossword(parsedJpz.asCrossword()).asJpzFile()

        // TODO(#9): The conversion process currently drops 7D because it's not a conventional word. Once this issue is
        // fixed, it should no longer be necessary to manually add 7D back to the converted JPZ.
        assertTrue(convertedJpz is CrosswordCompiler)
        val adjustedClues = listOf(
            convertedJpz.rectangularPuzzle.crossword?.clues!![0],
            convertedJpz.rectangularPuzzle.crossword?.clues!![1].copy(
                clues = convertedJpz.rectangularPuzzle.crossword?.clues!![1].clues +
                        Jpz.RectangularPuzzle.Crossword.Clues.Clue(
                            word = 1007,
                            number = "7",
                            text = listOf("Will be removed")
                        )
            )
        )
        val adjustedWords = convertedJpz.rectangularPuzzle.crossword?.words!! +
                Jpz.RectangularPuzzle.Crossword.Word(
                    1007,
                    listOf(
                        Jpz.RectangularPuzzle.Crossword.Word.Cells(7, 3),
                        Jpz.RectangularPuzzle.Crossword.Word.Cells(7, 4),
                        Jpz.RectangularPuzzle.Crossword.Word.Cells(7, 5),
                    )
                )
        val adjustedJpz = convertedJpz.copy(
            rectangularPuzzle = convertedJpz.rectangularPuzzle.copy(
                crossword = convertedJpz.rectangularPuzzle.crossword?.copy(
                    clues = adjustedClues,
                    words = adjustedWords
                )
            )
        )

        assertEquals(parsedJpz, adjustedJpz)
    }

    @Test
    fun jpzReadAndWrite_solved() = runTest {
        val parsedJpz = Jpz.fromXmlString(readStringResource(PuzzleTest::class, "jpz/test.jpz"))
        val convertedJpz = Puzzle.fromCrossword(parsedJpz.asCrossword()).asJpzFile(solved = true)
        assertEquals(readStringResource(PuzzleTest::class, "jpz/test-solved.jpz"), convertedJpz.toXmlString())
    }
}