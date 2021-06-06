package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.model.BLACK_SQUARE
import com.jeffpdavidson.kotwords.model.Crossword
import com.jeffpdavidson.kotwords.model.Puzzle
import com.jeffpdavidson.kotwords.model.Square
import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.core.toByteArray
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.modules.SerializersModule
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.XmlException
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue

private const val CCA_NS = "http://crossword.info/xml/crossword-compiler-applet"
private const val PUZZLE_NS = "http://crossword.info/xml/rectangular-puzzle"

// TODO: Is it possible to restrict the types of the elements of this list at compile time?
// See https://github.com/pdvrieze/xmlutil/issues/30
private typealias Snippet = List<@Polymorphic Any>

private val XmlSerializer = XML(Jpz.module()) {
    xmlDeclMode = XmlDeclMode.Charset
    autoPolymorphic = true
    // Ignore unknown elements
    unknownChildHandler = { _, _, _, _ -> }
}

/** Container for a puzzle in the JPZ file format. */
interface Jpz : Crosswordable {
    val rectangularPuzzle: RectangularPuzzle

    fun toXmlString(): String

    @Serializable
    @XmlSerialName("rectangular-puzzle", PUZZLE_NS, "")
    data class RectangularPuzzle(
        val metadata: Metadata = Metadata(),
        @XmlSerialName("crossword", PUZZLE_NS, "") val crossword: Crossword? = null,
        @XmlSerialName("acrostic", PUZZLE_NS, "") val acrostic: Crossword? = null
    ) {

        @Serializable
        @SerialName("metadata")
        data class Metadata(
            @SerialName("title") @XmlElement(true) val title: String? = null,
            @SerialName("creator") @XmlElement(true) val creator: String? = null,
            @SerialName("copyright") @XmlElement(true) val copyright: String? = null,
            @SerialName("description") @XmlElement(true) val description: String? = null
        )

        @Serializable
        data class Crossword(val grid: Grid, val words: List<Word>, val clues: List<Clues>) {

            @Serializable
            @SerialName("grid")
            data class Grid(
                @SerialName("width") val width: Int,
                @SerialName("height") val height: Int,
                val gridLook: GridLook = GridLook(),
                val cell: List<Cell>
            ) {

                @Serializable
                @SerialName("grid-look")
                data class GridLook(@SerialName("numbering-scheme") val numberingScheme: String = "normal")

                @Serializable
                @SerialName("cell")
                data class Cell(
                    @SerialName("x") val x: Int,
                    @SerialName("y") val y: Int,
                    @SerialName("solution") val solution: String? = null,
                    @SerialName("foreground-color") val foregroundColor: String? = null,
                    @SerialName("background-color") val backgroundColor: String? = null,
                    @SerialName("number") val number: String? = null,
                    @SerialName("type") val type: String? = null,
                    @SerialName("solve-state") val solveState: String? = null,
                    @SerialName("top-right-number") val topRightNumber: String? = null,
                    @SerialName("background-shape") val backgroundShape: String? = null,
                    @SerialName("top-bar") val topBar: Boolean? = null,
                    @SerialName("left-bar") val leftBar: Boolean? = null,
                    @SerialName("right-bar") val rightBar: Boolean? = null,
                    @SerialName("bottom-bar") val bottomBar: Boolean? = null
                )
            }

            @Serializable
            @SerialName("word")
            data class Word(
                @SerialName("id") val id: Int,
                val cells: List<Cells>
            ) {

                @Serializable
                @SerialName("cells")
                data class Cells(val x: Int, val y: Int)
            }

            @Serializable
            @SerialName("clues")
            data class Clues(val title: Title, val clues: List<Clue>) {

                @Serializable
                @SerialName("title")
                data class Title(@XmlValue(true) val data: Snippet)

                @Serializable
                @SerialName("clue")
                data class Clue(
                    @SerialName("word") val word: Int,
                    @SerialName("number") val number: String,
                    @XmlValue(true) val text: Snippet
                )
            }
        }
    }

    @Serializable
    @SerialName("b")
    data class B(@XmlValue(true) val data: Snippet)

    @Serializable
    @SerialName("i")
    data class I(@XmlValue(true) val data: Snippet)

