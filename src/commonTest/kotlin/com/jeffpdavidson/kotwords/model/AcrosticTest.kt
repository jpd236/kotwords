package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.readStringResource
import com.jeffpdavidson.kotwords.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class AcrosticTest {
    @Test
    fun asJpz() = runTest {
        assertEquals(
            readStringResource(AcrosticTest::class, "acrostic/acrostic.jpz"),
            acrostic.asPuzzle().asJpzFile().toXmlString()
        )
    }

    @Test
    fun asJpz_withAttribution() = runTest {
        assertEquals(
            readStringResource(AcrosticTest::class, "acrostic/acrostic-attribution.jpz"),
            acrostic.asPuzzle(includeAttribution = true).asJpzFile().toXmlString()
        )
    }

    @Suppress("UnusedDataClassCopyResult")
    @Test
    fun createWithAnswerValidation() {
        acrostic.copy(answers = listOf("CAR", "STOIC"))
    }

    @Suppress("UnusedDataClassCopyResult")
    @Test
    fun createWithBadAnswersFailsValidation() {
        try {
            acrostic.copy(answers = listOf("CRA", "STOIC"))
            fail("Should have thrown IllegalArgumentException due to bad answers")
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun getAnswerColumnWidths_cluesMax() {
        // 15 | 13 -> 16 | 1 | 14, greater than min width of 27
        val splitAnswers = listOf(listOf((1..15).toList()), listOf((1..13).toList()))
        assertEquals(
            16 to 14,
            Acrostic.getAnswerColumnWidths(splitAnswers, 28)
        )
    }

    @Test
    fun getAnswerColumnWidths_minWidthMax() {
        // 10 | 9 -> 11 | 1 | 10, padded to 13 | 1 | 13
        val splitAnswers = listOf(listOf((1..10).toList()), listOf((1..9).toList()))
        assertEquals(
            13 to 13,
            Acrostic.getAnswerColumnWidths(splitAnswers, 0)
        )
    }

    @Test
    fun getAnswerColumnWidths_suggestedWidthMax() {
        // 10 | 9 -> 11 | 1 | 10, padded to 15 | 1 | 14
        val splitAnswers = listOf(listOf((1..10).toList()), listOf((1..9).toList()))
        assertEquals(
            15 to 14,
            Acrostic.getAnswerColumnWidths(splitAnswers, 30)
        )
    }

    companion object {
        private val acrostic = Acrostic(
            title = "Test title",
            creator = "Test creator",
            copyright = "Test copyright",
            description = "Test description",
            suggestedWidth = 0,
            solution = "ACRO-ST IC",
            gridKey = listOf(listOf(2, 1, 3), listOf(5, 6, 4, 7, 8)),
            clues = listOf("Clue 1", "Clue 2"),
            crosswordSolverSettings = Puzzle.CrosswordSolverSettings(
                "#00b100", "#80ff80", "All done!"
            )
        )
    }
}