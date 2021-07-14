package com.jeffpdavidson.kotwords.js

import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.w3c.xhr.ARRAYBUFFER
import org.w3c.xhr.TEXT
import org.w3c.xhr.XMLHttpRequest
import org.w3c.xhr.XMLHttpRequestResponseType
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object Http {
    suspend fun getBinary(path: String): ByteArray {
        return get(path, XMLHttpRequestResponseType.ARRAYBUFFER) {
            Int8Array(it.response as ArrayBuffer).asDynamic() as ByteArray
        }
    }

    suspend fun getString(path: String): String {
        return get(path, XMLHttpRequestResponseType.TEXT) { it.responseText }
    }

    private suspend fun <T : Any> get(
        path: String,
        responseType: XMLHttpRequestResponseType,
        responseHandler: (XMLHttpRequest) -> T
    ): T {
        return suspendCoroutine { continuation ->
            val xhr = XMLHttpRequest()
            xhr.responseType = responseType
            xhr.open("GET", path)
            xhr.onload = {
                if (xhr.status != 200.toShort()) {
                    continuation.resumeWithException(
                        IllegalArgumentException("Error loading path $path")
                    )
                } else {
                    continuation.resume(responseHandler(xhr))
                }
            }
            xhr.send()
        }
    }
}