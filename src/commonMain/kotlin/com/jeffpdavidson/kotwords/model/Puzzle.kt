package com.jeffpdavidson.kotwords.model

import com.jeffpdavidson.kotwords.formats.CrosswordCompiler
import com.jeffpdavidson.kotwords.formats.CrosswordCompilerApplet
import com.jeffpdavidson.kotwords.formats.Jpz

// TODO: Validate data structures.
data class Puzzle(
    val title: String,
    val creator: String,
    val copyright: String,
    val description: String,
    val grid: List<List<Cell>>,
    val clues: List<ClueList>,
    val words: List<Word>,
    val hasHtmlClues: Boolean = false,
    val crosswordSolverSettings: CrosswordSolverSettings? = null,
    val puzzleType: PuzzleType = PuzzleType.CROSSWORD
) {

    data class CrosswordSolverSettings(
        val completionMessage: String = "Congratulations! The puzzle is solved correctly.",
        val cursorColor: String = "#00B100",
        val selectedCellsColor: String = "#80FF80"
    )

    enum class CellType {
        REGULAR,
        BLOCK,
        CLUE,
        VOID
    }

    enum class BackgroundShape {
        NONE,
        CIRCLE
    }

    enum class BorderDirection {
        TOP,
        LEFT,
        RIGHT,
        BOTTOM
    }

    data class Cell(
        val x: Int,
        val y: Int,
        val solution: String = "",
        val foregroundColor: String = "",
        val backgroundColor: String = "",
        val number: String = "",
        val topRightNumber: String = "",
        val cellType: CellType = CellType.REGULAR,
        val backgroundShape: BackgroundShape = BackgroundShape.NONE,
        val borderDirections: Set<BorderDirection> = setOf()
    )

    data class Word(
        val id: Int,
        val cells: List<Cell>
    )

    data class Clue(
        val wordId: Int,
        val number: String,
        val text: String
    )

    data class ClueList(
        val title: String,
        val clues: List<Clue>
    )

    enum class PuzzleType {
        CROSSWORD,
        ACROSTIC
    }

    /**
     * Returns this puzzle as a JPZ file.
     *
     * @param solved If true, the grid will be filled in with the correct solution.
     */
    fun asJpzFile(solved: Boolean = false): Jpz {
        val jpzGrid = Jpz.RectangularPuzzle.Crossword.Grid(
            width = grid[0].size,
            height = grid.size,
            cell = grid.mapIndexed { y, row ->
                row.mapIndexed { x, cell ->
                    val type = when (cell.cellType) {
                        CellType.BLOCK -> "block"
                        CellType.CLUE -> "clue"
                        CellType.VOID -> "void"
                        else -> null
                    }
                    val backgroundShape = if (cell.backgroundShape == BackgroundShape.CIRCLE) "circle" else null
                    // Crossword Solver only renders top and left borders, so if we have a right border, apply it
                    // as a left border on the square to the right (if we're not at the right edge), and if we have
                    // a bottom border, apply it as a top border on the square to the bottom (if we're not at the
                    // bottom edge).
                    val topBorder = cell.borderDirections.contains(BorderDirection.TOP)
                            || (y > 0 && grid[y - 1][x].borderDirections.contains(BorderDirection.BOTTOM))
                    val bottomBorder = cell.borderDirections.contains(BorderDirection.BOTTOM) && y == grid.size - 1
                    val leftBorder = cell.borderDirections.contains(BorderDirection.LEFT)
                            || (x > 0 && grid[y][x - 1].borderDirections.contains(BorderDirection.RIGHT))
                    val rightBorder = cell.borderDirections.contains(BorderDirection.RIGHT) && x == grid[y].size - 1
                    Jpz.RectangularPuzzle.Crossword.Grid.Cell(
                        x = cell.x,
                        y = cell.y,
                        solution = cell.solution.ifEmpty { null },
                        backgroundColor = cell.backgroundColor.ifEmpty { null },
                        number = cell.number.ifEmpty { null },
                        type = type,
                        solveState = if (cell.cellType == CellType.CLUE || solved) cell.solution else null,
                        topRightNumber = cell.topRightNumber.ifEmpty { null },
                        backgroundShape = backgroundShape,
                        topBar = if (topBorder) true else null,
                        bottomBar = if (bottomBorder) true else null,
                        leftBar = if (leftBorder) true else null,
                        rightBar = if (rightBorder) true else null,
                    )
                }
            }.flatten()
        )

        val jpzWords = words.map { word ->
            Jpz.RectangularPuzzle.Crossword.Word(
                id = word.id,
                cells = word.cells.map { cell ->
                    Jpz.RectangularPuzzle.Crossword.Word.Cells(cell.x, cell.y)
                }
            )
        }

        val jpzClues = clues.map { clueList ->
            val title = if (hasHtmlClues) Jpz.htmlToSnippet(clueList.title) else listOf(Jpz.B(listOf(clueList.title)))
            Jpz.RectangularPuzzle.Crossword.Clues(
                title = Jpz.RectangularPuzzle.Crossword.Clues.Title(title),
                clues = clueList.clues.map { clue ->
                    val htmlClues = if (hasHtmlClues) clue.text else formatClue(clue.text)
                    Jpz.RectangularPuzzle.Crossword.Clues.Clue(
                        word = clue.wordId,
                        number = clue.number,
                        text = Jpz.htmlToSnippet(htmlClues)
                    )
                })
        }

        val crossword = Jpz.RectangularPuzzle.Crossword(jpzGrid, jpzWords, jpzClues)

        val rectangularPuzzle = Jpz.RectangularPuzzle(
            metadata = Jpz.RectangularPuzzle.Metadata(
                title = title.ifBlank { null },
                creator = creator.ifBlank { null },
                copyright = copyright.ifBlank { null },
                description = description.ifBlank { null }
            ),
            crossword = if (puzzleType == PuzzleType.CROSSWORD) crossword else null,
            acrostic = if (puzzleType == PuzzleType.ACROSTIC) crossword else null
        )

        return if (crosswordSolverSettings == null) {
            CrosswordCompiler(rectangularPuzzle = rectangularPuzzle)
        } else {
            CrosswordCompilerApplet(
                appletSettings = CrosswordCompilerApplet.AppletSettings(
                    cursorColor = crosswordSolverSettings.cursorColor,
                    selectedCellsColor = crosswordSolverSettings.selectedCellsColor,
                    completion = CrosswordCompilerApplet.AppletSettings.Completion(
                        message = crosswordSolverSettings.completionMessage
                    )
                ),
                rectangularPuzzle = rectangularPuzzle
            )
        }
    }

    companion object {
        fun fromCrossword(
            crossword: Crossword,
            crosswordSolverSettings: CrosswordSolverSettings? = null
        ): Puzzle {
            val gridMap = mutableMapOf<Pair<Int, Int>, Cell>()
            val hasCustomNumbering = Crossword.hasCustomNumbering(crossword.grid)
            Crossword.forEachSquare(crossword.grid) { x, y, clueNumber, _, _, square ->
                if (square.isBlack) {
                    gridMap[x to y] = Cell(
                        x + 1, y + 1,
                        cellType = CellType.BLOCK,
                        backgroundColor = square.backgroundColor ?: ""
                    )
                } else {
                    val solution = square.solutionRebus.ifEmpty { "${square.solution}" }
                    val number =
                        "${
                            if (hasCustomNumbering) {
                                square.number
                            } else {
                                clueNumber
                            } ?: ""
                        }"
                    val backgroundShape =
                        if (square.isCircled) {
                            BackgroundShape.CIRCLE
                        } else {
                            BackgroundShape.NONE
                        }
                    gridMap[x to y] =
                        Cell(
                            x + 1, y + 1,
                            solution = solution,
                            number = number,
                            backgroundShape = backgroundShape,
                            cellType = if (square.isGiven) CellType.CLUE else CellType.REGULAR,
                            foregroundColor = square.foregroundColor ?: "",
                            backgroundColor = square.backgroundColor ?: "",
                            borderDirections = square.borderDirections.mapNotNull { BORDER_DIRECTION_MAP[it] }.toSet()
                        )
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
            val words = mutableListOf<Word>()
            if (crossword.acrossWords.isNotEmpty() && crossword.downWords.isNotEmpty()) {
                // Custom word scheme. Use all of the provided words, and add clue entries for each clue that
                // corresponds to a word. (Some words may be unclued, but all clues must have words - a clue without a
                // word will be dropped).
                crossword.acrossWords.forEach { word ->
                    words.add(Word(word.id, word.squares.map { (x, y) -> grid[y][x] }))
                    val clueNumberString = grid[word.squares[0].second][word.squares[0].first].number
                    if (clueNumberString.isNotEmpty()) {
                        val clueNumber = clueNumberString.toInt()
                        val clueText = crossword.acrossClues[clueNumber]
                        if (clueText != null) {
                            acrossClues.add(
                                Clue(
                                    wordId = word.id,
                                    number = clueNumberString,
                                    text = clueText
                                )
                            )
                        }
                    }
                }
                crossword.downWords.forEach { word ->
                    words.add(Word(word.id, word.squares.map { (x, y) -> grid[y][x] }))
                    val clueNumberString = grid[word.squares[0].second][word.squares[0].first].number
                    if (clueNumberString.isNotEmpty()) {
                        val clueNumber = clueNumberString.toInt()
                        val clueText = crossword.downClues[clueNumber]
                        if (clueText != null) {
                            downClues.add(
                                Clue(
                                    wordId = word.id,
                                    number = clueNumberString,
                                    text = clueText
                                )
                            )
                        }
                    }
                }
            } else {
                // No custom word scheme - generate words based on standard crossword numbering.
                Crossword.forEachSquare(crossword.grid) { x, y, number, isAcross, isDown, square ->
                    val clueNumber = if (hasCustomNumbering) square.number else number
                    if (isAcross) {
                        val word = mutableListOf<Cell>()
                        var i = x
                        while (i < crossword.grid[y].size && !crossword.grid[y][i].isBlack) {
                            word.add(grid[y][i])
                            i++
                        }
                        if (clueNumber != null && crossword.acrossClues.containsKey(clueNumber)) {
                            acrossClues.add(
                                Clue(clueNumber, "$clueNumber", crossword.acrossClues[clueNumber]!!)
                            )
                            words.add(Word(clueNumber, word))
                        }
                    }
                    if (isDown) {
                        val word = mutableListOf<Cell>()
                        var j = y
                        while (j < crossword.grid.size && !crossword.grid[j][x].isBlack) {
                            word.add(grid[j][x])
                            j++
                        }
                        if (clueNumber != null && crossword.downClues.containsKey(clueNumber)) {
                            downClues.add(
                                Clue(1000 + clueNumber, "$clueNumber", crossword.downClues[clueNumber]!!)
                            )
                            words.add(Word(1000 + clueNumber, word))
                        }
                    }
                }
            }

            val acrossTitle = if (crossword.hasHtmlClues) "<b>Across</b>" else "Across"
            val downTitle = if (crossword.hasHtmlClues) "<b>Down</b>" else "Down"
            return Puzzle(
                crossword.title,
                crossword.author,
                crossword.copyright,
                crossword.notes,
                grid,
                listOf(ClueList(acrossTitle, acrossClues), ClueList(downTitle, downClues)),
                words.sortedBy { it.id },
                crosswordSolverSettings = crosswordSolverSettings,
                hasHtmlClues = crossword.hasHtmlClues,
            )
        }

        /**
         * Format a raw clue as valid inner HTML for a JPZ file.
         *
         * <p>Invalid XML characters are escaped, and text surrounded by asterisks is italicized.
         */
        private fun formatClue(rawClue: String): String {
            val escapedClue = rawClue.replace("&", "&amp;").replace("<", "&lt;")
            // Only italicize text if there are an even number of asterisks to try to avoid false positives on text like
            // "M*A*S*H". If this proves to trigger in other unintended circumstances, it may need to be removed from
            // here and applied instead at a higher level where the intent is clearer.
            val asteriskCount = escapedClue.count { it == '*' }
            return if (asteriskCount > 0 && asteriskCount % 2 == 0) {
                escapedClue.replace("\\*([^*]+)\\*".toRegex(), "<i>$1</i>")
            } else {
                escapedClue
            }
        }

        private val BORDER_DIRECTION_MAP = mapOf(
            Square.BorderDirection.TOP to BorderDirection.TOP,
            Square.BorderDirection.BOTTOM to BorderDirection.BOTTOM,
            Square.BorderDirection.LEFT to BorderDirection.LEFT,
            Square.BorderDirection.RIGHT to BorderDirection.RIGHT,
        )
    }
}
