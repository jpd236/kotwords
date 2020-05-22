package com.jeffpdavidson.kotwords.formats

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

internal actual object Zip {
    actual suspend fun zip(filename: String, data: ByteArray): ByteArray {
        val zipBytes = ByteArrayOutputStream()
        ZipOutputStream(zipBytes).use {
            val entry = ZipEntry(filename)
            it.putNextEntry(entry)
            it.write(data)
            it.closeEntry()
        }
        return zipBytes.toByteArray()
    }

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