package com.jeffpdavidson.kotwords.web

import com.jeffpdavidson.kotwords.model.Puzzle
import com.jeffpdavidson.kotwords.model.TwistsAndTurns
import com.jeffpdavidson.kotwords.web.html.FormFields.inputField
import com.jeffpdavidson.kotwords.web.html.FormFields.textBoxField
import com.jeffpdavidson.kotwords.web.html.Html.getElementById
import com.jeffpdavidson.kotwords.web.html.Html.renderPage
import kotlinx.html.InputType
import kotlinx.html.div
import kotlinx.html.dom.append
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLTextAreaElement
import kotlin.js.Promise

private const val ID_TITLE = "title"
private const val ID_CREATOR = "creator"
private const val ID_COPYRIGHT = "copyright"
private const val ID_DESCRIPTION = "description"
private const val ID_WIDTH = "width"
private const val ID_HEIGHT = "height"
private const val ID_TWIST_BOX_SIZE = "twist-box-size"
private const val ID_TURNS_ANSWERS = "turns-answers"
private const val ID_TURNS_CLUES = "turns-clues"
private const val ID_TWISTS_CLUES = "twists-clues"
private const val ID_LIGHT_TWISTS_COLOR = "light-twists-color"
private const val ID_DARK_TWISTS_COLOR = "dark-twists-color"

/** Form to convert Twists and Turns puzzles into JPZ files. */
class TwistsAndTurnsForm {
    private val jpzForm = JpzForm(::createPuzzle)
    private val title: HTMLInputElement by getElementById(ID_TITLE)
    private val creator: HTMLInputElement by getElementById(ID_CREATOR)
    private val copyright: HTMLInputElement by getElementById(ID_COPYRIGHT)
    private val description: HTMLTextAreaElement by getElementById(ID_DESCRIPTION)
    private val width: HTMLInputElement by getElementById(ID_WIDTH)
    private val height: HTMLInputElement by getElementById(ID_HEIGHT)
    private val twistBoxSize: HTMLInputElement by getElementById(ID_TWIST_BOX_SIZE)
    private val turnsAnswers: HTMLTextAreaElement by getElementById(ID_TURNS_ANSWERS)
    private val turnsClues: HTMLTextAreaElement by getElementById(ID_TURNS_CLUES)
    private val twistsClues: HTMLTextAreaElement by getElementById(ID_TWISTS_CLUES)
    private val lightTwistsColor: HTMLInputElement by getElementById(ID_LIGHT_TWISTS_COLOR)
    private val darkTwistsColor: HTMLInputElement by getElementById(ID_DARK_TWISTS_COLOR)

    init {
        renderPage {
            with(jpzForm) {
                append.jpzForm(bodyBlock = {
                    inputField(ID_TITLE, "Title")
                    inputField(ID_CREATOR, "Creator (optional)")
                    inputField(ID_COPYRIGHT, "Copyright (optional)")
                    textBoxField(ID_DESCRIPTION, "Description (optional)") {
                        rows = "5"
                    }
                    div(classes = "form-row") {
                        inputField(ID_WIDTH, "Width", flexCols = 4) {
                            type = InputType.number
                        }
                        inputField(ID_HEIGHT, "Height", flexCols = 4) {
                            type = InputType.number
                        }
                        inputField(ID_TWIST_BOX_SIZE, "Twist width/height", flexCols = 4) {
                            type = InputType.number
                        }
                    }
                    textBoxField(ID_TURNS_ANSWERS, "Turns answers") {
                        placeholder =
                                "In sequential order, separated by whitespace. Non-alphabetical characters are ignored."
                        rows = "2"
                    }
                    textBoxField(ID_TURNS_CLUES, "Turns clues") {
                        placeholder = "One clue per row. Omit clue numbers."
                        rows = "10"
                    }
                    textBoxField(ID_TWISTS_CLUES, "Twists clues") {
                        placeholder = "Ordered from left-to-right, top-to-bottom. One clue per row. Omit clue numbers."
                        rows = "10"
                    }
                }, advancedOptionsBlock = {
                    div(classes = "form-row") {
                        inputField(ID_LIGHT_TWISTS_COLOR, "Light twists color", flexCols = 6) {
                            type = InputType.color
                            value = "#FFFFFF"
                        }
                        inputField(ID_DARK_TWISTS_COLOR, "Dark twists color", flexCols = 6) {
                            type = InputType.color
                            value = "#888888"
                        }
                    }
                })
            }
        }
    }

    private fun createPuzzle(crosswordSolverSettings: Puzzle.CrosswordSolverSettings): Promise<Puzzle> {
        val twistsAndTurns = TwistsAndTurns(
                title = title.value.trim(),
                creator = creator.value.trim(),
                copyright = copyright.value.trim(),
                description = description.value.trim(),
                width = width.value.trim().toInt(),
                height = height.value.trim().toInt(),
                twistBoxSize = twistBoxSize.value.trim().toInt(),
                turnsAnswers = turnsAnswers.value.trim().toUpperCase().replace("[^A-Z ]", "").split(" +".toRegex()),
                turnsClues = turnsClues.value.trim().split("\n").map { it.trim() },
                twistsClues = twistsClues.value.trim().split("\n").map { it.trim() },
                lightTwistsColor = lightTwistsColor.value.trim(),
                darkTwistsColor = darkTwistsColor.value.trim(),
                crosswordSolverSettings = crosswordSolverSettings)
        return Promise.resolve(twistsAndTurns.asPuzzle())
    }
}