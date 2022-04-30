package com.jeffpdavidson.kotwords.web

import com.jeffpdavidson.kotwords.KotwordsInternal
import com.jeffpdavidson.kotwords.formats.Pdf.asPdf
import com.jeffpdavidson.kotwords.formats.PdfFonts
import com.jeffpdavidson.kotwords.model.HelterSkelter
import com.jeffpdavidson.kotwords.model.Puzzle
import com.jeffpdavidson.kotwords.util.trimmedLines
import com.jeffpdavidson.kotwords.web.html.FormFields
import com.jeffpdavidson.kotwords.web.html.Html

@JsExport
@KotwordsInternal
class HelterSkelterForm {
    private val form = PuzzleFileForm("helter-skelter", ::createPuzzle, createPdfFn = ::createPdf)
    private val grid: FormFields.TextBoxField = FormFields.TextBoxField("grid")
    private val answers: FormFields.TextBoxField = FormFields.TextBoxField("answers")
    private val clues: FormFields.TextBoxField = FormFields.TextBoxField("clues")
    private val answerVectors: FormFields.TextBoxField = FormFields.TextBoxField("answer-vectors")
    private val extendToEdges: FormFields.CheckBoxField = FormFields.CheckBoxField("extend-to-edges")

    init {
        Html.renderPage {
            form.render(this, bodyBlock = {
                grid.render(this, "Grid") {
                    placeholder = "Letters of the grid, separated into rows."
                    rows = "10"
                }
                answers.render(this, "Answers") {
                    placeholder =
                        "In sequential order, separated by whitespace. Non-alphabetical characters are ignored."
                    rows = "2"
                }
                clues.render(this, "Clues") {
                    placeholder = "One clue per row. Omit clue numbers."
                    rows = "10"
                }
                answerVectors.render(this, "Answer vectors (optional)") {
                    placeholder = "Starting location and direction of each answer. Enter one vector per row in the " +
                            "form \"x y direction\", where (x, y) is the starting location of the answer with (0, 0) " +
                            "representing the top-left square, and direction is one of N, NE, E, SE, S, SW, W, NW. " +
                            "May be omitted as long as each answer can be uniquely placed."
                    rows = "10"
                }
            }, advancedOptionsBlock = {
                extendToEdges.render(this, "Extend answers to grid edges (to mask enumerations)")
            })
        }
    }

    private suspend fun createPuzzle(): Puzzle = createHelterSkelter().asPuzzle()

    private suspend fun createPdf(blackSquareLightnessAdjustment: Float): ByteArray =
        createPuzzle().asPdf(
            fontFamily = PdfFonts.getNotoFontFamily(),
            blackSquareLightnessAdjustment = blackSquareLightnessAdjustment,
        )

    private fun createHelterSkelter(): HelterSkelter {
        return HelterSkelter(
            title = form.title,
            creator = form.creator,
            copyright = form.copyright,
            description = form.description,
            grid = grid.value.uppercase().trimmedLines().map { it.toList() },
            answers = answers.value.uppercase().replace("[^A-Z\\s]".toRegex(), "").split("\\s+".toRegex()),
            clues = clues.value.trimmedLines(),
            answerVectors = answerVectors.value.trimmedLines().map { rawVector ->
                val match = VECTOR_PATTERN.matchEntire(rawVector.uppercase())
                require(match != null) {
                    "Invalid answer vector: $rawVector"
                }
                HelterSkelter.AnswerVector(
                    start = match.groupValues[1].toInt() to match.groupValues[2].toInt(),
                    direction = when (match.groupValues[3]) {
                        "N" -> HelterSkelter.AnswerVector.Direction.NORTH
                        "NE" -> HelterSkelter.AnswerVector.Direction.NORTHEAST
                        "E" -> HelterSkelter.AnswerVector.Direction.EAST
                        "SE" -> HelterSkelter.AnswerVector.Direction.SOUTHEAST
                        "S" -> HelterSkelter.AnswerVector.Direction.SOUTH
                        "SW" -> HelterSkelter.AnswerVector.Direction.SOUTHWEST
                        "W" -> HelterSkelter.AnswerVector.Direction.WEST
                        "NW" -> HelterSkelter.AnswerVector.Direction.NORTHWEST
                        else -> throw IllegalArgumentException("Unknown direction: ${match.groupValues[3]}")
                    }
                )
            },
            extendToEdges = extendToEdges.value,
        )
    }

    companion object {
        private val VECTOR_PATTERN = "([0-9]+)\\s+([0-9]+)\\s(N|NE|E|SE|S|SW|W|NW)".toRegex()
    }
}