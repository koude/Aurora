package com.koude.aurora.data.settings

import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.core.model.ConfigurationOverride
import com.github.kr328.clash.util.withClash

interface OverrideRepository {
    suspend fun query(): ConfigurationOverride
    suspend fun patch(configuration: ConfigurationOverride)
    suspend fun clear()
}

class ServiceOverrideRepository : OverrideRepository {
    override suspend fun query(): ConfigurationOverride =
        withClash { queryOverride(Clash.OverrideSlot.Persist) }

    override suspend fun patch(configuration: ConfigurationOverride) {
        withClash { patchOverride(Clash.OverrideSlot.Persist, configuration) }
    }

    override suspend fun clear() {
        withClash { clearOverride(Clash.OverrideSlot.Persist) }
    }
}
