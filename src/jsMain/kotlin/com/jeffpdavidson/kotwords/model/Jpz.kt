package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.jslib.JSZip
import com.jeffpdavidson.kotwords.jslib.ZipOutputType
import com.jeffpdavidson.kotwords.jslib.newGenerateAsyncOptions
import org.w3c.dom.parsing.XMLSerializer
import kotlin.browser.document
import kotlin.dom.appendText
import kotlin.js.Promise

// TODO: Figure out how to unify this with the JVM JPZ reader.

data class CrosswordSolverSettings(
        val cursorColor: String,
        val selectedCellsColor: String,
        val completionMessage: String)

enum class CellType {
    REGULAR,
    BLOCK,
    CLUE
}

enum class BackgroundShape {
    NONE,
    CIRCLE
}

data class Cell(
        val x: Int,
        val y: Int,
        val solution: String = "",
        val backgroundColor: String = "",
        val number: String = "",
        val topRightNumber: String = "",
        val cellType: CellType = CellType.REGULAR,
        val backgroundShape: BackgroundShape = BackgroundShape.NONE)

data class Word(
        val id: Int,
        val cells: List<Cell>)

data class Clue(
        val word: Word,
        val number: String,
        val text: String)

data class ClueList(
        val title: String,
        val clues: List<Clue>)

enum class PuzzleType {
    CROSSWORD,
    ACROSTIC
}

