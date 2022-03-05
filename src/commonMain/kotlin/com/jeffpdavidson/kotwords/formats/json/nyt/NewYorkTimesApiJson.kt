package com.jeffpdavidson.kotwords.formats.json.nyt

import com.jeffpdavidson.kotwords.formats.json.JsonSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** JSON for NewYorkTimes puzzles, as obtained from the svc/crosswords/v6/puzzle API. */
internal object NewYorkTimesApiJson {

    @Serializable
    data class Dimensions(val height: Int, val width: Int)

    @Serializable
    data class Overlays(val beforeStart: Int? = null)

    @Serializable
    data class Text(
        val formatted: String = "",
        val plain: String = "",
    )

    @Serializable
    data class Clue(
        val cells: List<Int>,
        val label: String,
        val text: List<Text>,
    )

    @Serializable
    data class Attribute(
        val name: String,
        val value: String,
    )

    @Serializable
    data class XmlElement(
        val name: String = "",
        val attributes: List<Attribute> = listOf(),
        val children: List<XmlElement> = listOf(),
    ) {
        fun toStandardXmlElement(): NewYorkTimesJson.XmlElement = NewYorkTimesJson.XmlElement(
            name = name,
            attributes = attributes.associate { it.name to it.value },
            children = children.map { it.toStandardXmlElement() },
        )
    }

    @Serializable
    data class Body(
        val dimensions: Dimensions,
        val cells: List<NewYorkTimesJson.Cell>,
        val clueLists: List<NewYorkTimesJson.ClueList>,
        val clues: List<Clue>,
        @SerialName("SVG") val svg: XmlElement = XmlElement(),
        val overlays: Overlays = Overlays(),
    )

    @Serializable
    data class Asset(val uri: String)

    @Serializable
    data class Data(
        val assets: List<Asset> = listOf(),
        val body: List<Body>,
        val publicationDate: String,
        val title: String = "",
        val editor: String = "",
        val copyright: String,
        val constructors: List<String>,
        val notes: List<NewYorkTimesJson.Note>? = listOf(),
    )

    fun parse(json: String, stream: String): NewYorkTimesJson {
        val data = JsonSerializer.fromJson<Data>(json)
        val body = data.body[0]
        return object : NewYorkTimesJson {
            override val publicationDate: String = data.publicationDate
            override val publishStream: String = stream
            override val height: Int = body.dimensions.height
            override val width: Int = body.dimensions.width
            override val cells: List<NewYorkTimesJson.Cell> = body.cells
            override val notes: List<NewYorkTimesJson.Note>? = data.notes
            override val title: String = data.title
            override val constructors: List<String> = data.constructors
            override val editor: String = data.editor
            override val copyright: String = data.copyright
            override val clueLists: List<NewYorkTimesJson.ClueList> = body.clueLists
            override val board: NewYorkTimesJson.XmlElement = body.svg.toStandardXmlElement()

            override val clues: List<NewYorkTimesJson.Clue> = body.clues.map { clue ->
                NewYorkTimesJson.Clue(
                    cells = clue.cells,
                    label = clue.label,
                    text = clue.text[0].formatted.ifEmpty { clue.text[0].plain }
                )
            }

            override val beforeStartOverlay: String? =
                if (body.overlays.beforeStart != null && (body.overlays.beforeStart - 1) in data.assets.indices) {
                    data.assets[body.overlays.beforeStart - 1].uri.ifEmpty { null }
                } else {
                    null
                }
        }
    }
}