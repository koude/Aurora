package com.github.kr328.clash.design.component

import android.content.Context
import android.graphics.Color
import androidx.core.content.ContextCompat
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.util.getPixels
import com.github.kr328.clash.design.util.resolveThemedResourceId

class ProxyViewConfig(val context: Context, var proxyLine: Int) {
    private val colorSurfaceContainer = ContextCompat.getColor(context, R.color.aurora_surface_container)

    private val compact: Boolean
        get() = proxyLine == 3

    val clickableBackground =
        context.resolveThemedResourceId(android.R.attr.selectableItemBackground)

    val selectedControl =
        ContextCompat.getColor(context, R.color.aurora_on_secondary_container)
    val selectedBackground =
        ContextCompat.getColor(context, R.color.aurora_secondary_container)
    val selectedOutline =
        ContextCompat.getColor(context, R.color.aurora_primary)

    val unselectedControl =
        ContextCompat.getColor(context, R.color.aurora_on_surface)
    val unselectedSubtitle =
        ContextCompat.getColor(context, R.color.aurora_on_surface_variant)
    val unselectedOutline =
        ContextCompat.getColor(context, R.color.aurora_outline_variant)
    val unselectedBackground: Int
        get() = colorSurfaceContainer

    val delayGood = ContextCompat.getColor(context, R.color.aurora_primary)
    val delayMedium = ContextCompat.getColor(context, R.color.aurora_tertiary)
    val delayBad = ContextCompat.getColor(context, R.color.aurora_error)

    val layoutPadding = context.getPixels(R.dimen.proxy_layout_padding).toFloat()
    val contentPadding
        get() = context.getPixels(
            if (compact) R.dimen.proxy_content_padding_grid3 else R.dimen.proxy_content_padding
        ).toFloat()
    val textMargin
        get() = context.getPixels(
            if (compact) R.dimen.proxy_text_margin_grid3 else R.dimen.proxy_text_margin
        ).toFloat()
    val titleTextSize
        get() = context.getPixels(
            if (compact) R.dimen.proxy_title_text_size_grid3 else R.dimen.proxy_title_text_size
        ).toFloat()
    val subtitleTextSize
        get() = context.getPixels(
            if (compact) R.dimen.proxy_subtitle_text_size_grid3 else R.dimen.proxy_subtitle_text_size
        ).toFloat()
    val delayTextSize = context.getPixels(R.dimen.proxy_delay_text_size).toFloat()
    val cardMinHeight
        get() = context.getPixels(
            if (compact) R.dimen.proxy_card_min_height_grid3 else R.dimen.proxy_card_min_height
        ).toFloat()

    val shadow = Color.argb(
        0x15,
        Color.red(Color.DKGRAY),
        Color.green(Color.DKGRAY),
        Color.blue(Color.DKGRAY),
    )

    val cardRadius = context.getPixels(R.dimen.proxy_card_radius).toFloat()
    var cardOffset = context.getPixels(R.dimen.proxy_card_offset).toFloat()
    val cardStroke = context.getPixels(R.dimen.proxy_card_stroke).toFloat()
    val selectedIndicatorWidth =
        context.getPixels(R.dimen.proxy_selected_indicator_width).toFloat()
    val delayPaddingHorizontal =
        context.getPixels(R.dimen.proxy_delay_padding_horizontal).toFloat()
    val delayPaddingVertical =
        context.getPixels(R.dimen.proxy_delay_padding_vertical).toFloat()
}
