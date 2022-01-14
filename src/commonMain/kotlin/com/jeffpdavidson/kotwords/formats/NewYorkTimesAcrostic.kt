package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.json.JsonSerializer
import com.jeffpdavidson.kotwords.formats.json.NewYorkTimesAcrosticJson
import com.jeffpdavidson.kotwords.model.Acrostic
import okio.ByteString.Companion.decodeBase64

private val PUZZLE_DATA_REGEX = """\bgameData\s*=\s*"([^']+)"""".toRegex()

class NewYorkTimesAcrostic(val json: String) : Puzzleable {

    override suspend fun asPuzzle() = asAcrostic().asPuzzle()

    fun asAcrostic(): Acrostic {
        val response = JsonSerializer.fromJson<NewYorkTimesAcrosticJson.Data>(json)
        val puzzleDataLines = response.puzzleData.lines()
        require(puzzleDataLines.size >= 6) {
            "Invalid puzzle data - expect at least six lines, but found ${puzzleDataLines.size}"
        }

        // Line 1: Solution, cell IDs, clue IDs; no delimiter.
        val gridKeyParts = puzzleDataLines[0].chunked(puzzleDataLines[0].length / 3)
        val solution = gridKeyParts[0]
        val gridKey = mutableMapOf<Char, MutableMap<Char, Int>>()
        gridKeyParts[1]
            .filter { it in 'A'..'Z' }
            .zip(gridKeyParts[2].filter { it in 'A'..'Z' })
            .forEachIndexed { gridIndex, (cellId, clueId) ->
                val wordKey = gridKey.getOrPut(cellId) { mutableMapOf() }
                wordKey[clueId] = gridIndex + 1
            }

        // Line 2: Clues, split by |, with an extra "|" at the end.
        val clues = puzzleDataLines[1].split("|").dropLast(1)

        // Line 3: Title, byline, copyright, split by |
        val meta = puzzleDataLines[2].split("|")
        require(meta.size == 3) {
            "Invalid puzzle data - expected metadata line with three parts, but found ${meta.size}"
        }
        val title = meta[0]
        val creator = listOfNotNull(meta[1], response.puzzleMeta.editor.ifEmpty { null }).joinToString(" / ")
        val copyright = "\u00a9 ${meta[2]}"

        // Lines 4-6: Formatted quote, author, and source.
        val completionMessage = "\"${puzzleDataLines[3]}\" -${puzzleDataLines[4]}, ${puzzleDataLines[5]}"

        return Acrostic(
            title = title,
            creator = creator,
            copyright = copyright,
            description = "",
            suggestedWidth = null,
            solution = solution,
            gridKey = gridKey.keys.sorted().map { wordKey ->
                gridKey[wordKey]!!.entries.sortedBy { it.key }.map { it.value }
            },
            clues = clues,
            completionMessage = completionMessage,
        )
    }

    companion object {
        fun fromHtml(html: String): NewYorkTimesAcrostic = NewYorkTimesAcrostic(extractPuzzleJson(html))

        fun fromGameData(gameData: String): NewYorkTimesAcrostic = NewYorkTimesAcrostic(decodeGameData(gameData))

        internal fun extractPuzzleJson(html: String): String {
            // Look for "gameData = '[data]'" inside <script> tags; this is JSON puzzle data
            // encoded as escaped Base64.
            Xml.parse(html, format = DocumentFormat.HTML).select("script").forEach {
                val matchResult = PUZZLE_DATA_REGEX.find(it.data)
                if (matchResult != null) {
                    return decodeGameData(matchResult.groupValues[1])
                }
            }
            throw InvalidFormatException("Could not find puzzle data in New York Times HTML")
        }

        private fun decodeGameData(gameData: String): String =
            Encodings.unescape(
                gameData.decodeBase64()?.utf8()
                    ?: throw InvalidFormatException("gameData is invalid base64")
            )
    }
}