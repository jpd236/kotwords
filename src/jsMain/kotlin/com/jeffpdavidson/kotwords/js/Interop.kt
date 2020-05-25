package com.jeffpdavidson.kotwords.js

import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.w3c.files.File
import org.w3c.files.FileReader
import kotlin.js.Promise

/** Helpers for operating on Javascript objects and data types in Kotlin. */
object Interop {

    /** Return this [ArrayBuffer] as a [ByteArray]. */
    fun ArrayBuffer.toByteArray(): ByteArray {
        return Int8Array(this).asDynamic() as ByteArray
    }

    /** Return this [ByteArray] as an [ArrayBuffer]. */
    fun ByteArray.toArrayBuffer(): ArrayBuffer {
        return Int8Array(toTypedArray()).buffer
    }

    /** Return a [Promise] resolving to the contents of the given [File] as a [ByteArray]. */
    fun readFile(file: File): Promise<ByteArray> {
        return Promise { resolve, reject ->
            val reader = FileReader()
            reader.onload = { _ ->
                try {
                    val data = (reader.result as ArrayBuffer).toByteArray()
                    resolve(data)
                } catch (t: Throwable) {
                    reject(t)
                }
            }
            reader.readAsArrayBuffer(file)
        }
    }
}