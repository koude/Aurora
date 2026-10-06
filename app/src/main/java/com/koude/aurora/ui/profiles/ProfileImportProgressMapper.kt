package com.koude.aurora.ui.profiles

import com.github.kr328.clash.core.model.FetchStatus

internal fun FetchStatus.toProfileImportProgress(): ProfileImportProgress {
    val resource = args.firstOrNull()?.takeIf { it.isNotBlank() }
    val stage = when (action) {
        FetchStatus.Action.FetchConfiguration -> "正在下载配置文件${resource?.let { " · $it" }.orEmpty()}"
        FetchStatus.Action.FetchProviders -> {
            val position = if (max > 0) "（${(progress + 1).coerceAtMost(max)}/$max）" else ""
            "正在下载资源$position${resource?.let { " · $it" }.orEmpty()}"
        }
        FetchStatus.Action.SubscriptionInfo -> "正在读取订阅信息…"
        FetchStatus.Action.Verifying -> "正在校验配置文件…"
    }
    return ProfileImportProgress(
        stage = stage,
        downloadedBytes = downloadedBytes,
        totalBytes = totalBytes,
        speedBytesPerSecond = speedBytesPerSecond,
    )
}
