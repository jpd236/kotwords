package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.formats.Jpz.Companion.asJpzFile
import com.jeffpdavidson.kotwords.readStringResource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class HelterSkelterTest {
    @Test
    fun asPuzzle() = runTest {
        assertEquals(
            readStringResource(HelterSkelterTest::class, "helter-skelter/helter-skelter.jpz"),
            helterSkelter.asPuzzle().asJpzFile().toXmlString(),
        )
    }

    @Test
    fun asPuzzle_withoutVectors() = runTest {
        assertEquals(
            readStringResource(HelterSkelterTest::class, "helter-skelter/helter-skelter.jpz"),
            helterSkelter.copy(answerVectors = listOf()).asPuzzle().asJpzFile().toXmlString(),
        )
    }

    @Test
    fun asPuzzle_extendToEdges() = runTest {
        assertEquals(
            readStringResource(HelterSkelterTest::class, "helter-skelter/helter-skelter-extend-to-edges.jpz"),
            helterSkelter.copy(extendToEdges = true).asPuzzle().asJpzFile().toXmlString(),
        )
    }

    companion object {
        private val helterSkelter = HelterSkelter(
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
            answers = listOf(
                "ABCD",
                "DHLP",
                "PKF",
                "FGHIJ",
                "JOTY",
                "TSRQ",
                "RMHC",
                "HLP",
                "LQV",
                "VU",
                "UQMIE",
                "MNO",
                "OTY",
                "YXWV",
                "WQK",
            ),
            clues = listOf(
                "Clue 1",
                "Clue 2",
                "Clue 3",
                "Clue 4",
                "Clue 5",
                "Clue 6",
                "Clue 7",
                "Clue 8",
                "Clue 9",
                "Clue 10",
                "Clue 11",
                "Clue 12",
                "Clue 13",
                "Clue 14",
                "Clue 15",
            ),
            answerVectors = listOf(
                HelterSkelter.AnswerVector(0 to 0, HelterSkelter.AnswerVector.Direction.EAST),
                HelterSkelter.AnswerVector(3 to 0, HelterSkelter.AnswerVector.Direction.SOUTHWEST),
                HelterSkelter.AnswerVector(0 to 3, HelterSkelter.AnswerVector.Direction.NORTH),
                HelterSkelter.AnswerVector(0 to 1, HelterSkelter.AnswerVector.Direction.EAST),
                HelterSkelter.AnswerVector(4 to 1, HelterSkelter.AnswerVector.Direction.SOUTH),
                HelterSkelter.AnswerVector(4 to 3, HelterSkelter.AnswerVector.Direction.WEST),
                HelterSkelter.AnswerVector(2 to 3, HelterSkelter.AnswerVector.Direction.NORTH),
                HelterSkelter.AnswerVector(2 to 1, HelterSkelter.AnswerVector.Direction.SOUTHWEST),
                HelterSkelter.AnswerVector(1 to 2, HelterSkelter.AnswerVector.Direction.SOUTH),
                HelterSkelter.AnswerVector(1 to 4, HelterSkelter.AnswerVector.Direction.WEST),
                HelterSkelter.AnswerVector(0 to 4, HelterSkelter.AnswerVector.Direction.NORTHEAST),
                HelterSkelter.AnswerVector(2 to 2, HelterSkelter.AnswerVector.Direction.EAST),
                HelterSkelter.AnswerVector(4 to 2, HelterSkelter.AnswerVector.Direction.SOUTH),
                HelterSkelter.AnswerVector(4 to 4, HelterSkelter.AnswerVector.Direction.WEST),
                HelterSkelter.AnswerVector(2 to 4, HelterSkelter.AnswerVector.Direction.NORTHWEST),
            )
        )
    }
}