package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.formats.ImageComparator
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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