    @Serializable
    @SerialName("sub")
    data class Sub(@XmlValue(true) val data: Snippet)

    @Serializable
    @SerialName("sup")
    data class Sup(@XmlValue(true) val data: Snippet)

    @Serializable
    @SerialName("span")
    data class Span(@XmlValue(true) val data: Snippet)

    suspend fun toCompressedFile(filename: String): ByteArray {
        return Zip.zip(filename, toXmlString().toByteArray(Charsets.UTF_8))
    }

    override fun asCrossword(): Crossword {
        // Extract the grid by first building a map from point to square and then converting it to a list-of-lists.
        require(rectangularPuzzle.crossword != null) {
            "JPZ file does not contain a <crossword> element"
        }
        val width = rectangularPuzzle.crossword!!.grid.width
        val height = rectangularPuzzle.crossword!!.grid.height
        val gridMap: MutableMap<Pair<Int, Int>, Square> = mutableMapOf()
        rectangularPuzzle.crossword!!.grid.cell.forEach {
            val position = Pair(it.x - 1, it.y - 1)
            if (it.type == "block") {
                gridMap[position] = BLACK_SQUARE.copy(backgroundColor = it.backgroundColor)
            } else {
                val solution = it.solution ?: ""
                val solutionRebus = if (solution.length > 1) solution else ""
                val isCircled =
                    "circle".equals(it.backgroundShape, ignoreCase = true)
                gridMap[position] = Square(
                    solution = solution[0],
                    solutionRebus = solutionRebus,
                    isCircled = isCircled,
                    number = if (it.number?.isNotEmpty() == true) it.number.toInt() else null,
                    foregroundColor = it.foregroundColor,
                    backgroundColor = it.backgroundColor,
                )
            }
        }
        val grid: MutableList<MutableList<Square>> = mutableListOf()
        for (y in 0 until height) {
            val row = mutableListOf<Square>()
            for (x in 0 until width) {
                row.add(gridMap[Pair(x, y)]!!)
            }
            grid.add(row)
        }

        val (acrossClues, downClues) = buildClueMaps(rectangularPuzzle.crossword!!.clues)

        return Crossword(
            title = rectangularPuzzle.metadata.title ?: "",
            author = rectangularPuzzle.metadata.creator ?: "",
            copyright = rectangularPuzzle.metadata.copyright ?: "",
            notes = rectangularPuzzle.metadata.description ?: "",
            grid = grid,
            acrossClues = acrossClues,
            downClues = downClues,
            hasHtmlClues = true,
        )
    }

    /** Convert the given list of <clues> elements into across and down clues lists. */
    private fun buildClueMaps(clues: List<RectangularPuzzle.Crossword.Clues>):
            Pair<Map<Int, String>, Map<Int, String>> {
        // Create a map from clue list title to the list of <clue> elements under that title.
        val clueGroups = clues.filter { it.title.data.isNotEmpty() }.associate {
            val clueListTitle = it.title.data.toText().toLowerCase()
            val clueList = it.clues
            clueListTitle to clueList
        }

        // Convert the <clue> element lists for across/down clues into the expected map format.
        val acrossClues =
            (clueGroups["across"] ?: error("No Across clues")).associate { it.number.toInt() to it.text.toHtml() }
        val downClues =
            (clueGroups["down"] ?: error("No Down clues")).associate { it.number.toInt() to it.text.toHtml() }

        return acrossClues to downClues
    }

    private fun Snippet.toHtml(): String {
        return joinToString("") {
            when (it) {
                is String -> it.replace("&", "&amp;").replace("<", "&lt;")
                is B -> "<b>${it.data.toHtml()}</b>"
                is I -> "<i>${it.data.toHtml()}</i>"
                is Sub -> "<sub>${it.data.toHtml()}</sub>"
                is Sup -> "<sup>${it.data.toHtml()}</sup>"
                is Span -> "<span>${it.data.toHtml()}</span>"
                else -> throw IllegalStateException("Unknown data type: $it")
            }
        }.trim()
    }

