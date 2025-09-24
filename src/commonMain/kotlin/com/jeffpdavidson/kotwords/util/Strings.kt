package com.jeffpdavidson.kotwords.util

internal fun String.trimmedLines(): List<String> = if (isBlank()) listOf() else trim().lines().map { it.trim() }

internal fun String.trimmedAlphabeticalWords(): List<String> =
    uppercase().replace("[^A-Z\\s]".toRegex(), "").split("\\s+".toRegex())