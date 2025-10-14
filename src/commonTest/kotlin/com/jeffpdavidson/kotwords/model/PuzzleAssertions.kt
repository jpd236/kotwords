package com.jeffpdavidson.kotwords.model

import korlibs.image.core.CoreImage
import korlibs.image.core.CoreImage32Color
import korlibs.image.core.decodeBytes
import kotlin.math.abs
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
            val expectedImageData = CoreImage.decodeBytes(expectedImage.bytes.toByteArray()).to32()
            val actualImageData = CoreImage.decodeBytes(actualImage.bytes.toByteArray()).to32()
            // On some platforms, we may get color values that are very slightly different than on others. This is
            // unlikely to be a bug in Kotwords as we delegate the image processing to a library. So we tolerate slight
            // differences in each color channel.
            expectedImageData.data.zip(actualImageData.data).forEach { (expected, actual) ->
                val expectedColor = CoreImage32Color(expected)
                val actualColor = CoreImage32Color(actual)
                assertTrue(abs(expectedColor.red - actualColor.red) <= 1)
                assertTrue(abs(expectedColor.green - actualColor.green) <= 1)
                assertTrue(abs(expectedColor.blue - actualColor.blue) <= 1)
                assertTrue(abs(expectedColor.alpha - actualColor.alpha) <= 1)
            }
        }
    }
}