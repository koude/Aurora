package com.koude.aurora.data.connections

class AppLabelResolver(private val lookup: (String) -> String?) {
    private val cache = mutableMapOf<String, String?>()

    @Synchronized
    fun resolve(packageName: String): String? {
        if (packageName.isBlank()) return null
        if (cache.containsKey(packageName)) return cache[packageName]

        val label = runCatching { lookup(packageName) }
            .getOrNull()
            ?.trim()
            ?.takeIf(String::isNotEmpty)
        cache[packageName] = label
        return label
    }
}
