package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.json.JsonSerializer
import com.jeffpdavidson.kotwords.formats.json.PuzzleMeJson
import com.jeffpdavidson.kotwords.model.BLACK_SQUARE
import com.jeffpdavidson.kotwords.model.Crossword
import com.jeffpdavidson.kotwords.model.Square
import org.jsoup.Jsoup
import java.nio.charset.StandardCharsets
import java.util.Base64

private val PUZZLE_DATA_REGEX = """\bwindow\.rawc\s*=\s*'([^']+)'""".toRegex()

/** Container for a puzzle in the PuzzleMe (Amuse Labs) format. */
class PuzzleMe(private val html: String) : Crosswordable {

    override fun asCrossword(): Crossword {
        return toCrossword(extractPuzzleJson(html))
    }

    companion object {
        internal fun extractPuzzleJson(html: String): String {
            // Look for "window.rawc = '[data]'" inside <script> tags; this is JSON puzzle data
            // encoded as Base64.
            Jsoup.parse(html).getElementsByTag("script").forEach {
                val matchResult = PUZZLE_DATA_REGEX.find(it.data())
                if (matchResult != null) {
                    return String(
                            Base64.getDecoder().decode(matchResult.groupValues[1]),
                            StandardCharsets.UTF_8)
                }
            }
            throw InvalidFormatException("Could not find puzzle data in PuzzleMe HTML")
        }

        internal fun toCrossword(json: String): Crossword {
            val data = JsonSerializer.fromJson(PuzzleMeJson.Data::class.java, json)
            val grid: MutableList<MutableList<Square>> = mutableListOf()

            // PuzzleMe supports circled cells, cells with special background shapes, and different
            // background colors per cell, but Across Lite only supports circled cells. We pick one
            // mechanism to map to circles (preferring "isCircled" which is a direct match) and
            // ignore any others.
            val circledCells =
                    if (data.cellInfos.find { it.isCircled } != null) {
                        data.cellInfos.filter { it.isCircled }.map { it.x to it.y }
                    } else if (!data.backgroundShapeBoxes.isEmpty()) {
                        data.backgroundShapeBoxes.filter { it.size == 2 }.map { it[0] to it[1] }
                    } else {
                        // Note that if there are multiple distinct colors, all of them will be
                        // mapped to circles.
                        data.cellInfos.filter { it.bgColor != "" }.map { it.x to it.y }
                    }

            // If bgColor == fgColor, assume the square is meant to be hidden/black and revealed after solving.
            val voidCells = data.cellInfos
                    .filter { it.isVoid || (it.bgColor.isNotEmpty() && it.bgColor == it.fgColor) }.map { it.x to it.y }

            for (y in 0 until data.box[0].size) {
                val row: MutableList<Square> = mutableListOf()
                for (x in 0 until data.box.size) {
                    // Treat black squares, void squares, and squares with no intersecting words that aren't pre-filled
                    // (which likely means they're meant to be revealed after solving) as black squares.
                    if (data.box[x][y] == "\u0000" ||
                            voidCells.contains(x to y) ||
                            (data.boxToPlacedWordsIdxs.isNotEmpty() && data.boxToPlacedWordsIdxs[x][y] == null &&
                                    (data.preRevealIdxs.isEmpty() || !data.preRevealIdxs[x][y]))) {
                        row.add(BLACK_SQUARE)
                    } else {
                        val solutionRebus = if (data.box[x][y].length > 1) data.box[x][y] else ""
                        val isCircled = circledCells.contains(x to y)
                        val isPrefilled = data.preRevealIdxs.isNotEmpty() && data.preRevealIdxs[x][y]
                        row.add(Square(
                                solution = data.box[x][y][0],
                                solutionRebus = solutionRebus,
                                isCircled = isCircled,
                                entry = if (isPrefilled) data.box[x][y][0] else null,
                                isGiven = isPrefilled))
                    }
                }
                if (grid.size > 0 && grid[0].size != row.size) {
                    throw InvalidFormatException("Grid is not square")
                }
                grid.add(row)
            }

            // Void cells can lead to entirely black rows/columns on the outer edges. Delete these.
            val anyNonBlackSquare =  { row: List<Square> -> row.any { it != BLACK_SQUARE } }
            val topRowsToDelete = grid.indexOfFirst(anyNonBlackSquare)
            val bottomRowsToDelete = grid.size - grid.indexOfLast(anyNonBlackSquare) - 1
            val leftRowsToDelete =
                    grid.filter(anyNonBlackSquare).map { row -> row.indexOfFirst { it != BLACK_SQUARE } }.min()!!
            val rightRowsToDelete = grid[0].size -
                    grid.filter(anyNonBlackSquare).map { row -> row.indexOfLast { it != BLACK_SQUARE } }.max()!! - 1
            val filteredGrid = grid.drop(topRowsToDelete).dropLast(bottomRowsToDelete)
                    .map { row -> row.drop(leftRowsToDelete).dropLast(rightRowsToDelete) }

            // Sanitize clues since PuzzleMe grids can have non-standard numbering.
            var sanitizedClues =
                    buildClueMap(data.placedWords.filter { it.acrossNotDown }) to
                            buildClueMap(data.placedWords.filter { !it.acrossNotDown })
            if (data.clueNums.isNotEmpty()) {
                val filteredClueNums = data.clueNums.drop(leftRowsToDelete).dropLast(rightRowsToDelete)
                        .map { col -> col.drop(topRowsToDelete).dropLast(bottomRowsToDelete) }
                sanitizedClues = ClueSanitizer.sanitizeClues(
                        filteredGrid,
                        filteredClueNums.mapIndexed { x, col ->
                            col.mapIndexed { y, clueNum ->
                                (x to y) to clueNum
                            }
                        }.flatten().toMap(),
                        sanitizedClues.first,
                        sanitizedClues.second)
            }

            return Crossword(
                    title = data.title,
                    author = data.author,
                    copyright = data.copyright,
                    notes = data.description,
                    grid = filteredGrid,
                    acrossClues = sanitizedClues.first,
                    downClues = sanitizedClues.second
            )
        }

        private val clueReplacements = mapOf(
                "</?i>" to "\"",
                "</?span>" to "",
                "ł" to "l",
                "[ăā]" to "a",
                "Ō" to "O",
                "ę" to "e",
                "[Αα]" to "[Alpha]",
                "[Ββ]" to "[Beta]",
                "[Γγ]" to "[Gamma]",
                "[Δδ]" to "[Delta]",
                "[Εε]" to "[Epsilon]",
                "[Ζζ]" to "[Zeta]",
                "[Ηη]" to "[Eta]",
                "[Θθ]" to "[Theta]",
                "[Ιι]" to "[Iota]",
                "[Κκ]" to "[Kappa]",
                "[Λλ]" to "[Lambda]",
                "[Μμ]" to "[Mu]",
                "[Νν]" to "[Nu]",
                "[Ξξ]" to "[Xi]",
                "[Οο]" to "[Omicron]",
                "[Ππ]" to "[Pi]",
                "[Ρρ]" to "[Rho]",
                "[Σσς]" to "[Sigma]",
                "[Ττ]" to "[Tau]",
                "[Υυ]" to "[Upsilon]",
                "[Φφ]" to "[Phi]",
                "[Χχ]" to "[Chi]",
                "[Ψψ]" to "[Psi]",
                "[Ωω]" to "[Omega]")
                .map { (key, value) -> key.toRegex() to value }.toMap()

        private fun buildClueMap(clueList: List<PuzzleMeJson.PlacedWord>): Map<Int, String> {
            return clueList
                    .map {
                        // TODO(#2): Generalize and centralize accented character replacement.
                        it.clueNum to
                                clueReplacements.entries.fold(it.clue.clue) { clue, (from, to) ->
                                    clue.replace(from, to)
                                }
                    }
                    .toMap()
        }
    }
}
