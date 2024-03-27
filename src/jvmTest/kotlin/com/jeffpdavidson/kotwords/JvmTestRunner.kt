package com.jeffpdavidson.kotwords

import java.io.InputStream
import java.nio.charset.StandardCharsets
import kotlin.reflect.KClass

actual suspend fun <T : Any> readBinaryResource(clazz: KClass<T>, resourceName: String): ByteArray {
    return getResourceAsStream(clazz, resourceName).readBytes()
}

actual suspend fun <T : Any> readStringResource(clazz: KClass<T>, resourceName: String): String {
    return getResourceAsStream(clazz, resourceName)
        .bufferedReader(StandardCharsets.UTF_8)
        .use { it.readText() }
}

private fun <T : Any> getResourceAsStream(clazz: KClass<T>, resourceName: String): InputStream {
    return clazz.java.classLoader.getResourceAsStream(resourceName)
        ?: throw IllegalArgumentException("Error loading resource $resourceName")
}

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
actual annotation class IgnoreNative