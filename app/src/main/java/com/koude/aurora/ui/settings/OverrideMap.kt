package com.koude.aurora.ui.settings

internal sealed interface OverrideMapInput {
    data class Valid(val entries: Map<String, String>) : OverrideMapInput
    data class Invalid(val lineNumber: Int) : OverrideMapInput
}

/** Parses one key=value entry per line without silently discarding or replacing entries. */
internal fun parseOverrideMapInput(input: String): OverrideMapInput {
    val entries = linkedMapOf<String, String>()
    input.lines().forEachIndexed { index, rawLine ->
        val line = rawLine.trim()
        if (line.isEmpty()) return@forEachIndexed

        val separator = line.indexOf('=')
        if (separator <= 0 || separator == line.lastIndex) {
            return OverrideMapInput.Invalid(index + 1)
        }

        val key = line.substring(0, separator).trim()
        val value = line.substring(separator + 1).trim()
        if (key.isEmpty() || value.isEmpty() || entries.containsKey(key)) {
            return OverrideMapInput.Invalid(index + 1)
        }
        entries[key] = value
    }
    return OverrideMapInput.Valid(entries)
}
