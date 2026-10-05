package com.github.kr328.clash.design

import android.content.Context
import android.view.View

/**
 * Minimal lifecycle/error-reporting anchor for the Compose-based MainActivity.
 * The former XML dashboard is no longer part of the rendered UI.
 */
class MainDesign(context: Context) : Design<Nothing>(context) {
    override val root: View = View(context)
}
