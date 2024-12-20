package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.formats.CrosswordCompilerApplet
import com.jeffpdavidson.kotwords.readStringResource
import io.github.pdvrieze.xmlutil.testutil.assertXmlEquals
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.fail

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
            includeEnumerations = true,
            includeDirections = true,
            lightTrackColor = "#FFFFFF",
            darkTrackColor = "#C0C0C0",
        )
        val puzzle = eightTracks.asPuzzle()

        val expected = readStringResource(EightTracks::class, "eight-tracks/annotations.jpz")
        assertXmlEquals(
            expected, puzzle.asJpz(
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
            includeEnumerations = false,
            includeDirections = false,
            lightTrackColor = "#FFFFFF",
            darkTrackColor = "#C0C0C0",
        )
        val puzzle = eightTracks.asPuzzle()

        val expected = readStringResource(EightTracks::class, "eight-tracks/no-annotations.jpz")
        assertXmlEquals(
            expected, puzzle.asJpz(
                appletSettings = CrosswordCompilerApplet.AppletSettings(
                    cursorColor = "#00b100",
                    selectedCellsColor = "#80ff80",
                    completion = CrosswordCompilerApplet.AppletSettings.Completion(message = "All done!"),
                )
            ).toXmlString()
        )
    }

    @Test
    fun jpzGeneration_markInnerTrackStarts() = runTest {
        val eightTracks = EightTracks(
            title = "Test title",
            creator = "Test creator",
            copyright = "Test copyright",
            description = "Test description",
            trackDirections = listOf(EightTracks.Direction.CLOCKWISE, EightTracks.Direction.COUNTERCLOCKWISE),
            trackStartingOffsets = listOf(3, 7),
            trackAnswers = listOf(listOf("CDEF", "GHIJK", "LMN", "OPAB"), listOf("MKIG", "ECAO")),
            trackClues = listOf(listOf("Clue 1", "Clue 2", "Clue 3", "Clue 4"), listOf("Clue 1", "Clue 2")),
            includeEnumerations = true,
            includeDirections = true,
            lightTrackColor = "#FFFFFF",
            darkTrackColor = "#C0C0C0",
            markInnerTrackStarts = true,
        )
        val puzzle = eightTracks.asPuzzle()

        val expected = readStringResource(EightTracks::class, "eight-tracks/inner-track-starts.jpz")
        assertXmlEquals(
            expected, puzzle.asJpz(
                appletSettings = CrosswordCompilerApplet.AppletSettings(
                    cursorColor = "#00b100",
                    selectedCellsColor = "#80ff80",
                    completion = CrosswordCompilerApplet.AppletSettings.Completion(message = "All done!"),
                )
            ).toXmlString()
        )
    }

    @Test
    fun asPuzzle_invalidSection() = runTest {
        val eightTracks = EightTracks(
            title = "Test title",
            creator = "Test creator",
            copyright = "Test copyright",
            description = "Test description",
            trackDirections = listOf(EightTracks.Direction.CLOCKWISE, EightTracks.Direction.COUNTERCLOCKWISE),
            trackStartingOffsets = listOf(3, 7),
            trackAnswers = listOf(listOf("CDEF", "GHIJK", "LMN", "OPAB"), listOf("MKIG", "ECAQ")),
            trackClues = listOf(listOf("Clue 1", "Clue 2", "Clue 3", "Clue 4"), listOf("Clue 1", "Clue 2")),
            includeEnumerations = true,
            includeDirections = true,
            lightTrackColor = "#FFFFFF",
            darkTrackColor = "#C0C0C0",
        )
        try {
            eightTracks.asPuzzle()
            fail("Expected invalid section to throw IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }
}