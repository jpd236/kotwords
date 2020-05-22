package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.model.Puzzle
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import org.khronos.webgl.Int8Array
import org.w3c.files.Blob
import org.w3c.files.FileReader
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.js.Promise

object JsJpzFile {
    /** Returns this puzzle as a data URL. */
    @JsName("asDataUrl")
    fun asDataUrl(puzzle: Puzzle): Promise<String> {
        return GlobalScope.promise {
            val data = puzzle.asJpzFile().toCompressedFile("${puzzle.title.replace("[^A-Za-z0-9]".toRegex(), "")}.xml")
            val blob = Blob(arrayOf(Int8Array(data.toTypedArray()).buffer))
            val reader = FileReader()
            suspendCoroutine<String> { cont ->
                reader.onload = { event ->
                    cont.resume(event.target.asDynamic().result)
                }
                reader.readAsDataURL(blob)
            }
        }
    }
}