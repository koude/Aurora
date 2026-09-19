package com.github.kr328.clash

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import com.github.kr328.clash.common.util.intent
import kotlin.reflect.KClass

internal fun Activity.navigateTopLevel(destination: KClass<out Activity>) {
    val intent = destination.intent.apply {
        addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)

        if (destination == MainActivity::class) {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
    }

    val options = ActivityOptions.makeCustomAnimation(this, 0, 0)
    startActivity(intent, options.toBundle())
    finish()
    @Suppress("DEPRECATION")
    overridePendingTransition(0, 0)
}
