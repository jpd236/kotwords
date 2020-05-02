package com.jeffpdavidson.kotwords.formats.json

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import se.ansman.kotshi.JsonDefaultValue
import se.ansman.kotshi.KotshiJsonAdapterFactory
import java.lang.reflect.Type

/** Utility class for serializing to / deserializing from Json. */
internal object JsonSerializer {
    @KotshiJsonAdapterFactory
    abstract class ApplicationJsonAdapterFactory : JsonAdapter.Factory {
        companion object {
            val INSTANCE: ApplicationJsonAdapterFactory =
                    KotshiJsonSerializer_ApplicationJsonAdapterFactory()
        }
    }

    private var INSTANCE: Moshi =
            Moshi.Builder()
                    .add(ApplicationJsonAdapterFactory.INSTANCE)
                    .add(WallStreetJournalJson.HtmlStringAdapter())
                    .build()

    fun <T> toJson(type: Class<T>, value: T): String {
        return INSTANCE.adapter(type).toJson(value)
    }

    fun <T> fromJson(type: Class<T>, string: String): T {
        return INSTANCE.adapter(type).fromJson(string)!!
    }

    fun <T> fromJson(type: Type, string: String): T {
        return INSTANCE.adapter<T>(type).fromJson(string)!!
    }
}

@JsonDefaultValue
internal fun <T> provideDefaultList() = emptyList<T>()