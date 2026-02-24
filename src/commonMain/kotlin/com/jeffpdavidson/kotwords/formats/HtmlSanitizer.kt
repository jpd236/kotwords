package com.jeffpdavidson.kotwords.formats

import com.jeffpdavidson.kotwords.formats.unidecode.Unidecode

/** Utility for constrain HTML strings to plain text. */
internal object HtmlSanitizer {
    // Empty tags which should be filtered out to avoid replacing them with unnecessary substitution characters.
    private val emptyTagRegexes = listOf("<b></b>", "<i></i>").map { it.toRegex(RegexOption.IGNORE_CASE) }

    private val stripHtmlReplacements = mapOf(
        "</?[a-z]+>" to "",
        "&amp;" to "&",
        "&lt;" to "<",
    ).mapKeys { it.key.toRegex(RegexOption.IGNORE_CASE) }

    private val formatHtmlReplacements = mapOf(
        "</?b>" to "*",
        "</?i>" to "\"",
    ).mapKeys { it.key.toRegex(RegexOption.IGNORE_CASE) } + stripHtmlReplacements

    fun substituteUnsupportedText(
        text: String,
        sanitizeCharacters: Boolean,
        formatHtml: Boolean = true,
    ): String {
        // First, filter out any empty tags repeatedly until none remain (to handle nesting).
        var withoutEmpty = text
        do {
            val temp = withoutEmpty
            withoutEmpty = emptyTagRegexes.fold(withoutEmpty) { clue, regex -> clue.replace(regex, "") }
        } while (temp != withoutEmpty)

        // Next, replace all HTML with supported substitutions.
        val replacements = if (formatHtml) (formatHtmlReplacements + stripHtmlReplacements) else stripHtmlReplacements
        val withoutHtml = replacements.entries.fold(withoutEmpty) { clue, (from, to) ->
            clue.replace(from, to)
        }

        if (!sanitizeCharacters) {
            return withoutHtml
        }
        return Unidecode.unidecode(withoutHtml)
    }
}