package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.jslib.JSZip
import com.jeffpdavidson.kotwords.jslib.ZipOutputType
import com.jeffpdavidson.kotwords.jslib.newGenerateAsyncOptions
import kotlinx.coroutines.await
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.Uint8Array
import kotlin.js.RegExp

internal actual object Zip {
    actual suspend fun zip(filename: String, data: ByteArray): ByteArray {
        val zip = JSZip()
        zip.file(filename, Uint8Array(data.toTypedArray()))
        val options = newGenerateAsyncOptions(ZipOutputType.ARRAY_BUFFER)
        val result = zip.generateAsync(options).await() as ArrayBuffer
        return result.asByteArray()
    }

    actual suspend fun unzip(data: ByteArray): ByteArray {
        try {
            val zip = JSZip().loadAsync(Uint8Array(data.toTypedArray())).await()
            val files = zip.file(RegExp(""))
            if (files.isNotEmpty()) {
                val result = files[0].async("arraybuffer").await() as ArrayBuffer
                return result.asByteArray()
            }
        } catch (e: Throwable) {
            throw InvalidZipException("Error unzipping data", e)
        }
        throw InvalidZipException("No file entry in ZIP file")
    }

    private fun ArrayBuffer.asByteArray(): ByteArray {
        return Int8Array(this).asDynamic() as ByteArray
    }
}