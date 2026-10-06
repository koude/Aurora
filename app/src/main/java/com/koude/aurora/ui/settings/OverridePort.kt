package com.koude.aurora.ui.settings

/** Blank means an explicitly disabled port; null means the input is invalid. */
internal fun parsePortOverrideInput(input: String): Int? {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return 0
    return trimmed.toIntOrNull()?.takeIf { it in 0..65535 }
}
