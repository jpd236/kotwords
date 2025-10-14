package com.jeffpdavidson.kotwords

import kotlin.reflect.KClass

/** Read the given resource as binary data. */
expect suspend fun <T : Any> readBinaryResource(clazz: KClass<T>, resourceName: String): ByteArray

/** Read the given resource as a String. */
expect suspend fun <T : Any> readStringResource(clazz: KClass<T>, resourceName: String): String