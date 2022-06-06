package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.formats.ImageComparator
import com.jeffpdavidson.kotwords.formats.Jpz
import com.jeffpdavidson.kotwords.formats.Jpz.Companion.asJpzFile
import com.jeffpdavidson.kotwords.readStringResource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PuzzleTest {
    @Test
    fun jpzReadAndWrite() = runTest {
        val parsedJpz = Jpz.fromXmlString(readStringResource(PuzzleTest::class, "jpz/test.jpz"))
        val convertedJpz = parsedJpz.asPuzzle().asJpzFile(appletSettings = null)
        assertEquals(parsedJpz, convertedJpz)
    }

    @Test
    fun jpzReadAndWrite_gaps() = runTest {
        val parsedJpz = Jpz.fromXmlString(readStringResource(PuzzleTest::class, "jpz/gaps.jpz"))
        val convertedJpz = parsedJpz.asPuzzle().asJpzFile(appletSettings = null)
        assertEquals(parsedJpz, convertedJpz)
    }

    @Test
    fun jpzReadAndWrite_solved() = runTest {
        val parsedJpz = Jpz.fromXmlString(readStringResource(PuzzleTest::class, "jpz/test.jpz"))
        val convertedJpz = parsedJpz.asPuzzle().asJpzFile(solved = true, appletSettings = null)
        assertEquals(readStringResource(PuzzleTest::class, "jpz/test-solved.jpz"), convertedJpz.toXmlString())
    }

    @Test
    fun jpzReadAndWrite_inlineCells() = runTest {
        val parsedJpz = Jpz.fromXmlString(readStringResource(PuzzleTest::class, "jpz/test.jpz"))
        val parsedInlineCellJpz = Jpz.fromXmlString(readStringResource(PuzzleTest::class, "jpz/test-inline-cells.jpz"))
        val convertedInlineCellJpz = parsedInlineCellJpz.asPuzzle().asJpzFile(appletSettings = null)
        assertEquals(parsedJpz, convertedInlineCellJpz)
    }

    @Test
    fun jpzReadAndWrite_bgImages() = runTest {
        val parsedJpz = Jpz.fromXmlString(readStringResource(PuzzleTest::class, "jpz/test-bgimage.jpz"))
        val convertedJpz = parsedJpz.asPuzzle().asJpzFile(appletSettings = null)
        assertEquals(parsedJpz, convertedJpz)
    }
}

/**
 * Assert that two puzzles are equal.
 *
 * Any images in the grid are compared by their rendered contents rather than direct equality.
 */
suspend fun assertPuzzleEquals(expected: Puzzle, actual: Puzzle) {
    fun getImages(puzzle: Puzzle) =
        puzzle.grid.flatMapIndexed { y, row ->
            row.mapIndexed { x, cell ->
                (x to y) to cell.backgroundImage
            }
        }.toMap()

    fun removeImages(puzzle: Puzzle) =
        puzzle.copy(
            grid = puzzle.grid.map { row ->
                row.map { cell ->
                    cell.copy(backgroundImage = Puzzle.Image.None)
                }
            }
        )
    assertEquals(removeImages(expected), removeImages(actual))
    val expectedImages = getImages(expected)
    val actualImages = getImages(actual)
    assertEquals(expectedImages.keys, actualImages.keys)
    expectedImages.keys.forEach { (x, y) ->
        val expectedImage = expectedImages[x to y]
        val actualImage = actualImages[x to y]
        if (expectedImage is Puzzle.Image.Data) {
            assertTrue(actualImage is Puzzle.Image.Data)
            ImageComparator.assertPngEquals(expectedImage.bytes.toByteArray(), actualImage.bytes.toByteArray())
        }
    }
}