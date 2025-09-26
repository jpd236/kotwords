package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.model.Crossword
import com.jeffpdavidson.kotwords.model.Puzzle
import com.jeffpdavidson.kotwords.util.trimmedLines

/**
 * Container for a puzzle in the .xd format.
 *
 * See:
 * -  https://github.com/century-arcade/xd/blob/master/doc/xd-format.md
 * -  https://github.com/puzzmo-com/xd-crossword-tools
 */
class Xd(private val data: String) : DelegatingPuzzleable() {

    override suspend fun getPuzzleable(): Puzzleable {
        // Parse each section into a map from the section name to the non-empty lines of that section.
        val sections = mutableMapOf<String, List<String>>()
        var currentSection = mutableListOf<String>()
        var currentSectionName = ""
        val lines = data.trimmedLines()
        var inComment = false
        lines.forEachIndexed { i, line ->
            val isSectionStart = line.startsWith("## ")
            val isFileEnd = i == lines.size - 1
            if (!line.isBlank() && (!isSectionStart || isFileEnd)) {
                if (line.startsWith("<!--")) {
                    inComment = true
                }
                if (!inComment) {
                    currentSection.add(line)
                }
                if (line.endsWith("-->")) {
                    inComment = false
                }
            }
            if (isSectionStart || isFileEnd) {
                if (currentSectionName.isNotEmpty()) {
                    sections.put(currentSectionName, currentSection.toList())
                }
                if (isSectionStart) {
                    currentSectionName = line.substring(3).lowercase()
                    currentSection.clear()
                }
            }
        }

        val metadata = sections
            .getOrElse("metadata") { listOf<String>() }
            .map { it.split(": ", limit = 2) }
            .filter { it.size == 2 }
            .associate { it[0] to it[1] }

        val clues = sections.getOrElse("clues") { listOf<String>() }.partition { it.startsWith("A") }
        val acrossClues = toClueMap(clues.first)
        val downClues = toClueMap(clues.second)

        // Description can either come from "description" metadata or the Notes section.
        val description =
            (metadata["description"] ?: "").ifBlank { sections.getOrElse("notes") { listOf() }.joinToString("\n") }

        // Map from special characters in the grid to the rebus entry that should go in those cells.
        val rebusMap = (metadata["rebus"] ?: "").split(" ").associate {
            it.substringBefore('=') to it.substringAfter('=')
        }

        // The design section has a list of styles keyed by characters, followed by a grid with some cells marked by
        // those character keys, indicating the styles to apply to those cells.
        val design = sections.getOrElse("design") { listOf<String>() }
        val styleMap = mutableMapOf<Pair<Int, Int>, Map<String, String>>()
        if (!design.isEmpty()) {
            // Styles are between <style> and </style> tags.
            val styleSection = design.joinToString("").substringAfter("<style>").substringBefore("</style>")
            val styles = "\\s*(.)\\s*\\{([^}]*)}".toRegex().findAll(styleSection).associate { style ->
                style.groupValues[1][0] to style.groupValues[2].trim().split(";\\s*".toRegex()).associate {
                    val key = it.substringBefore(':').trim()
                    val value = it.substringAfter(':').trim()
                    key to value
                }
            }

            // Remaining lines are the grid itself.
            design.subList(design.indexOfFirst { it.contains("</style>") } + 1, design.size).forEachIndexed { y, row ->
                row.forEachIndexed { x, ch ->
                    styles[ch]?.let {
                        styleMap[x to y] = it
                    }
                }
            }
        }

        // The start section consists of squares to prefill.
        val start = sections.getOrElse("start") { listOf<String>() }
        val startMap = mutableMapOf<Pair<Int, Int>, Char>()
        if (!start.isEmpty()) {
            start.forEachIndexed { y, row ->
                row.forEachIndexed { x, ch ->
                    if (ch != '#' && ch != '.') {
                        startMap[x to y] = ch
                    }
                }
            }
        }

        return Crossword(
            title = metadata["title"]?.toHtml() ?: "",
            creator = metadata["author"]?.toHtml() ?: "",
            copyright = metadata["copyright"]?.toHtml() ?: "",
            description = description.toHtml(),
            grid = sections.getOrElse("grid") { listOf<String>() }.mapIndexed { y, row ->
                row.mapIndexed { x, ch ->
                    if (ch == '.') {
                        Puzzle.Cell(cellType = Puzzle.CellType.BLOCK)
                    } else {
                        val solutionChar = "${ch.uppercase()}"
                        val solution = rebusMap.getOrElse(solutionChar) { solutionChar }
                        val style = styleMap[x to y] ?: mapOf()
                        val backgroundShape = if (style["background"] == "circle" || ch.isLowerCase()) {
                            Puzzle.BackgroundShape.CIRCLE
                        } else {
                            Puzzle.BackgroundShape.NONE
                        }
                        val borderDirections = setOfNotNull(
                            if (style["bar-top"] == "true") Puzzle.BorderDirection.TOP else null,
                            if (style["bar-left"] == "true") Puzzle.BorderDirection.LEFT else null,
                        )
                        Puzzle.Cell(
                            solution = solution,
                            backgroundShape = backgroundShape,
                            backgroundColor = style["background-light"] ?: "",
                            borderDirections = borderDirections,
                            cellType = if (startMap[x to y] == ch) Puzzle.CellType.CLUE else Puzzle.CellType.REGULAR,
                        )
                    }
                }
            },
            acrossClues = acrossClues,
            downClues = downClues,
            hasHtmlClues = true,
        )
    }

    private fun toClueMap(clues: List<String>): Map<Int, String> {
        return clues.filter { clue ->
            // Filter out alternative clues.
            clue.matches("^[AD][0-9]+. .*$".toRegex())
        }.associate { clue ->
            val number = clue.substringBefore(". ").substring(1).toInt()
            val clueText = clue.substringAfter(". ").substringBeforeLast(" ~ ")
            number to clueText.toHtml()
        }
    }

    private fun String.toHtml(): String {
        return this
            // Escape HTML entities
            .replace("&", "&amp;").replace("<", "&lt;")
            // Supported HTML tags
            .replace("{/", "<i>").replace("/}", "</i>")
            .replace("{*", "<b>").replace("*}", "</b>")
            .replace("{~", "<sub>").replace("~}", "</sub>")
            .replace("{^", "<sup>").replace("^}", "</sup>")
            // Strikethrough
            .replace("{-", "---").replace("-}", "---")
            // Links - extract just the text
            .replace("\\{@ ([^|]*) \\| [^}]* @\\}".toRegex(), "$1")
            // Images - extract just the alt text
            .replace("\\{!!? [^|]* \\| ([^|]*) \\| [^}]* !!?\\}".toRegex(), "$1")
            // Colored text - extract just the text
            .replace("\\{# ([^|]*) \\| [^}]* #\\}".toRegex(), "$1")
    }
}