package com.koude.aurora.ui.profiles

import com.github.kr328.clash.core.model.FetchStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileImportProgressMapperTest {
    @Test fun providerProgressKeepsStageAndTransferValues() {
        val status = FetchStatus(
            action = FetchStatus.Action.FetchProviders,
            args = listOf("provider.yaml"),
            progress = 1,
            max = 3,
            downloadedBytes = 1024,
            totalBytes = 4096,
            speedBytesPerSecond = 512,
        )

        assertEquals(
            ProfileImportProgress("正在下载资源（2/3） · provider.yaml", 1024, 4096, 512),
            status.toProfileImportProgress(),
        )
    }
}
