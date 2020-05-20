package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.jslib.JSZip
import kotlinx.coroutines.await
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.Uint8Array
import kotlin.js.RegExp

internal actual object Zip {
    actual suspend fun unzip(data: ByteArray): ByteArray {
        try {
            val zip = JSZip().loadAsync(Uint8Array(data.toTypedArray())).await()
            val files = zip.file(RegExp(""))
            if (files.isNotEmpty()) {
                val result = files[0].async("arraybuffer").await() as ArrayBuffer
                return Int8Array(result).asDynamic() as ByteArray
            }
        } catch (e: Throwable) {
            throw InvalidZipException("Error unzipping data", e)
        }
        throw InvalidZipException("No file entry in ZIP file")
    }
}