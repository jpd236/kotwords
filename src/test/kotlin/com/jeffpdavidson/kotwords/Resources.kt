package com.jeffpdavidson.kotwords

import java.nio.charset.StandardCharsets
import kotlin.reflect.KClass

/** Read the given resource as a UTF-8 string. */
fun <T : Any> KClass<T>.readUtf8Resource(resourceName: String): String {
    return java.classLoader.getResourceAsStream(resourceName)
            .bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
}

/** Read the given resource as binary data. */
fun <T : Any> KClass<T>.readBinaryResource(resourceName: String): ByteArray {
    return java.classLoader.getResourceAsStream(resourceName).readBytes()
}