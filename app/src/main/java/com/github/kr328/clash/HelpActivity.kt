package com.github.kr328.clash

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.setContent
import com.github.kr328.clash.design.R as DesignR
import com.koude.aurora.designsystem.theme.AuroraTheme
import com.koude.aurora.ui.help.HelpLink
import com.koude.aurora.ui.help.HelpScreen
import kotlinx.coroutines.isActive

class HelpActivity : BaseActivity() {
    override suspend fun main() {
        val documents = listOf(
            HelpLink(DesignR.string.clash_wiki, DesignR.string.clash_wiki_url, getString(DesignR.string.clash_wiki_url)),
            HelpLink(DesignR.string.clash_meta_wiki, DesignR.string.clash_meta_wiki_url, getString(DesignR.string.clash_meta_wiki_url)),
        )
        val sources = listOf(
            HelpLink(DesignR.string.clash_meta_core, DesignR.string.clash_meta_core_url, getString(DesignR.string.clash_meta_core_url)),
            HelpLink(DesignR.string.clash_meta_for_android, DesignR.string.meta_github_url, getString(DesignR.string.meta_github_url)),
        )
        setContent {
            AuroraTheme(darkTheme = isDarkTheme) {
                HelpScreen(
                    documents = documents,
                    sources = sources,
                    onOpenLink = { url -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
                    onBack = ::finish,
                )
            }
        }
        while (isActive) events.receive()
    }
}