data class Jpz(
        val title: String,
        val creator: String,
        val copyright: String,
        val description: String,
        val grid: List<List<Cell>>,
        val clues: List<ClueList>,
        val crosswordSolverSettings: CrosswordSolverSettings,
        val puzzleType: PuzzleType = PuzzleType.CROSSWORD) {
    // TODO: Validate data structures.
    // TODO: Generalize for other puzzle types.

    /** Returns this puzzle as a JPZ XML string. */
    fun asXmlString(): String {
        val doc = document.implementation.createDocument("", "", null)

        doc.appendChild(doc.createProcessingInstruction("xml", "version=\"1.0\""))

        val root = doc.createElement("crossword-compiler-applet")
        root.setAttribute("xmlns", "http://crossword.info/xml/crossword-compiler")

        val appletSettings = doc.createElement("applet-settings")
        appletSettings.setAttribute("cursor-color", crosswordSolverSettings.cursorColor)
        appletSettings.setAttribute("selected-cells-color",
                crosswordSolverSettings.selectedCellsColor)
        val completion = doc.createElement("completion")
        completion.setAttribute("friendly-submit", "false")
        completion.setAttribute("only-if-correct", "true")
        completion.appendText(crosswordSolverSettings.completionMessage)
        appletSettings.appendChild(completion)
        val actions = doc.createElement("actions")
        actions.setAttribute("graphical-buttons", "false")
        actions.setAttribute("wide-buttons", "false")
        actions.setAttribute("buttons-layout", "left")
        val revealWord = doc.createElement("reveal-word")
        revealWord.setAttribute("label", "Reveal Word")
        actions.appendChild(revealWord)
        val revealLetter = doc.createElement("reveal-letter")
        revealLetter.setAttribute("label", "Reveal Letter")
        actions.appendChild(revealLetter)
        val check = doc.createElement("check")
        check.setAttribute("label", "Check")
        actions.appendChild(check)
        val solution = doc.createElement("solution")
        solution.setAttribute("label", "Solution")
        actions.appendChild(solution)
        val pencil = doc.createElement("pencil")
        pencil.setAttribute("label", "Pencil")
        actions.appendChild(pencil)
        appletSettings.appendChild(actions)
        root.appendChild(appletSettings)

        val rectangularPuzzle = doc.createElement("rectangular-puzzle")
        rectangularPuzzle.setAttribute("xmlns", "http://crossword.info/xml/rectangular-puzzle")
        rectangularPuzzle.setAttribute("alphabet", "ABCDEFGHIJKLMNOPQRSTUVWXYZ")

        val metadata = doc.createElement("metadata")
        if (!title.isBlank()) {
            metadata.appendChild(doc.createElement("title").appendText(title))
        }
        if (!creator.isBlank()) {
            metadata.appendChild(doc.createElement("creator").appendText(creator))
        }
        if (!copyright.isBlank()) {
            metadata.appendChild(doc.createElement("copyright").appendText(copyright))
        }
        if (!description.isBlank()) {
            metadata.appendChild(doc.createElement("description").appendText(description))
        }
        rectangularPuzzle.appendChild(metadata)

        val crossword = doc.createElement(when(puzzleType) {
            PuzzleType.CROSSWORD -> "crossword"
            PuzzleType.ACROSTIC -> "acrostic"
        })
        val gridElem = doc.createElement("grid")
        gridElem.setAttribute("width", "${grid[0].size}")
        gridElem.setAttribute("height", "${grid.size}")
        val gridLook = doc.createElement("grid-look")
        gridLook.setAttribute("numbering-scheme", "normal")
        gridElem.appendChild(gridLook)
        grid.forEach { row ->
            row.forEach { cell ->
                val cellElem = doc.createElement("cell")
                cellElem.setAttribute("x", "${cell.x}")
                cellElem.setAttribute("y", "${cell.y}")
                if (cell.solution != "") {
                    cellElem.setAttribute("solution", cell.solution)
                }
                if (cell.backgroundColor != "") {
                    cellElem.setAttribute("background-color", cell.backgroundColor)
                }
                if (cell.number != "") {
                    cellElem.setAttribute("number", cell.number)
                }
                when (cell.cellType) {
                    CellType.BLOCK -> cellElem.setAttribute("type", "block")
                    CellType.CLUE -> {
                        cellElem.setAttribute("type", "clue")
                        cellElem.setAttribute("solve-state", cell.solution)
                    }
                    CellType.REGULAR -> {}
                }
                if (cell.topRightNumber != "") {
                    cellElem.setAttribute("top-right-number", cell.topRightNumber)
                }
                if (cell.backgroundShape == BackgroundShape.CIRCLE) {
                    cellElem.setAttribute("background-shape", "circle")
                }
                gridElem.appendChild(cellElem)
            }
        }
        crossword.appendChild(gridElem)

        clues.forEach { clueList ->
            clueList.clues.forEach { clue ->
                val wordElem = doc.createElement("word")
                wordElem.setAttribute("id", "${clue.word.id}")
                clue.word.cells.forEach { cell ->
                    val cellsElem = doc.createElement("cells")
                    cellsElem.setAttribute("x", "${cell.x}")
                    cellsElem.setAttribute("y", "${cell.y}")
                    wordElem.appendChild(cellsElem)
                }
                crossword.appendChild(wordElem)
            }
        }

        clues.forEach { clueList ->
            val cluesElem = doc.createElement("clues")
            cluesElem.setAttribute("ordering", "normal")
            val titleElem = doc.createElement("title")
            titleElem.appendChild(doc.createElement("b").appendText(clueList.title))
            cluesElem.appendChild(titleElem)
            clueList.clues.forEach { clue ->
                val clueElem = doc.createElement("clue")
                clueElem.setAttribute("word", "${clue.word.id}")
                clueElem.setAttribute("number", clue.number)
                // TODO: Handle HTML in clues
                clueElem.appendText(clue.text)
                cluesElem.appendChild(clueElem)
            }
            crossword.appendChild(cluesElem)
        }

        rectangularPuzzle.appendChild(crossword)
        root.appendChild(rectangularPuzzle)
        doc.appendChild(root)

        return XMLSerializer().serializeToString(doc)
    }

    /** Returns this puzzle as a Base64-encoded JPZ zip file. */
    fun asBase64JpzZip(): Promise<String> {
        val zip = JSZip()
        zip.file("${title.replace("[^A-Za-z0-9]".toRegex(), "")}.xml", asXmlString())
        val options = newGenerateAsyncOptions(type = ZipOutputType.BASE64)
        return zip.generateAsync(options).unsafeCast<Promise<String>>()
    }

    companion object {
        @JsName("fromCrossword")
        fun fromCrossword(crossword: Crossword,
                          crosswordSolverSettings: CrosswordSolverSettings): Jpz {
            val gridMap = mutableMapOf<Pair<Int, Int>, Cell>()
            Crossword.forEachSquare(crossword.grid) { x, y, clueNumber, _, _, square ->
                if (square == BLACK_SQUARE) {
                    gridMap[x to y] = Cell(x + 1, y + 1, cellType = CellType.BLOCK)
                } else {
                    val solution =
                            if (square.solutionRebus.isEmpty()) {
                                "${square.solution}"
                            } else {
                                square.solutionRebus
                            }
                    val number = if (clueNumber != null) "$clueNumber" else ""
                    val backgroundShape =
                            if (square.isCircled) {
                                BackgroundShape.CIRCLE
                            } else {
                                BackgroundShape.NONE
                            }
                    gridMap[x to y] =
                            Cell(x + 1, y + 1,
                                    solution = solution,
                                    number = number,
                                    backgroundShape = backgroundShape)
                }
            }
            val grid = mutableListOf<List<Cell>>()
            crossword.grid.indices.forEach { y ->
                val row = mutableListOf<Cell>()
                crossword.grid[y].indices.forEach { x ->
                    row.add(gridMap[x to y]!!)
                }
                grid.add(row)
            }

            val acrossClues = mutableListOf<Clue>()
            val downClues = mutableListOf<Clue>()
            Crossword.forEachNumberedSquare(crossword.grid) { x, y, number, isAcross, isDown ->
                if (isAcross) {
                    val word = mutableListOf<Cell>()
                    var i = x
                    while (i < crossword.grid[y].size && crossword.grid[y][i] != BLACK_SQUARE) {
                        word.add(grid[y][i])
                        i++
                    }
                    acrossClues.add(Clue(Word(number, word), "$number",
                            crossword.acrossClues[number] ?:
                            error("No across clue for number $number")))
                }
                if (isDown) {
                    val word = mutableListOf<Cell>()
                    var j = y
                    while (j < crossword.grid.size && crossword.grid[j][x] != BLACK_SQUARE) {
                        word.add(grid[j][x])
                        j++
                    }
                    downClues.add(Clue(Word(1000 + number, word), "$number",
                            crossword.downClues[number] ?:
                            error("No down clue for number $number")))
                }
            }

            return Jpz(
                    crossword.title,
                    crossword.author,
                    crossword.copyright,
                    crossword.notes,
                    grid,
                    listOf(ClueList("Across", acrossClues), ClueList("Down", downClues)),
                    crosswordSolverSettings)
        }
    }
}