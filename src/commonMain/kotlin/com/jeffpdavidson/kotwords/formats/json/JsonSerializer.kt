package com.jeffpdavidson.kotwords.formats.json

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/** Utility class for serializing to Json. */
internal object JsonSerializer {

    private var INSTANCE: Json = Json {
        ignoreUnknownKeys = true
    }

    @OptIn(ExperimentalSerializationApi::class)
    inline fun <reified T> fromJson(string: String): T {
        return INSTANCE.decodeFromString(string)
    }
}
