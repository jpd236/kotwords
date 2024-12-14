package com.jeffpdavidson.kotwords.formats.json

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

internal object CnnJson {
    @Serializable
    internal data class Data(
        val data: JsonObject,
        val metaData: MetaData = MetaData(),
    ) {
        @Serializable
        internal data class MetaData(
            val title: String? = null,
            val date: String? = null,
        )
    }
}