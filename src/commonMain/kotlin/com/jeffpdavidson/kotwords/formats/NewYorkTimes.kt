package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.json.nyt.NewYorkTimesApiJson
import com.jeffpdavidson.kotwords.formats.json.nyt.NewYorkTimesJson
import com.jeffpdavidson.kotwords.formats.json.nyt.NewYorkTimesPluribusJson
import com.jeffpdavidson.kotwords.model.Puzzle
import korlibs.time.DateFormat
import korlibs.time.format
import korlibs.time.parseDate
import okio.ByteString
import okio.ByteString.Companion.toByteString
import okio.use
import kotlin.math.roundToInt

private val PUZZLE_DATA_REGEX = """\bpluribus\s*=\s*'([^']+)'""".toRegex()

private val TITLE_DATE_FORMAT = DateFormat("EEEE, MMMM d, yyyy")
private val PUBLICATION_DATE_FORMAT = DateFormat("YYYY-MM-dd")

/** Container for a puzzle in the New York Times format. */
class NewYorkTimes internal constructor(
    private val data: NewYorkTimesJson,
    private val httpGetter: (suspend (String) -> ByteArray)? = null
) : Puzzleable() {
    override suspend fun createPuzzle(): Puzzle {
        val publicationDate = PUBLICATION_DATE_FORMAT.parseDate(data.publicationDate)
        val puzzleName = if (data.publishStream == "mini") "NY Times Mini Crossword" else "NY Times"
        val baseTitle = "$puzzleName, ${TITLE_DATE_FORMAT.format(publicationDate)}"

        val backgroundImageUrl = data.beforeStartOverlay
        var hasUnsupportedFeatures = false
        val backgroundImageData =
            if (backgroundImageUrl == null || httpGetter == null) {
                // If we have a background image, but can't load it, add the unsupported features flag. Background
                // pictures may be instrumental to the puzzle (e.g. 2021-02-14).
                hasUnsupportedFeatures = backgroundImageUrl != null
                null
            } else {
                httpGetter.invoke(backgroundImageUrl)
            }
        val cellBackgrounds = mutableMapOf<Pair<Int, Int>, ByteString>()
        if (backgroundImageData != null) {
            val format = ParsedImageFormat.fromExtension(backgroundImageUrl!!.substringAfterLast('.'))
            ParsedImage.parse(format, backgroundImageData).use { backgroundImage ->
                val borderWidth = getBorderWidth(backgroundImage.width)
                if (borderWidth == null) {
                    // Have a background image but can't determine the border width to crop it properly.
                    hasUnsupportedFeatures = true
                    return@use
                }
                // Slice the image into cells which can be set as background images in each cell.
                // Only include images which contain non-transparent pixels.
                val cellWidth = (backgroundImage.width - 2 * borderWidth) / data.width
                val cellHeight = (backgroundImage.height - 2 * borderWidth) / data.height
                for (y in 0 until data.height) {
                    for (x in 0 until data.width) {
                        backgroundImage.crop(
                            width = cellWidth.roundToInt(),
                            height = cellHeight.roundToInt(),
                            x = (borderWidth + x * cellWidth).roundToInt(),
                            y = (borderWidth + y * cellHeight).roundToInt(),
                        ).use { cellImage ->
                            if (cellImage.containsVisiblePixels()) {
                                cellBackgrounds[x to y] = cellImage.toPngBytes().toByteString()
                            }
                        }
                    }
                }
            }
        }

        val grid = (0 until data.height).map { y ->
            (0 until data.width).map { x ->
                val cell = data.cells[y * data.width + x]
                val cellType = when (cell.type) {
                    0 -> Puzzle.CellType.BLOCK
                    4 -> Puzzle.CellType.VOID
                    else -> Puzzle.CellType.REGULAR
                }
                val backgroundColor = if (cell.type == 3) "#dcdcdc" else ""
                val backgroundShape =
                    if (cell.type == 2) {
                        Puzzle.BackgroundShape.CIRCLE
                    } else {
                        Puzzle.BackgroundShape.NONE
                    }
                val cellBackground =
                    cellBackgrounds[x to y]?.let { Puzzle.Image.Data(Puzzle.ImageFormat.PNG, it) } ?: Puzzle.Image.None
                Puzzle.Cell(
                    solution = Encodings.decodeHtmlEntities(cell.answer),
                    backgroundColor = backgroundColor,
                    number = cell.label,
                    cellType = cellType,
                    backgroundShape = backgroundShape,
                    moreAnswers = cell.moreAnswers.valid,
                    backgroundImage = cellBackground,
                )
            }
        }

        val webNotes = data.notes?.filter { it.platforms.web }

        return Puzzle(
            title = listOfNotNull(baseTitle, data.title.normalizeEntities().ifEmpty { null }).joinToString(" "),
            creator = renderByline(constructors = data.constructors, editor = data.editor),
            copyright = "© ${data.copyright}, The New York Times",
            description = webNotes?.map { it.text.normalizeEntities() }?.firstOrNull() ?: "",
            grid = grid,
            clues = data.clueLists.map { clueList ->
                Puzzle.ClueList(title = "<b>${clueList.name}</b>", clues = clueList.clues.map { clueIndex ->
                    val clue = data.clues[clueIndex]
                    Puzzle.Clue(wordId = clueIndex, number = clue.label, text = clue.text.normalizeEntities())
                })
            },
            words = data.clues.mapIndexed { clueIndex, clue ->
                Puzzle.Word(id = clueIndex, cells = clue.cells.map {
                    Puzzle.Coordinate(x = it % data.width, y = it / data.width)
                })
            },
            hasHtmlClues = true,
            hasUnsupportedFeatures = hasUnsupportedFeatures,
        )
    }

    /** Return the list of extra data URLs that will be requested with [httpGetter] by [asPuzzle]. */
    fun getExtraDataUrls(): List<String> {
        return listOfNotNull(data.beforeStartOverlay)
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

    /**
     * Get the border width of the grid, in pixels, given the width of the space the grid is rendered to.
     *
     * The board element is the SVG (in a deconstructed JSON format); we search for the "grid" data-group, which has a
     * "rect" child with a width and relative stroke width. We then scale up the stroke width proportionally to the
     * given width.
     */
    internal fun getBorderWidth(imageWidth: Int): Double? {
        val grid = data.board.children.firstOrNull { it.attributes.getOrElse("data-group") { "" } == "grid" }
        val rect = grid?.children?.firstOrNull { it.name == "rect" }
        val width = rect?.attributes?.getOrElse("width") { "" } ?: ""
        val strokeWidth = rect?.attributes?.getOrElse("strokeWidth") {
            rect.attributes.getOrElse("stroke-width") { "" }
        } ?: ""
        return if (width.isEmpty() || strokeWidth.isEmpty()) {
            null
        } else {
            val strokeWidthDouble = strokeWidth.toDouble()
            strokeWidthDouble * imageWidth / (width.toDouble() + strokeWidthDouble)
        }
    }

    companion object {
        fun fromHtml(html: String, httpGetter: (suspend (String) -> ByteArray)? = null): NewYorkTimes =
            NewYorkTimes(NewYorkTimesPluribusJson.parse(extractPuzzleJson(html)), httpGetter)

        fun fromPluribusJson(pluribusJson: String, httpGetter: (suspend (String) -> ByteArray)? = null): NewYorkTimes =
            NewYorkTimes(NewYorkTimesPluribusJson.parse(pluribusJson), httpGetter)

        fun fromPluribus(pluribus: String, httpGetter: (suspend (String) -> ByteArray)? = null): NewYorkTimes =
            fromPluribusJson(decodePluribus(pluribus), httpGetter)

        fun fromApiJson(
            apiJson: String,
            stream: String,
            httpGetter: (suspend (String) -> ByteArray)? = null
        ): NewYorkTimes =
            NewYorkTimes(NewYorkTimesApiJson.parse(apiJson, stream), httpGetter)

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

        internal fun decodePluribus(pluribus: String): String = LzString.decompress(Encodings.unescape(pluribus))

        private fun String.normalizeEntities(): String {
            // Text uses a mix of valid HTML entities and standalone "&" characters, which are invalid HTML/XML.
            // First, decode the valid HTML entities to get a regular string.
            // Then, encode & and < to get valid XML.
            // Finally, re-decode known valid HTML tags, with substitutions for unsupported tags.
            return Encodings.decodeHtmlEntities(this)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(
                    "&lt;(/?(?:b|i|sup|sub|span|strong|s|br|em))( [^>]*)?>".toRegex(RegexOption.IGNORE_CASE),
                    "<$1>"
                )
                .replace("<(/?)strong>".toRegex(RegexOption.IGNORE_CASE), "<$1b>")
                .replace("<(/?)em>".toRegex(RegexOption.IGNORE_CASE), "<$1i>")
                .replace("</?s>".toRegex(RegexOption.IGNORE_CASE), "---")
                .replace("<br>".toRegex(RegexOption.IGNORE_CASE), "\n")
        }
    }
}