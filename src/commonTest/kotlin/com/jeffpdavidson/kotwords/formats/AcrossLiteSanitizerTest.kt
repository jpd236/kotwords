package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.model.Puzzle
import kotlin.test.Test
import kotlin.test.assertEquals

class AcrossLiteSanitizerTest {
    @Test
    fun mapGivenToSanitizedClueNumbers() {
        // Skip 2-Down, 4-Across, and 5-Across/Down; add a fake 7-Down
        val rawGrid = listOf(
            "1 X 2 . . . .",
            "X X X . X 3 4",
            "5 X X . 6 X 7",
            "8 X X . 9 X X",
            ". . . . 10 X X"
        )
        val grid = rawGrid.map { row ->
            row.split(" ").map { square ->
                when (square) {
                    "." -> Puzzle.Cell(cellType = Puzzle.CellType.BLOCK)
                    "X" -> Puzzle.Cell(solution = "X")
                    else -> Puzzle.Cell(solution = "X", number = square)
                }
            }.toMutableList()
        }

        // Add cell borders, which should be ignored for Across Lite numbering purposes.
        grid[2][1] = grid[2][1].copy(borderDirections = setOf(Puzzle.BorderDirection.TOP, Puzzle.BorderDirection.TOP))

        val expectedSanitizedMap = mapOf(
            "1" to "1",
            "2" to "3",
            "3" to "6",
            "4" to "7",
            "5" to "8",
            "6" to "9",
            "8" to "10",
            "9" to "11",
            "10" to "12",
        )
        assertEquals(expectedSanitizedMap, AcrossLiteSanitizer.mapGivenToSanitizedClueNumbers(grid))
    }

    @Test
    fun sanitizeClue_normalClue() {
        val givenClue = "Just a normal clue"
        val givenToSanitizedClueNumMap: Map<String, String> = mapOf()
        assertEquals(
            "Just a normal clue",
            AcrossLiteSanitizer.sanitizeClue(givenClue, givenToSanitizedClueNumMap, true)
        )
    }

    @Test
    fun sanitizeClue_simpleReplacement_hyphen() {
        val givenClue = "See 25-Down"
        val givenToSanitizedClueNumMap = mapOf("25" to "27")
        assertEquals("See 27-Down", AcrossLiteSanitizer.sanitizeClue(givenClue, givenToSanitizedClueNumMap, true))
    }

    @Test
    fun sanitizeClue_simpleReplacement_space() {
        val givenClue = "See 25 Down"
        val givenToSanitizedClueNumMap = mapOf("25" to "27")
        assertEquals("See 27 Down", AcrossLiteSanitizer.sanitizeClue(givenClue, givenToSanitizedClueNumMap, true))
    }

    @Test
    fun sanitizeClue_complexReplacements_hyphen() {
        val givenClue = "Where the end of 17-, 25- and 47-Across and 14- and 28-Down may be found"
        val givenToSanitizedClueNumMap = mapOf("14" to "14", "17" to "17", "25" to "26", "28" to "29", "47" to "49")
        assertEquals(
            "Where the end of 17-, 26- and 49-Across and 14- and 29-Down may be found",
            AcrossLiteSanitizer.sanitizeClue(givenClue, givenToSanitizedClueNumMap, true)
        )
    }

    @Test
    fun sanitizeClue_complexReplacements_space() {
        val givenClue = "Where the end of 17, 25 and 47 Across and 14 and 28 Down may be found"
        val givenToSanitizedClueNumMap = mapOf("14" to "14", "17" to "17", "25" to "26", "28" to "29", "47" to "49")
        assertEquals(
            "Where the end of 17, 26 and 49 Across and 14 and 29 Down may be found",
            AcrossLiteSanitizer.sanitizeClue(givenClue, givenToSanitizedClueNumMap, true)
        )
    }

    @Test
    fun sanitizeClue_specialCharacters() {
        val givenClue = "★Clue with a <i>star</i>\n\n"
        val givenToSanitizedClueNumMap: Map<String, String> = mapOf()
        assertEquals(
            "*Clue with a \"star\"",
            AcrossLiteSanitizer.sanitizeClue(givenClue, givenToSanitizedClueNumMap, true)
        )
    }

    @Test
    fun sanitizeClue_specialCharacters_doNotSanitizeCharacters() {
        val givenClue = "★Clue with a <i>star</i>"
        val givenToSanitizedClueNumMap: Map<String, String> = mapOf()
        assertEquals(
            "★Clue with a \"star\"",
            AcrossLiteSanitizer.sanitizeClue(givenClue, givenToSanitizedClueNumMap, false)
        )
    }

    @Test
    fun sanitizeClue_html() {
        val givenClue =
            "Some <b>bold</b> and <i>italic</i> and <b><i>bold italic</i></b> and <i><b>italic bold</b></i> and" +
                    " <xyz>unknown</xyz><b><i><b></b></i></b>"
        val givenToSanitizedClueNumMap: Map<String, String> = mapOf()
        assertEquals(
            "Some *bold* and \"italic\" and *\"bold italic\"* and \"*italic bold*\" and unknown",
            AcrossLiteSanitizer.sanitizeClue(givenClue, givenToSanitizedClueNumMap, true)
        )
    }

    @Test
    fun sanitizeClue_blankHtml() {
        val givenClue = "<b></b>"
        val givenToSanitizedClueNumMap: Map<String, String> = mapOf()
        assertEquals(
            "-",
            AcrossLiteSanitizer.sanitizeClue(givenClue, givenToSanitizedClueNumMap, true)
        )
    }

    @Test
    fun sanitizeClue_atLengthLimit() {
        val givenClue = "A".repeat(505)
        assertEquals(givenClue, AcrossLiteSanitizer.sanitizeClue(givenClue, mapOf(), true))
    }

    @Test
    fun sanitizeClue_overLengthLimit() {
        val givenClue = "A".repeat(506)
        val expectedClue = "A".repeat(502) + "..."
        assertEquals(expectedClue, AcrossLiteSanitizer.sanitizeClue(givenClue, mapOf(), true))
    }
}