package com.jeffpdavidson.kotwords

import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.reflect.KClass

// TODO: Handle sourceSet-specific resources

actual suspend fun <T : Any> readBinaryResource(
    clazz: KClass<T>,
    resourceName: String
): ByteArray {
    return FileSystem.SYSTEM.read("src/commonTest/resources/$resourceName".toPath()) {
        readByteArray()
    }
}

actual suspend fun <T : Any> readStringResource(clazz: KClass<T>, resourceName: String): String {
    return FileSystem.SYSTEM.read("src/commonTest/resources/$resourceName".toPath()) {
        readUtf8()
    }
}

actual typealias IgnoreNative = kotlin.test.Ignore