package com.jeffpdavidson.kotwords.formats

/**
 * Comparators to use to evaluate whether two images are equal.
 *
 * Since images are generally non-deterministic due to timestamps, unique identifiers, and/or metadata, we generally
 * want to compare only their rendered content.
 */
expect object ImageComparator {
    suspend fun assertPdfEquals(expected: ByteArray, actual: ByteArray)

    suspend fun assertPngEquals(expected: ByteArray, actual: ByteArray)
}