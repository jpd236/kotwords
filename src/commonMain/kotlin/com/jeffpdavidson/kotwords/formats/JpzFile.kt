package com.jeffpdavidson.kotwords.formats

import kotlinx.io.charsets.Charsets
import kotlinx.io.core.toByteArray
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.modules.SerialModule
import kotlinx.serialization.modules.SerializersModule
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue

private const val CC_NS = "http://crossword.info/xml/crossword-compiler"
private const val PUZZLE_NS = "http://crossword.info/xml/rectangular-puzzle"

// TODO: Is it possible to restrict the types of the elements of this list at compile time?
// See https://github.com/pdvrieze/xmlutil/issues/30
typealias Snippet = List<@Polymorphic Any>

@Serializable
@XmlSerialName("crossword-compiler-applet", CC_NS, "")
data class JpzFile(val appletSettings: AppletSettings, val rectangularPuzzle: RectangularPuzzle) {

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
                @XmlSerialName("reveal-word", CC_NS, "") val revealWord: Action? = Action("Reveal Word"),
                @XmlSerialName("reveal-letter", CC_NS, "") val revealLetter: Action? = Action("Reveal Letter"),
                @XmlSerialName("check", CC_NS, "") val check: Action? = Action("Check"),
                @XmlSerialName("solution", CC_NS, "") val solution: Action? = Action("Solution"),
                @XmlSerialName("pencil", CC_NS, "") val pencil: Action? = Action("Pencil")
        ) {

            @Serializable
            data class Action(@SerialName("label") val label: String)
        }
    }

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
            data class Word(@SerialName("id") val id: Int, val cells: List<Cells>) {

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
    @SerialName("span")
    data class Span(@XmlValue(true) val data: Snippet)

    companion object {
        private fun module(): SerialModule {
            return SerializersModule {
                // Snippet tags
                polymorphic(Any::class) {
                    String::class with String.serializer()
                    B::class with B.serializer()
                    I::class with I.serializer()
                    Span::class with Span.serializer()
                }
            }
        }

        /** Parse the given HTML string as a [Snippet] (i.e. for use in clues). */
        fun htmlToSnippet(html: String): Snippet {
            @Serializable
            @SerialName("dummy")
            data class Dummy(@XmlValue(true) val data: Snippet)

            val dummyXml = "<dummy>$html</dummy>"
            val snippet = XML(module()) {
                autoPolymorphic = true
            }.parse(Dummy.serializer(), dummyXml).data
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

    fun toXmlString(): String {
        return XML(module()) {
            xmlDeclMode = XmlDeclMode.Charset
            autoPolymorphic = true
        }.stringify(serializer(), this)
    }

    suspend fun toCompressedFile(filename: String): ByteArray {
        return Zip.zip(filename, toXmlString().toByteArray(Charsets.UTF_8))
    }
}
