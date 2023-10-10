package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.model.Acrostic
import com.jeffpdavidson.kotwords.readStringResource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class WallStreetJournalAcrosticTest {
    @Test
    fun acrosticPuzzle() = runTest {
        val acrostic = WallStreetJournalAcrostic(
            readStringResource(WallStreetJournalAcrosticTest::class, "wsj/test-acrostic.json"),
        )
        assertEquals(
            Acrostic(
                title = "Test title",
                creator = "Test byline",
                copyright = "© 2021 Test publisher",
                description = "",
                suggestedWidth = 20,
                solution = "ACRO-ST IC",
                gridKey = listOf(listOf(2, 1, 3), listOf(5, 6, 4, 7, 8)),
                clues = listOf("Clue 1", "Clue 2"),
                completionMessage = "Quote author, “Quote”",
                includeAttribution = true,
            ), acrostic.getPuzzleable()
        )
    }

    @Test
    fun acrosticPuzzleNoByline() = runTest {
        val acrostic = WallStreetJournalAcrostic(
            readStringResource(WallStreetJournalAcrosticTest::class, "wsj/test-acrostic-no-byline.json"),
        )
        assertEquals(
            Acrostic(
                title = "Test title",
                creator = "",
                copyright = "© 2021 Test publisher",
                description = "",
                suggestedWidth = 20,
                solution = "ACRO-ST IC",
                gridKey = listOf(listOf(2, 1, 3), listOf(5, 6, 4, 7, 8)),
                clues = listOf("Clue 1", "Clue 2"),
                completionMessage = "Quote author, “Quote”",
                includeAttribution = true,
            ), acrostic.getPuzzleable()
        )
    }
}