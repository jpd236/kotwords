package com.jeffpdavidson.kotwords

import org.w3c.xhr.XMLHttpRequest
import kotlin.js.Promise

/** Read the given resource as a UTF-8 string. */
fun readUtf8Resource(resourceName: String): Promise<String> {
    return Promise { resolve, reject ->
        val xhr = XMLHttpRequest()
        xhr.open("GET", "/base/resources/test/$resourceName")
        xhr.onload = {
            if (xhr.status != 200.toShort()) {
                reject(IllegalArgumentException("Error loading resource $resourceName"))
            } else {
                resolve(xhr.responseText)
            }
        }
        xhr.send()
    }
}
