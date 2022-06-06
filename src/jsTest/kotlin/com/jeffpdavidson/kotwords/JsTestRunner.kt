package com.jeffpdavidson.kotwords

import com.jeffpdavidson.kotwords.js.Http
import kotlin.reflect.KClass

// See karma.config.d/resources.js for details on this path.
private const val BASE_RESOURCE_PATH = "/base/build/processedResources/js/test"

actual suspend fun <T : Any> readBinaryResource(clazz: KClass<T>, resourceName: String): ByteArray =
    Http.getBinary("$BASE_RESOURCE_PATH/$resourceName")

actual suspend fun <T : Any> readStringResource(clazz: KClass<T>, resourceName: String): String =
    Http.getString("$BASE_RESOURCE_PATH/$resourceName")
