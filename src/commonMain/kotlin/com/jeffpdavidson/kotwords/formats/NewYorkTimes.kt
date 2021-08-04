package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.json.JsonSerializer
import com.jeffpdavidson.kotwords.formats.json.NewYorkTimesJson
import com.jeffpdavidson.kotwords.model.Crossword
import com.jeffpdavidson.kotwords.model.Puzzle
import com.soywiz.klock.DateFormat
import com.soywiz.klock.format
import com.soywiz.klock.parseDate

private val PUZZLE_DATA_REGEX = """\bpluribus\s*=\s*'([^']+)'""".toRegex()

private val TITLE_DATE_FORMAT = DateFormat("EEEE, MMMM d, yyyy")
private val PUBLICATION_DATE_FORMAT = DateFormat("YYYY-MM-dd")

/**
 * Container for a puzzle in the New York Times embedded web format.
 *
 * Prefer [asPuzzle] where possible - New York Times puzzles occasionally make use of alternative word lists that cannot
 * be encoded with the [Crossword] interface, but can be encoded as a [Puzzle] (and saved as a JPZ file). [asCrossword]
 * should still work most of the time but may omit key information.
 *
 * Known, potentially fixable issues:
 * - Older NYT puzzles may use "&" instead of "&amp;" (example: 2013/05/08, 7-Down).
 * - 2020/07/08 42-Across uses <s> for strike-through, which is technically unsupported in JPZ.
 */
class NewYorkTimes(private val json: String) : Crosswordable {

    override fun asCrossword(): Crossword = asPuzzle().asJpzFile().asCrossword()

    fun asPuzzle(): Puzzle {
        val data = JsonSerializer.fromJson<NewYorkTimesJson.Data>(json).gamePageData
        val publicationDate = PUBLICATION_DATE_FORMAT.parseDate(data.meta.publicationDate)
        val puzzleName = if (data.meta.publishStream == "mini") "NY Times Mini Crossword" else "NY Times"
        val baseTitle = "$puzzleName, ${TITLE_DATE_FORMAT.format(publicationDate)}"

        val grid = (0 until data.dimensions.rowCount).map { y ->
            (0 until data.dimensions.columnCount).map { x ->
                val cell = data.cells[y * data.dimensions.columnCount + x]
                val cellType = if (cell.type == 0) Puzzle.CellType.BLOCK else Puzzle.CellType.REGULAR
                val backgroundColor = if (cell.type == 3) "#dcdcdc" else ""
                // Circle "external" squares as well, since these are puzzle-dependent and vary.
                val backgroundShape =
                    if (cell.type == 2 || cell.type == 4) {
                        Puzzle.BackgroundShape.CIRCLE
                    } else {
                        Puzzle.BackgroundShape.NONE
                    }
                Puzzle.Cell(
                    x = x + 1,
                    y = y + 1,
                    solution = cell.answer,
                    backgroundColor = backgroundColor,
                    number = cell.label,
                    cellType = cellType,
                    backgroundShape = backgroundShape,
                )
            }
        }

        return Puzzle(
            title = listOfNotNull(baseTitle, data.meta.title.ifEmpty { null }).joinToString(" "),
            creator = renderByline(constructors = data.meta.constructors, editor = data.meta.editor),
            copyright = "© ${data.meta.copyright}, The New York Times",
            description = data.meta.notes?.filter { it.platforms.web }?.map { it.text }?.firstOrNull() ?: "",
            grid = grid,
            clues = data.clueLists.map { clueList ->
                Puzzle.ClueList(title = "<b>${clueList.name}</b>", clues = clueList.clues.map { clueIndex ->
                    val clue = data.clues[clueIndex]
                    Puzzle.Clue(wordId = clueIndex, number = clue.label, text = clue.text)
                })
            },
            words = data.clues.mapIndexed { clueIndex, clue ->
                Puzzle.Word(id = clueIndex, cells = clue.cells.map {
                    grid[it / data.dimensions.columnCount][it % data.dimensions.rowCount]
                })
            },
            hasHtmlClues = true,
            crosswordSolverSettings = Puzzle.CrosswordSolverSettings()
        )
    }

    private fun renderByline(constructors: List<String>, editor: String): String {
        val joinedConstructors = when (constructors.size) {
            0 -> null
            1 -> constructors[0]
            2 -> "${constructors[0]} and ${constructors[1]}"
            else -> "${constructors.subList(0, constructors.size - 1).joinToString(", ")} and ${constructors.last()}"
        }
        return listOfNotNull(joinedConstructors, editor.ifEmpty { null }).joinToString(" / ")
    }

    companion object {
        fun fromHtml(html: String): NewYorkTimes = NewYorkTimes(extractPuzzleJson(html))

        fun fromPluribus(pluribus: String): NewYorkTimes = NewYorkTimes(decodePluribus(pluribus))

        internal fun extractPuzzleJson(html: String): String {
            // Look for "pluribus='[data]'" inside <script> tags; this is JSON puzzle data
            // encoded as an escaped string compressed with LZString.
            Xml.parse(html, format = DocumentFormat.HTML).select("script").forEach {
                val matchResult = PUZZLE_DATA_REGEX.find(it.data)
                if (matchResult != null) {
                    return decodePluribus(matchResult.groupValues[1])
                }
            }
            throw InvalidFormatException("Could not find puzzle data in New York Times HTML")
        }

        private fun decodePluribus(pluribus: String): String = LzString.decompress(Encodings.unescape(pluribus))
    }
}