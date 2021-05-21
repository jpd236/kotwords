package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.model.Acrostic
import com.jeffpdavidson.kotwords.model.Puzzle
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement

@Serializable
@SerialName("puzzle")
data class ApzFile(
    val metadata: Metadata,
    @XmlElement(true) val solution: String? = null,
    @XmlElement(true) val source: String? = null,
    @XmlElement(true) val quote: String? = null,
    @XmlElement(true) val gridkey: String? = null,
    @XmlElement(true) val answers: String? = null,
    @XmlElement(true) val clues: String? = null
) {

    @Serializable
    @SerialName("metadata")
    data class Metadata(
        @XmlElement(true) val title: String? = null,
        @XmlElement(true) val creator: String? = null,
        @XmlElement(true) val copyright: String? = null,
        @XmlElement(true) val suggestedwidth: String? = null,
        @XmlElement(true) val description: String? = null
    )


    fun toAcrostic(crosswordSolverSettings: Puzzle.CrosswordSolverSettings): Acrostic {
        val completionMessage =
            crosswordSolverSettings.completionMessage.ifEmpty {
                val source = source ?: ""
                val quote = quote ?: ""
                listOf(source.trim(), quote.trim())
                    .filter { it.isNotEmpty() }.joinToString("\n\n")
            }
        val settings = Puzzle.CrosswordSolverSettings(
            crosswordSolverSettings.cursorColor,
            crosswordSolverSettings.selectedCellsColor,
            completionMessage
        )
        return Acrostic.fromRawInput(
            title = metadata.title ?: "",
            creator = metadata.creator ?: "",
            copyright = metadata.copyright ?: "",
            description = metadata.description ?: "",
            suggestedWidth = metadata.suggestedwidth ?: "",
            solution = solution ?: "",
            gridKey = gridkey ?: "",
            clues = clues ?: "",
            answers = answers ?: "",
            crosswordSolverSettings = settings
        )
    }

    companion object {
        fun parse(apzContents: String): ApzFile {
            return XML {
                // Ignore unknown elements
                unknownChildHandler = { _, _, _, _ -> }
            }.decodeFromString(serializer(), apzContents)
        }
    }
}