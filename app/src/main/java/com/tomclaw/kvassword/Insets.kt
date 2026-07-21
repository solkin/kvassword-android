package com.tomclaw.kvassword

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/**
 * Applies system-bar insets as padding so content is not drawn under the status
 * or navigation bars in edge-to-edge mode (enforced on Android 15+ / SDK 35+).
 * The original padding is preserved and the inset is added on top of it.
 */
fun View.applySystemBarsPadding(top: Boolean = true, bottom: Boolean = true) {
    val initialTop = paddingTop
    val initialBottom = paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        v.updatePadding(
            top = if (top) initialTop + bars.top else v.paddingTop,
            bottom = if (bottom) initialBottom + bars.bottom else v.paddingBottom
        )
        insets
    }
}
