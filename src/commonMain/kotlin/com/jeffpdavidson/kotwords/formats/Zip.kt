package com.jeffpdavidson.kotwords.formats

class InvalidZipException: Exception {
    constructor(message: String, ex: Throwable): super(message, ex)
    constructor(message: String): super(message)
}

internal expect object Zip {
    /**
     * Return the contents of the first file contained in the given ZIP file data.
     *
     * @throws InvalidZipException if the file is not a valid ZIP file with at least one file
     *                             inside.
     */
    suspend fun unzip(data: ByteArray): ByteArray
}