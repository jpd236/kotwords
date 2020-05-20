package com.jeffpdavidson.kotwords.formats

import kotlinx.io.charsets.Charsets
import kotlinx.io.core.String
import kotlinx.serialization.Serializable
import net.mamoe.yamlkt.Yaml
import kotlin.js.JsName

@Serializable
data class RowsGarden(
        val title: String,
        val author: String,
        val copyright: String,
        val notes: String = "",
        val rows: List<List<Entry>>,
        val light: List<Entry>,
        val medium: List<Entry>,
        val dark: List<Entry>) {
    @Serializable
    data class Entry(val clue: String, val answer: String)

    companion object {
        @JsName("fromRawInput")
        fun fromRawInput(
                title: String,
                author: String,
                copyright: String,
                notes: String,
                rowClues: String,
                rowAnswers: String,
                lightClues: String,
                lightAnswers: String,
                mediumClues: String,
                mediumAnswers: String,
                darkClues: String,
                darkAnswers: String): RowsGarden {
            return RowsGarden(
                    title = title.trim(),
                    author = author.trim(),
                    copyright = copyright.trim(),
                    notes = notes.trim(),
                    rows = rowClues.trim().split("\n").zip(rowAnswers.trim().split("\n"))
                            .map { (clues, answers) ->
                                clues.trim().split("/")
                                        .zip(answers.trim().split("/"))
                                        .map { (clue, answer) -> Entry(clue.trim(), answer.trim()) }
                            },
                    light = lightClues.trim().split("\n").zip(lightAnswers.trim().split("\n"))
                            .map { (clue, answer) -> Entry(clue.trim(), answer.trim()) },
                    medium = mediumClues.trim().split("\n").zip(mediumAnswers.trim().split("\n"))
                            .map { (clue, answer) -> Entry(clue.trim(), answer.trim()) },
                    dark = darkClues.trim().split("\n").zip(darkAnswers.trim().split("\n"))
                            .map { (clue, answer) -> Entry(clue.trim(), answer.trim()) })
        }

        suspend fun parse(rgz: ByteArray): RowsGarden {
            val rg =
                try {
                    Zip.unzip(rgz)
                } catch (e: InvalidZipException) {
                    // Try as a plain-text file.
                    rgz
                }
            return parseRg(String(rg, charset = Charsets.UTF_8))
        }

        private fun parseRg(rg: String): RowsGarden {
            return Yaml.default.parse(serializer(), fixInvalidYamlValues(rg))
        }

        /**
         * Fix invalid values in the provided YAML text.
         *
         * .rg files often contain invalid YAML due to values containing reserved characters. This
         * method attempts to adds quotes around any value, escaping existing quotes as needed.
         */
        private fun fixInvalidYamlValues(yaml: String): String {
            return yaml.lines().joinToString("\n") { line ->
                // Find a value on this line - anything following a ":", ignoring any whitespace at
                // the beginning and end.
                val valuePattern = ":\\s*([^\\s].+[^\\s])\\s*".toRegex()
                val result = valuePattern.find(line)
                if (result == null) {
                    // Nothing found; insert the line as is.
                    line
                } else {
                    // Insert the line with the value surrounded by quotes and any existing quotes
                    // escaped.
                    val escapedValue = result.groupValues[1].replace("\"", "\\\"")
                    line.replace(
                            ":\\s*.+".toRegex(),
                            Regex.escapeReplacement(": \"$escapedValue\""))
                }
            }
        }
    }
}