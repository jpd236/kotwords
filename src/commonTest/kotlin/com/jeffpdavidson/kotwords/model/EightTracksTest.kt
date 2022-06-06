package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.formats.CrosswordCompilerApplet
import com.jeffpdavidson.kotwords.formats.Jpz.Companion.asJpzFile
import com.jeffpdavidson.kotwords.readStringResource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class EightTracksTest {
    @Test
    fun jpzGeneration_withAnnotations() = runTest {
        val eightTracks = EightTracks(
            title = "Test title",
            creator = "Test creator",
            copyright = "Test copyright",
            description = "Test description",
            trackDirections = listOf(EightTracks.Direction.CLOCKWISE, EightTracks.Direction.COUNTERCLOCKWISE),
            trackStartingOffsets = listOf(3, 7),
            trackAnswers = listOf(listOf("CDEF", "GHIJK", "LMN", "OPAB"), listOf("MKIG", "ECAO")),
            trackClues = listOf(listOf("Clue 1", "Clue 2", "Clue 3", "Clue 4"), listOf("Clue 1", "Clue 2")),
            includeEnumerationsAndDirections = true,
            lightTrackColor = "#FFFFFF",
            darkTrackColor = "#C0C0C0",
        )
        val puzzle = eightTracks.asPuzzle()

        val expected = readStringResource(EightTracks::class, "eight-tracks/annotations.jpz")
        assertEquals(
            expected, puzzle.asJpzFile(
                appletSettings = CrosswordCompilerApplet.AppletSettings(
                    cursorColor = "#00b100",
                    selectedCellsColor = "#80ff80",
                    completion = CrosswordCompilerApplet.AppletSettings.Completion(message = "All done!"),
                )
            ).toXmlString()
        )
    }

    @Test
    fun jpzGeneration_withoutAnnotations() = runTest {
        val eightTracks = EightTracks(
            title = "Test title",
            creator = "Test creator",
            copyright = "Test copyright",
            description = "Test description",
            trackDirections = listOf(EightTracks.Direction.CLOCKWISE, EightTracks.Direction.COUNTERCLOCKWISE),
            trackStartingOffsets = listOf(3, 7),
            trackAnswers = listOf(listOf("CDEF", "GHIJK", "LMN", "OPAB"), listOf("MKIG", "ECAO")),
            trackClues = listOf(listOf("Clue 1", "Clue 2", "Clue 3", "Clue 4"), listOf("Clue 1", "Clue 2")),
            includeEnumerationsAndDirections = false,
            lightTrackColor = "#FFFFFF",
            darkTrackColor = "#C0C0C0",
        )
        val puzzle = eightTracks.asPuzzle()

        val expected = readStringResource(EightTracks::class, "eight-tracks/no-annotations.jpz")
        assertEquals(
            expected, puzzle.asJpzFile(
                appletSettings = CrosswordCompilerApplet.AppletSettings(
                    cursorColor = "#00b100",
                    selectedCellsColor = "#80ff80",
                    completion = CrosswordCompilerApplet.AppletSettings.Completion(message = "All done!"),
                )
            ).toXmlString()
        )
    }
}