package com.jeffpdavidson.kotwords.formats.json

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
internal data class WorldOfCrosswordsJson(
    val success: Boolean = false,
    val msg: List<JsonElement> = listOf()
)