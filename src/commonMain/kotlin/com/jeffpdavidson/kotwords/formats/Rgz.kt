package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.model.RowsGarden
import kotlinx.serialization.Serializable
import net.mamoe.yamlkt.Yaml

/** Container for a Rows Garden puzzle in the .rgz format. */
class Rgz(val rg: String) : DelegatingPuzzleable() {
    override suspend fun getPuzzleable() = asRowsGarden()

    @Serializable
    private data class RgzYaml(
        val title: String,
        val author: String,
        val copyright: String,
        val notes: String? = null,
        val rows: List<List<RowsGarden.Entry>>,
        val light: List<RowsGarden.Entry>,
        val medium: List<RowsGarden.Entry>,
        val dark: List<RowsGarden.Entry>,
    )

    fun asRowsGarden(
        lightBloomColor: String = "#FFFFFF",
        mediumBloomColor: String = "#C3C8FA",
        darkBloomColor: String = "#5765F7",
        addWordCount: Boolean = true,
        addHyphenated: Boolean = true,
    ): RowsGarden {
        val rgzYaml = Yaml.Default.decodeFromString(RgzYaml.serializer(), fixInvalidYamlValues(rg))
        return RowsGarden(
            title = rgzYaml.title,
            creator = rgzYaml.author,
            copyright = rgzYaml.copyright.replace("(c)", "©", ignoreCase = true),
            description = rgzYaml.notes ?: "",
            rows = rgzYaml.rows,
            light = rgzYaml.light,
            medium = rgzYaml.medium,
            dark = rgzYaml.dark,
            lightBloomColor = lightBloomColor,
            mediumBloomColor = mediumBloomColor,
            darkBloomColor = darkBloomColor,
            addWordCount = addWordCount,
            addHyphenated = addHyphenated,
        )
    }

    companion object {
        suspend fun fromRgzFile(rgz: ByteArray): Rgz {
            val rg =
                try {
                    Zip.unzip(rgz)
                } catch (e: InvalidZipException) {
                    // Try as a plain-text file.
                    rgz
                }
            var rgString = rg.decodeToString()
            // Strip off BOM from beginning if present.
            if (rgString.startsWith('\uFEFF')) {
                rgString = rgString.substring(1)
            }
            return Rgz(rgString)
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
                        Regex.escapeReplacement(": \"$escapedValue\"")
                    )
                }
            }
        }
    }
}