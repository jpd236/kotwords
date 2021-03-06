package com.jeffpdavidson.kotwords.js

import org.khronos.webgl.Uint8Array
import kotlin.js.Promise
import kotlin.js.RegExp

@JsModule("jszip")
@JsNonModule
internal external class JSZip {
    fun file(regex: RegExp): Array<ZipObject>
    fun file(name: String, data: Any): JSZip
    fun loadAsync(data: Uint8Array): Promise<JSZip>
    fun generateAsync(options: GenerateAsyncOptions): Promise<Any>
}

internal external class ZipObject {
    fun async(type: String): Promise<Any?>
}

internal enum class ZipOutputType(val jsValue: String) {
    BASE64("base64"),
    ARRAY_BUFFER("arraybuffer")
}

internal enum class ZipOutputCompression(val jsValue: String) {
    DEFLATE("deflate")
}

internal external interface GenerateAsyncOptions {
    var type: String
    var compression: String
}

internal fun newGenerateAsyncOptions(
    type: ZipOutputType = ZipOutputType.BASE64,
    compression: ZipOutputCompression = ZipOutputCompression.DEFLATE
): GenerateAsyncOptions {
    val options = js("{}").unsafeCast<GenerateAsyncOptions>()
    options.type = type.jsValue
    options.compression = compression.jsValue
    return options
}