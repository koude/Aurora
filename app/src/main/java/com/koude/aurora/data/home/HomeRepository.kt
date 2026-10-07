package com.koude.aurora.data.home

import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.core.model.RoutePreview
import com.github.kr328.clash.core.model.TunnelState
import com.github.kr328.clash.core.util.trafficDownload
import com.github.kr328.clash.core.util.trafficUpload
import com.github.kr328.clash.util.withClash
import com.github.kr328.clash.util.withProfile

data class HomeConnectionSnapshot(
    val mode: TunnelState.Mode,
    val profileName: String?,
)

data class HomeTrafficSnapshot(
    val uploadSpeed: String,
    val downloadSpeed: String,
)

interface HomeRepository {
    suspend fun connection(): HomeConnectionSnapshot
    suspend fun traffic(): HomeTrafficSnapshot
    suspend fun setMode(mode: TunnelState.Mode)
    suspend fun previewRoute(target: String): RoutePreview
    suspend fun hasActiveImportedProfile(): Boolean
}

class ServiceHomeRepository : HomeRepository {
    override suspend fun connection(): HomeConnectionSnapshot {
        val state = withClash { queryTunnelState() }
        val profileName = withProfile { queryActive()?.name }
        return HomeConnectionSnapshot(state.mode, profileName)
    }

    override suspend fun traffic(): HomeTrafficSnapshot = withClash {
        val traffic = queryTrafficNow()
        HomeTrafficSnapshot(
            uploadSpeed = "${traffic.trafficUpload()}/s",
            downloadSpeed = "${traffic.trafficDownload()}/s",
        )
    }

    override suspend fun setMode(mode: TunnelState.Mode) {
        withClash {
            val override = queryOverride(Clash.OverrideSlot.Session)
            override.mode = mode
            patchOverride(Clash.OverrideSlot.Session, override)
        }
    }

    override suspend fun previewRoute(target: String): RoutePreview =
        withClash { queryRoutePreview(target) }

    override suspend fun hasActiveImportedProfile(): Boolean =
        withProfile { queryActive()?.imported == true }
}
