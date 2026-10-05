package com.github.kr328.clash

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.setContent
import com.koude.aurora.designsystem.theme.AuroraTheme
import com.koude.aurora.ui.misc.ApkBrokenScreen
import com.github.kr328.clash.design.ApkBrokenDesign
import com.github.kr328.clash.design.R
import kotlinx.coroutines.isActive

class ApkBrokenActivity : BaseActivity<ApkBrokenDesign>() {
    override suspend fun main() {
        setContent {
            AuroraTheme {
                ApkBrokenScreen(getString(R.string.meta_github_url)) { url ->
                    startActivity(Intent(Intent.ACTION_VIEW).setData(Uri.parse(url)))
                }
            }
        }

        while (isActive) events.receive()
    }
}
