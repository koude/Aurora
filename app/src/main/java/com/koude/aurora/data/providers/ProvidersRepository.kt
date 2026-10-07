package com.koude.aurora.data.providers

import com.github.kr328.clash.core.model.Provider
import com.github.kr328.clash.util.withClash

interface ProvidersRepository {
    suspend fun query(): List<Provider>
    suspend fun update(type: Provider.Type, name: String)
}

class ServiceProvidersRepository : ProvidersRepository {
    override suspend fun query(): List<Provider> = withClash { queryProviders().sorted() }

    override suspend fun update(type: Provider.Type, name: String) {
        withClash { updateProvider(type, name) }
    }
}
