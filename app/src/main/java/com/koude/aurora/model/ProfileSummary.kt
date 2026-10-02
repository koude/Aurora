package com.koude.aurora.model

import java.util.UUID

data class ProfileSummary(
    val id: UUID,
    val name: String,
    val kind: ProfileKind,
    val active: Boolean,
    val imported: Boolean,
    val updatedAt: Long,
)

enum class ProfileKind {
    File,
    Url,
    External,
}
