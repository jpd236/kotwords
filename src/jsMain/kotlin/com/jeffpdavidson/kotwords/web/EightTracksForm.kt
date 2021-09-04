package com.jeffpdavidson.kotwords.web

import com.jeffpdavidson.kotwords.KotwordsInternal
import com.jeffpdavidson.kotwords.model.EightTracks
import com.jeffpdavidson.kotwords.model.Puzzle
import com.jeffpdavidson.kotwords.web.html.FormFields
import com.jeffpdavidson.kotwords.web.html.Html
import kotlinx.html.InputType
import kotlinx.html.div
import kotlin.js.Promise

@JsExport
@KotwordsInternal
class EightTracksForm {
    private val jpzForm = PuzzleFileForm("eight-tracks", ::createPuzzle)
    private val title: FormFields.InputField = FormFields.InputField("title")
    private val creator: FormFields.InputField = FormFields.InputField("creator")
    private val copyright: FormFields.InputField = FormFields.InputField("copyright")
    private val description: FormFields.TextBoxField = FormFields.TextBoxField("description")
    private val trackDirections: FormFields.InputField = FormFields.InputField("track-directions")
    private val trackStartingOffsets: FormFields.InputField = FormFields.InputField("track-starting-offsets")
    private val trackAnswers: FormFields.TextBoxField = FormFields.TextBoxField("track-answers")
    private val trackClues: FormFields.TextBoxField = FormFields.TextBoxField("track-clues")
    private val includeEnumerationsAndDirection: FormFields.CheckBoxField =
        FormFields.CheckBoxField("include-enumerations-and-direction")
    private val lightTrackColor: FormFields.InputField = FormFields.InputField("light-track-color")
    private val darkTrackColor: FormFields.InputField = FormFields.InputField("dark-track-color")

    init {
        Html.renderPage {
            jpzForm.render(this, bodyBlock = {
                this@EightTracksForm.title.render(this, "Title")
                creator.render(this, "Creator (optional)")
                copyright.render(this, "Copyright (optional)")
                description.render(this, "Description (optional)") {
                    rows = "5"
                }
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
                includeEnumerationsAndDirection.render(this, "Include clue enumerations and track directions") {
                    checked = true
                }
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

    private fun createPuzzle(): Promise<Puzzle> {
        val eightTracks = EightTracks(
            title = title.getValue(),
            creator = creator.getValue(),
            copyright = copyright.getValue(),
            description = description.getValue(),
            trackDirections = trackDirections.getValue().split("\\s+".toRegex()).map {
                if (it == "+") EightTracks.Direction.CLOCKWISE else EightTracks.Direction.COUNTERCLOCKWISE
            },
            trackStartingOffsets = trackStartingOffsets.getValue().split("\\s+".toRegex()).map { it.toInt() },
            trackAnswers = trackAnswers.getValue().split("\n").map { clues ->
                clues.trim().split("/").map { it.trim() }
            },
            trackClues = trackClues.getValue().split("\n").map { clues ->
                clues.trim().split("/").map { it.trim() }
            },
            includeEnumerationsAndDirections = includeEnumerationsAndDirection.getValue(),
            lightTrackColor = lightTrackColor.getValue(),
            darkTrackColor = darkTrackColor.getValue(),
        )
        return Promise.resolve(eightTracks.asPuzzle())
    }
}