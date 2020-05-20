package com.jeffpdavidson.kotwords.formats

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipException
import java.util.zip.ZipInputStream

internal actual object Zip {
    actual suspend fun unzip(data: ByteArray): ByteArray {
        try {
            ZipInputStream(ByteArrayInputStream(data)).use {
                var entry = it.nextEntry
                while (entry != null) {
                    try {
                        if (!entry.isDirectory) {
                            ByteArrayOutputStream().use { output ->
                                it.copyTo(output)
                                return output.toByteArray()
                            }
                        }
                    } finally {
                        it.closeEntry()
                    }
                    entry = it.nextEntry
                }
                throw InvalidZipException("No file entry in ZIP file")
            }
        } catch (e: ZipException) {
            throw InvalidZipException("Error unzipping data", e)
        }
    }
}