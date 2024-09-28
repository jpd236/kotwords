package com.jeffpdavidson.kotwords.web

import com.jeffpdavidson.kotwords.KotwordsInternal
import com.jeffpdavidson.kotwords.model.EightTracks
import com.jeffpdavidson.kotwords.model.Puzzle
import com.jeffpdavidson.kotwords.util.trimmedLines
import com.jeffpdavidson.kotwords.web.html.FormFields
import com.jeffpdavidson.kotwords.web.html.Html
import kotlinx.html.InputType
import kotlinx.html.div

@JsExport
@KotwordsInternal
class EightTracksForm {
    private val form = PuzzleFileForm("eight-tracks", ::createPuzzle)
    private val trackDirections: FormFields.InputField = FormFields.InputField("track-directions")
    private val trackStartingOffsets: FormFields.InputField = FormFields.InputField("track-starting-offsets")
    private val trackAnswers: FormFields.TextBoxField = FormFields.TextBoxField("track-answers")
    private val trackClues: FormFields.TextBoxField = FormFields.TextBoxField("track-clues")
    private val includeEnumerations: FormFields.CheckBoxField = FormFields.CheckBoxField("include-enumerations")
    private val includeDirections: FormFields.CheckBoxField = FormFields.CheckBoxField("include-directions")
    private val trackLabel: FormFields.SelectField = FormFields.SelectField("track-label")
    private val lightTrackColor: FormFields.InputField = FormFields.InputField("light-track-color")
    private val darkTrackColor: FormFields.InputField = FormFields.InputField("dark-track-color")

    init {
        Html.renderPage {
            form.render(this, bodyBlock = {
                trackDirections.render(this, "Track directions") {
                    placeholder = "Direction of each track, separated by whitespace. " +
                            "Use + for clockwise tracks and - for counter-clockwise tracks."
                }
                trackStartingOffsets.render(this, "Track starting offsets") {
                    placeholder = "Starting position for the first entry in each track, separated by whitespace. " +
                            "Positions start at 1 in the upper-left corner and increase in the clockwise direction."
                }
                trackAnswers.render(this, "Track answers") {
                    placeholder = "The answers for each track; one line per row. " +
                            "Separate multiple answers for a track with a /."
                    rows = "8"
                }
                trackClues.render(this, "Track clues") {
                    placeholder = "The clues for each track; one line per track. " +
                            "Separate multiple clues for a track with a /."
                    rows = "8"
                }
            }, advancedOptionsBlock = {
                div(classes = "form-row") {
                    includeEnumerations.render(this, "Include clue enumerations", flexCols = 6) {
                        checked = true
                    }
                    includeDirections.render(this, "Include track directions", flexCols = 6) {
                        checked = true
                    }
                }
                trackLabel.render(
                    this,
                    "Track label",
                    EightTracks.TrackLabel.entries.map { it.name.lowercase().replaceFirstChar { it.uppercase() } },
                    help = "How to label the tracks in the clue list"
                )
                div(classes = "form-row") {
                    lightTrackColor.render(this, "Light track color", flexCols = 6) {
                        type = InputType.color
                        value = "#FFFFFF"
                    }
                    darkTrackColor.render(this, "Dark track color", flexCols = 6) {
                        type = InputType.color
                        value = "#C0C0C0"
                    }
                }
            })
        }
    }

    private suspend fun createPuzzle(): Puzzle {
        val eightTracks = EightTracks(
            title = form.title,
            creator = form.creator,
            copyright = form.copyright,
            description = form.description,
            trackDirections = trackDirections.value.split("\\s+".toRegex()).map {
                if (it == "+") EightTracks.Direction.CLOCKWISE else EightTracks.Direction.COUNTERCLOCKWISE
            },
            trackStartingOffsets = trackStartingOffsets.value.split("\\s+".toRegex()).map { it.toInt() },
            trackAnswers = trackAnswers.value.uppercase().trimmedLines().map { clues ->
                clues.split("/").map { it.trim() }
            },
            trackClues = trackClues.value.trimmedLines().map { clues ->
                clues.split("/").map { it.trim() }
            },
            includeEnumerations = includeEnumerations.value,
            includeDirections = includeDirections.value,
            lightTrackColor = lightTrackColor.value,
            darkTrackColor = darkTrackColor.value,
            trackLabel = EightTracks.TrackLabel.valueOf(trackLabel.value.uppercase())
        )
        return eightTracks.asPuzzle()
    }
}