package com.jeffpdavidson.kotwords.formats

import korlibs.io.file.std.MemoryVfs
import korlibs.io.file.std.createZipFromTree
import korlibs.io.file.std.openAsZip
import korlibs.io.stream.openAsync

internal actual object Zip {
    actual suspend fun zip(filename: String, data: ByteArray): ByteArray {
        return MemoryVfs(mapOf(filename to data.openAsync())).createZipFromTree()
    }

    actual suspend fun unzip(data: ByteArray): ByteArray {
        val fs = try {
            data.openAsync().openAsZip()
        } catch (e: IllegalArgumentException) {
            throw InvalidZipException("Error unzipping data", e)
        }
        val files = fs.listRecursiveSimple()
        files.forEach { file ->
            if (file.isFile()) {
                return file.readBytes()
            }
        }
        throw InvalidZipException("No file entry in ZIP file")
    }
}