    private fun Snippet.toText(): String {
        return joinToString("") {
            when (it) {
                is String -> it.replace("&", "&amp;").replace("<", "&lt;")
                is B -> it.data.toText()
                is I -> it.data.toText()
                is Sub -> it.data.toText()
                is Sup -> it.data.toText()
                is Span -> it.data.toText()
                else -> throw IllegalStateException("Unknown data type: $it")
            }
        }.trim()
    }

    companion object {
        /**
         * Serialize this crossword into a JPZ document.
         *
         * @param solved If true, the grid will be filled in with the correct solution.
         */
        fun Crossword.toJpz(solved: Boolean = false): Jpz {
            return Puzzle.fromCrossword(this).asJpzFile(solved = solved)
        }

        fun fromXmlString(xml: String): Jpz {
            // Try to parse as a <crossword-compiler-applet>; if it fails, fall back to <crossword-compiler>.
            return try {
                XmlSerializer.decodeFromString(CrosswordCompilerApplet.serializer(), xml)
            } catch (e: XmlException) {
                XmlSerializer.decodeFromString(CrosswordCompiler.serializer(), xml)
            }
        }

        internal fun module(): SerializersModule {
            return SerializersModule {
                // Snippet tags
                polymorphic(Any::class, String::class, String.serializer())
                polymorphic(Any::class, B::class, B.serializer())
                polymorphic(Any::class, I::class, I.serializer())
                polymorphic(Any::class, Sub::class, Sub.serializer())
                polymorphic(Any::class, Sup::class, Sup.serializer())
                polymorphic(Any::class, Span::class, Span.serializer())
            }
        }

        @Serializable
        @SerialName("dummy")
        private data class Dummy(@XmlValue(true) val data: Snippet)

        /** Parse the given HTML string as a [Snippet] (i.e. for use in clues). */
        internal fun htmlToSnippet(html: String): Snippet {
            val dummyXml = "<dummy>$html</dummy>"
            val snippet = XML(module()) {
                autoPolymorphic = true
            }.decodeFromString(Dummy.serializer(), dummyXml).data
            // For better compatibility with Crossword Solver, wrap plain text in spans whenever a snippet has HTML
            // children.
            if (snippet.filterNot { it is String }.isEmpty()) {
                return snippet
            }
            return snippet.map {
                if (it is String) {
                    Span(listOf(it))
                } else {
                    it
                }
            }
        }
    }
}

@Serializable
@XmlSerialName("crossword-compiler", "http://crossword.info/xml/crossword-compiler", "")
data class CrosswordCompiler(
    override val rectangularPuzzle: Jpz.RectangularPuzzle
) : Jpz {

    override fun toXmlString(): String {
        return XmlSerializer.encodeToString(serializer(), this)
    }
}

@Serializable
@XmlSerialName("crossword-compiler-applet", CCA_NS, "")
data class CrosswordCompilerApplet(
    val appletSettings: AppletSettings? = null,
    override val rectangularPuzzle: Jpz.RectangularPuzzle
) : Jpz {

    @Serializable
    @SerialName("applet-settings")
    data class AppletSettings(
        @SerialName("cursor-color") val cursorColor: String = "#00B100",
        @SerialName("selected-cells-color") val selectedCellsColor: String = "#80FF80",
        val completion: Completion,
        val actions: Actions = Actions()
    ) {

        @Serializable
        @SerialName("completion")
        data class Completion(
            @XmlValue(true) val message: String,
            @SerialName("only-if-correct") val onlyIfCorrect: Boolean = true
        )

        @Serializable
        @SerialName("actions")
        data class Actions(
            @SerialName("buttons-layout") val buttonsLayout: String = "left",
            @XmlSerialName("reveal-word", CCA_NS, "") val revealWord: Action? = Action("Reveal Word"),
            @XmlSerialName("reveal-letter", CCA_NS, "") val revealLetter: Action? = Action("Reveal Letter"),
            @XmlSerialName("check", CCA_NS, "") val check: Action? = Action("Check"),
            @XmlSerialName("solution", CCA_NS, "") val solution: Action? = Action("Solution"),
            @XmlSerialName("pencil", CCA_NS, "") val pencil: Action? = Action("Pencil")
        ) {

            @Serializable
            data class Action(@SerialName("label") val label: String)
        }
    }

    override fun toXmlString(): String {
        return XmlSerializer.encodeToString(serializer(), this)
    }
}