package com.github.kr328.clash.service.clash.module

import android.app.Service
import com.github.kr328.clash.common.constants.Intents

class RootRefreshModule(service: Service) : Module<Unit>(service) {
    override suspend fun run() {
        val changes = receiveBroadcast {
            addAction(Intents.ACTION_PROFILE_CHANGED)
            addAction(Intents.ACTION_OVERRIDE_CHANGED)
        }
        while (true) {
            changes.receive()
            enqueueEvent(Unit)
        }
    }
}
