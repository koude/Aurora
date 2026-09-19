package com.github.kr328.clash.design.component

import android.content.Context
import android.graphics.Color
import androidx.core.content.ContextCompat
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.util.getPixels
import com.github.kr328.clash.design.util.resolveThemedResourceId

class ProxyViewConfig(val context: Context, var proxyLine: Int) {
    private val colorSurfaceContainer = ContextCompat.getColor(context, R.color.aurora_surface_container)

    val clickableBackground =
        context.resolveThemedResourceId(android.R.attr.selectableItemBackground)

    val selectedControl =
        ContextCompat.getColor(context, R.color.aurora_on_secondary_container)
    val selectedBackground =
        ContextCompat.getColor(context, R.color.aurora_secondary_container)

    val unselectedControl =
        ContextCompat.getColor(context, R.color.aurora_on_surface)
    val unselectedBackground: Int
        get() = colorSurfaceContainer

    val layoutPadding = context.getPixels(R.dimen.proxy_layout_padding).toFloat()
    val contentPadding
        get() = if (proxyLine==2) context.getPixels(R.dimen.proxy_content_padding).toFloat() else context.getPixels(R.dimen.proxy_content_padding_grid3).toFloat()
    val textMargin
        get() = if (proxyLine==2) context.getPixels(R.dimen.proxy_text_margin).toFloat() else context.getPixels(R.dimen.proxy_text_margin_grid3).toFloat()
    val textSize
        get() = if (proxyLine==2) context.getPixels(R.dimen.proxy_text_size).toFloat() else context.getPixels(R.dimen.proxy_text_size_grid3).toFloat()

    val shadow = Color.argb(
        0x15,
        Color.red(Color.DKGRAY),
        Color.green(Color.DKGRAY),
        Color.blue(Color.DKGRAY),
    )

    val cardRadius = context.getPixels(R.dimen.proxy_card_radius).toFloat()
    var cardOffset = context.getPixels(R.dimen.proxy_card_offset).toFloat()
}
