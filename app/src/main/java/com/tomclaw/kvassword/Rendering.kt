package com.tomclaw.kvassword

import android.content.Context
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import androidx.core.content.ContextCompat
import com.tomclaw.kvassword.generator.ChunkType
import com.tomclaw.kvassword.generator.GeneratedPassword

/**
 * Renders a generated password as a colour-segmented spannable, colouring each
 * chunk by its semantic type — the visual basis for the colour legend.
 */
fun GeneratedPassword.toSpannable(context: Context): Spannable {
    val builder = SpannableStringBuilder()
    for (chunk in chunks) {
        val start = builder.length
        builder.append(chunk.text)
        val colorRes = when (chunk.type) {
            ChunkType.WORD -> R.color.segment_word
            ChunkType.DIGIT -> R.color.segment_digit
            ChunkType.SYMBOL -> R.color.segment_symbol
            ChunkType.LITERAL -> R.color.md_on_surface_variant
        }
        builder.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(context, colorRes)),
            start,
            builder.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }
    return builder
}
