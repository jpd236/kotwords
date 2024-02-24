package com.jeffpdavidson.kotwords.web

import com.jeffpdavidson.kotwords.KotwordsInternal
import com.jeffpdavidson.kotwords.model.HeartsAndArrows
import com.jeffpdavidson.kotwords.model.Puzzle
import com.jeffpdavidson.kotwords.model.RowsGarden
import com.jeffpdavidson.kotwords.util.trimmedLines
import com.jeffpdavidson.kotwords.web.html.FormFields
import com.jeffpdavidson.kotwords.web.html.Html
import kotlinx.html.InputType
import kotlinx.html.div
import kotlinx.html.dom.append
import kotlinx.html.p

/** Form to convert Hearts and Arrows puzzles into digital puzzle files. */
@JsExport
@KotwordsInternal
class HeartsAndArrowsForm {
    private val form = PuzzleFileForm("hearts-and-arrows", ::createPuzzle)
    private val solutionGrid: FormFields.TextBoxField = FormFields.TextBoxField("solution-grid")
    private val arrowClues: FormFields.TextBoxField = FormFields.TextBoxField("arrow-clues")
    private val arrowAnswers: FormFields.TextBoxField = FormFields.TextBoxField("arrow-answers")
    private val lightClues: FormFields.TextBoxField = FormFields.TextBoxField("light-clues")
    private val lightAnswers: FormFields.TextBoxField = FormFields.TextBoxField("light-answers")
    private val mediumClues: FormFields.TextBoxField = FormFields.TextBoxField("medium-clues")
    private val mediumAnswers: FormFields.TextBoxField = FormFields.TextBoxField("medium-answers")
    private val darkClues: FormFields.TextBoxField = FormFields.TextBoxField("dark-clues")
    private val darkAnswers: FormFields.TextBoxField = FormFields.TextBoxField("dark-answers")
    private val addAnnotations: FormFields.CheckBoxField = FormFields.CheckBoxField("add-annotations")
    private val lightHeartColor: FormFields.InputField = FormFields.InputField("light-heart-color")
    private val mediumHeartColor: FormFields.InputField = FormFields.InputField("medium-heart-color")
    private val darkHeartColor: FormFields.InputField = FormFields.InputField("dark-heart-color")
    private val labelHearts: FormFields.CheckBoxField = FormFields.CheckBoxField("label-hearts")

    init {
        Html.renderPage {
            append.p {
                +"Note: heart colors are determined by making the top-left heart dark, cycling between light, medium, "
                +"and dark in the band of hearts extending east-southeast from there, and then ensuring that no two "
                +"adjacent hearts have the same color."
            }
            form.render(this, bodyBlock = {
                solutionGrid.render(this, "Solution grid") {
                    rows = "16"
                    placeholder = "The solution grid. Use periods to represent empty cells."
                }
                div(classes = "form-row") {
                    arrowClues.render(this, "Arrow clues", flexCols = 6) {
                        rows = "16"
                        placeholder =
                            "The clues for each arrow; one line per arrow. Separate multiple clues for an arrow " +
                                    "with a /."
                    }
                    arrowAnswers.render(this, "Arrow answers", flexCols = 6) {
                        rows = "16"
                        placeholder =
                            "The answers for each arrow; one line per arrow. Separate multiple answers for an " +
                                    "arrow with a /."
                    }
                }
                div(classes = "form-row") {
                    lightClues.render(this, "Light clues", flexCols = 6) {
                        rows = "8"
                        placeholder =
                            "The light heart clues; one answer per line."
                    }
                    lightAnswers.render(this, "Light answers", flexCols = 6) {
                        rows = "8"
                        placeholder =
                            "The light heart answers; one answer per line."
                    }
                }
                div(classes = "form-row") {
                    mediumClues.render(this, "Medium clues", flexCols = 6) {
                        rows = "8"
                        placeholder =
                            "The medium heart clues; one answer per line."
                    }
                    mediumAnswers.render(this, "Medium answers", flexCols = 6) {
                        rows = "8"
                        placeholder =
                            "The medium heart answers; one answer per line."
                    }
                }
                div(classes = "form-row") {
                    darkClues.render(this, "Dark clues", flexCols = 6) {
                        rows = "8"
                        placeholder =
                            "The dark heart clues; one answer per line."
                    }
                    darkAnswers.render(this, "Dark answers", flexCols = 6) {
                        rows = "8"
                        placeholder =
                            "The dark heart answers; one answer per line."
                    }
                }
            }, advancedOptionsBlock = {
                labelHearts.render(
                    this,
                    "Label hearts (unlabeled requires Ipuz and the Crossword Nexus or squares.io solver)"
                ) {
                    checked = true
                }
                addAnnotations.render(this, "Add clue annotations (e.g. \"hyph.\", \"2 wds.\")") {
                    checked = false
                }
                div(classes = "form-row") {
                    lightHeartColor.render(this, "Light heart color", flexCols = 4) {
                        type = InputType.color
                        value = "#FFFFFF"
                    }
                    mediumHeartColor.render(this, "Medium heart color", flexCols = 4) {
                        type = InputType.color
                        value = "#F4BABA"
                    }
                    darkHeartColor.render(this, "Dark heart color", flexCols = 4) {
                        type = InputType.color
                        value = "#E06666"
                    }
                }
            })
        }
    }

    private suspend fun createPuzzle(): Puzzle {
        val heartsAndArrows = HeartsAndArrows(
            title = form.title,
            creator = form.creator,
            copyright = form.copyright,
            description = form.description,
            solutionGrid = solutionGrid.value.trimmedLines().map { it.toList() },
            arrows = arrowClues.value.trimmedLines().zip(arrowAnswers.value.uppercase().trimmedLines())
                .map { (clues, answers) ->
                    clues.split("/")
                        .zip(answers.split("/"))
                        .map { (clue, answer) -> RowsGarden.Entry(clue.trim(), answer.trim()) }
                },
            light = lightClues.value.trimmedLines().zip(lightAnswers.value.uppercase().trimmedLines())
                .map { (clue, answer) -> RowsGarden.Entry(clue, answer) },
            medium = mediumClues.value.trimmedLines().zip(mediumAnswers.value.uppercase().trimmedLines())
                .map { (clue, answer) -> RowsGarden.Entry(clue, answer) },
            dark = darkClues.value.trimmedLines().zip(darkAnswers.value.uppercase().trimmedLines())
                .map { (clue, answer) -> RowsGarden.Entry(clue, answer) },
            lightHeartColor = lightHeartColor.value,
            mediumHeartColor = mediumHeartColor.value,
            darkHeartColor = darkHeartColor.value,
            addHyphenated = addAnnotations.value,
            addWordCount = addAnnotations.value,
            labelHearts = labelHearts.value,
        )
        return heartsAndArrows.asPuzzle()
    }
}