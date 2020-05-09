package com.jeffpdavidson.kotwords

import kotlin.reflect.KClass

// Workaround for lack of suspend test functions.
// See: https://youtrack.jetbrains.com/issue/KT-22228
expect fun runTest(block: suspend () -> Unit)

/** Read the given resource as binary data. */
expect suspend fun <T : Any> readBinaryResource(clazz: KClass<T>, resourceName: String): ByteArray

/** Read the given resource as a String. */
expect suspend fun <T : Any> readStringResource(clazz: KClass<T>, resourceName: String): String