package com.github.kr328.clash.design.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.databinding.ComponentAuroraSettingsRowBinding
import com.github.kr328.clash.design.util.layoutInflater
import com.github.kr328.clash.design.util.resolveClickableAttrs
import com.github.kr328.clash.design.util.selectableItemBackground

class AuroraSettingsRow @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
) : FrameLayout(context, attributeSet, defStyleAttr) {
    private val binding = ComponentAuroraSettingsRowBinding
        .inflate(context.layoutInflater, this, true)

    var text: CharSequence?
        get() = binding.textView.text
        set(value) { binding.textView.text = value }

    var subtext: CharSequence?
        get() = binding.subtextView.text
        set(value) {
            binding.subtextView.text = value
            binding.subtextView.visibility = if (value.isNullOrEmpty()) View.GONE else View.VISIBLE
        }

    var icon: Drawable?
        get() = binding.iconView.drawable
        set(value) { binding.iconView.setImageDrawable(value) }

    init {
        context.resolveClickableAttrs(attributeSet, defStyleAttr) {
            isFocusable = focusable(true)
            isClickable = clickable(true)
            foreground = foreground() ?: context.selectableItemBackground
        }

        context.theme.obtainStyledAttributes(
            attributeSet,
            R.styleable.AuroraSettingsRow,
            defStyleAttr,
            0,
        ).apply {
            try {
                icon = getDrawable(R.styleable.AuroraSettingsRow_icon)
                text = getString(R.styleable.AuroraSettingsRow_text)
                subtext = getString(R.styleable.AuroraSettingsRow_subtext)
            } finally {
                recycle()
            }
        }
    }
}
