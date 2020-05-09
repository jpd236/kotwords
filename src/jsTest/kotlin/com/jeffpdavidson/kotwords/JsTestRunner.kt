package com.jeffpdavidson.kotwords

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.w3c.xhr.ARRAYBUFFER
import org.w3c.xhr.TEXT
import org.w3c.xhr.XMLHttpRequest
import org.w3c.xhr.XMLHttpRequestResponseType
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.reflect.KClass

actual fun runTest(block: suspend () -> Unit): dynamic = GlobalScope.promise { block() }

actual suspend fun <T : Any> readBinaryResource(clazz: KClass<T>, resourceName: String): ByteArray {
    return readResource(resourceName, XMLHttpRequestResponseType.ARRAYBUFFER) {
        Int8Array(it.response as ArrayBuffer).asDynamic() as ByteArray
    }
}

actual suspend fun <T : Any> readStringResource(clazz: KClass<T>, resourceName: String): String {
    return readResource(resourceName, XMLHttpRequestResponseType.TEXT) { it.responseText }
}

private suspend fun <T : Any> readResource(resourceName: String,
                                           responseType: XMLHttpRequestResponseType,
                                           responseHandler: (XMLHttpRequest) -> T): T {
    return suspendCoroutine { continuation ->
        val xhr = XMLHttpRequest()
        xhr.responseType = responseType
        xhr.open("GET", "/base/resources/test/$resourceName")
        xhr.onload = {
            if (xhr.status != 200.toShort()) {
                continuation.resumeWithException(
                        IllegalArgumentException("Error loading resource $resourceName"))
            } else {
                continuation.resume(responseHandler(xhr))
            }
        }
        xhr.send()
    }
}