package com.koude.aurora.ui.settings

import com.koude.aurora.data.settings.NetworkSettingsRepository
import com.koude.aurora.data.settings.NetworkSettingsSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkSettingsViewModelTest {
    @Test fun refreshReadsVpnAndNetworkSettings() {
        val repository = FakeRepository()
        val viewModel = NetworkSettingsViewModel(repository)

        viewModel.refresh(true)

        assertTrue(viewModel.uiState.value.running)
        assertTrue(viewModel.uiState.value.routeSystemTraffic)
        assertEquals("system", viewModel.uiState.value.tunStack)
        assertFalse(viewModel.uiState.value.showSystemProxy)
    }

    @Test fun everySettingWritesToItsRepositoryField() {
        val repository = FakeRepository()
        val viewModel = NetworkSettingsViewModel(repository)
        viewModel.refresh(false)

        viewModel.setRouteSystemTraffic(false)
        viewModel.setBypassPrivateNetwork(false)
        viewModel.setDnsHijacking(false)
        viewModel.setAllowBypass(false)
        viewModel.setAllowIpv6(true)
        viewModel.setSystemProxy(false)
        viewModel.setTunStack("gvisor")

        assertEquals(
            listOf("vpn:false", "private:false", "dns:false", "bypass:false", "ipv6:true", "proxy:false", "stack:gvisor"),
            repository.writes,
        )
        assertFalse(viewModel.uiState.value.routeSystemTraffic)
        assertTrue(viewModel.uiState.value.allowIpv6)
        assertEquals("gvisor", viewModel.uiState.value.tunStack)
    }

    private class FakeRepository : NetworkSettingsRepository {
        val writes = mutableListOf<String>()

        override fun read() = NetworkSettingsSnapshot(
            routeSystemTraffic = true,
            bypassPrivateNetwork = true,
            dnsHijacking = true,
            allowBypass = true,
            allowIpv6 = false,
            systemProxy = true,
            tunStack = "system",
            showSystemProxy = false,
        )

        override fun setRouteSystemTraffic(value: Boolean) { writes += "vpn:$value" }
        override fun setBypassPrivateNetwork(value: Boolean) { writes += "private:$value" }
        override fun setDnsHijacking(value: Boolean) { writes += "dns:$value" }
        override fun setAllowBypass(value: Boolean) { writes += "bypass:$value" }
        override fun setAllowIpv6(value: Boolean) { writes += "ipv6:$value" }
        override fun setSystemProxy(value: Boolean) { writes += "proxy:$value" }
        override fun setTunStack(value: String) { writes += "stack:$value" }
    }
}
