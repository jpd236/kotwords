package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.formats.Ipuz
import com.jeffpdavidson.kotwords.readStringResource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PatchworkTest {
    @Test
    fun jpzGeneration() = runTest {
        val puzzle = PATCHWORK.asPuzzle()
        val expected = readStringResource(PatchworkTest::class, "patchwork/patchwork.jpz")
        assertEquals(expected, puzzle.asJpz().toXmlString())
    }

    @Test
    fun ipuzGeneration() = runTest {
        val puzzle = PATCHWORK.asPuzzle()
        val expected = readStringResource(PatchworkTest::class, "patchwork/patchwork.ipuz")
        assertEquals(expected, Ipuz.asIpuzJson(puzzle).toJsonString())
    }

    @Test
    fun ipuzGeneration_unlabeledPieces() = runTest {
        val puzzle = PATCHWORK.copy(labelPieces = false).asPuzzle()
        val expected = readStringResource(PatchworkTest::class, "patchwork/patchwork-unlabeled-pieces.ipuz")
        assertEquals(expected, Ipuz.asIpuzJson(puzzle).toJsonString())
    }

    companion object {
        private val PATCHWORK = Patchwork(
            title = "Test title",
            creator = "Test creator",
            copyright = "Test copyright",
            description = "Test description",
            grid = listOf(
                listOf('A', 'B', 'C', 'D', 'E'),
                listOf('F', 'G', 'H', 'I', 'J'),
                listOf('K', 'L', 'M', 'N', 'O'),
                listOf('P', 'Q', 'R', 'S', 'T'),
                listOf('U', 'V', 'W', 'X', 'Y'),
            ),
            rowClues = listOf(
                listOf("Clue A1", "Clue A2"),
                listOf("Clue B1", "Clue B2"),
                listOf("Clue C1", "Clue C2"),
                listOf("Clue D1", "Clue D2"),
                listOf("Clue E1", "Clue E2"),
            ),
            pieceClues = listOf(
                "Piece 1",
                "Piece 2",
                "Piece 3",
                "Piece 4",
                "Piece 5",
            ),
            pieceNumbers = listOf(
                listOf(1, 1, 1, 2, 2),
                listOf(1, 1, 2, 2, 3),
                listOf(4, 4, 3, 3, 3),
                listOf(4, 4, 4, 3, 5),
                listOf(4, 5, 5, 5, 5),
            ),
            labelPieces = true,
        )
    }
}