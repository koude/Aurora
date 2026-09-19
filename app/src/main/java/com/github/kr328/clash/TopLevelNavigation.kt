package com.github.kr328.clash

import android.app.Activity
import android.content.Intent
import com.github.kr328.clash.common.util.intent
import kotlin.reflect.KClass

internal fun Activity.navigateTopLevel(destination: KClass<out Activity>) {
    val intent = destination.intent.apply {
        if (destination == MainActivity::class) {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
    }

    startActivity(intent)
    finish()
}
