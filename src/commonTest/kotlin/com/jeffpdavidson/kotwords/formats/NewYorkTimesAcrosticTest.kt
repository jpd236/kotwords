package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.model.Acrostic
import com.jeffpdavidson.kotwords.readStringResource
import com.jeffpdavidson.kotwords.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class NewYorkTimesAcrosticTest {
    @Test
    fun extractPuzzleJson() = runTest {
        assertEquals(
            readStringResource(NewYorkTimesAcrosticTest::class, "nyt/test-acrostic.json").replace("\r\n", "\n"),
            NewYorkTimesAcrostic.extractPuzzleJson(
                readStringResource(NewYorkTimesAcrosticTest::class, "nyt/test-acrostic.html")
            )
        )
    }

    @Test
    fun extractPuzzleJson_invalid() {
        try {
            NewYorkTimesAcrostic.extractPuzzleJson("nothing to see here <script>empty</script>")
            fail()
        } catch (e: InvalidFormatException) {
            // expected
        }
    }

    @Test
    fun asAcrostic() = runTest {
        val acrostic = NewYorkTimesAcrostic(
            readStringResource(NewYorkTimesAcrosticTest::class, "nyt/test-acrostic.json"),
        )
        assertEquals(
            Acrostic(
                title = "Test title",
                creator = "Test byline / Test editor",
                copyright = "© Test copyright",
                description = "",
                suggestedWidth = null,
                solution = "ACRO-ST IC",
                gridKey = listOf(listOf(2, 1, 3), listOf(5, 6, 4, 7, 8)),
                clues = listOf("Clue 1", "Clue 2"),
                completionMessage = "\"Quote\" -Quote author, Source",
                includeAttribution = true,
            ), acrostic.asAcrostic()
        )
    }
}