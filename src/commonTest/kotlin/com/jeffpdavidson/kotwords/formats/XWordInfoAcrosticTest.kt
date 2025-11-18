package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.model.Acrostic
import com.jeffpdavidson.kotwords.readStringResource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class XWordInfoAcrosticTest {
    @Test
    fun acrosticPuzzle() = runTest {
        val acrostic = XWordInfoAcrostic(
            readStringResource(XWordInfoAcrosticTest::class, "xwordinfo/test-acrostic.json"),
            author = "Test byline",
        )
        assertEquals(
            Acrostic(
                title = "Acrostic for Sunday, January 1, 2023",
                creator = "Test byline",
                copyright = "© 2023 Test publisher",
                description = "",
                gridWidth = 20,
                solution = "ACRO-ST IC",
                gridKey = listOf(listOf(2, 1, 3), listOf(5, 6, 4, 7, 8)),
                clues = listOf("Clue 1", "Clue 2"),
                completionMessage = "Quote author, “Quote”",
                includeAttribution = true,
            ), acrostic.getPuzzleable()
        )
    }
}