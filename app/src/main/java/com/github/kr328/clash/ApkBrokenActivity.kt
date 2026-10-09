package com.github.kr328.clash

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.setContent
import com.koude.aurora.designsystem.theme.AuroraTheme
import com.koude.aurora.ui.misc.ApkBrokenScreen
import com.github.kr328.clash.design.R
import kotlinx.coroutines.isActive

class ApkBrokenActivity : BaseActivity() {
    override suspend fun main() {
        setContent {
            AuroraTheme(darkTheme = isDarkTheme, dynamicColor = useDynamicColor) {
                ApkBrokenScreen(getString(R.string.aurora_releases_url)) { url ->
                    startActivity(Intent(Intent.ACTION_VIEW).setData(Uri.parse(url)))
                }
            }
        }

        while (isActive) events.receive()
    }
}
