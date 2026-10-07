package com.koude.aurora.data.proxy

import android.content.Context
import com.github.kr328.clash.core.model.ProxyGroup
import com.github.kr328.clash.core.model.ProxySort
import com.github.kr328.clash.design.store.UiStore
import com.github.kr328.clash.util.withClash

interface ProxyRepository {
    suspend fun groupNames(hideUnselectable: Boolean): List<String>
    suspend fun group(name: String, sort: ProxySort): ProxyGroup
    suspend fun select(group: String, proxy: String)
    suspend fun test(proxy: String)
    suspend fun healthCheck(group: String)
}

class ServiceProxyRepository : ProxyRepository {
    override suspend fun groupNames(hideUnselectable: Boolean): List<String> =
        withClash { queryProxyGroupNames(hideUnselectable) }

    override suspend fun group(name: String, sort: ProxySort): ProxyGroup =
        withClash { queryProxyGroup(name, sort) }

    override suspend fun select(group: String, proxy: String) {
        withClash { patchSelector(group, proxy) }
    }

    override suspend fun test(proxy: String) {
        withClash { testProxy(proxy) }
    }

    override suspend fun healthCheck(group: String) {
        withClash { healthCheck(group) }
    }
}

interface ProxyPreferences {
    var sort: ProxySort
    var hideUnselectable: Boolean
    var lastGroup: String
}

class StoredProxyPreferences(context: Context) : ProxyPreferences {
    private val store = UiStore(context.applicationContext)

    override var sort: ProxySort
        get() = store.proxySort
        set(value) { store.proxySort = value }

    override var hideUnselectable: Boolean
        get() = store.proxyExcludeNotSelectable
        set(value) { store.proxyExcludeNotSelectable = value }

    override var lastGroup: String
        get() = store.proxyLastGroup
        set(value) { store.proxyLastGroup = value }
}
