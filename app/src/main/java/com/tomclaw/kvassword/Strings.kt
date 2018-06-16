package com.tomclaw.kvassword

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Resources
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import java.util.Random

fun Random.digit(): String = (1 + nextInt(9)).toString()

fun Random.symbol(): String {
    val symbols = "!@#\$%&*+=?"
    val i = nextInt(symbols.length)
    return symbols.substring(i, i + 1)
}

fun Array<out String>.concat(): String {
    var string = ""
    forEach { string += it }
    return string
}

fun String.toFirstUpper(): String {
    return substring(0, 1).toUpperCase() + substring(1).toLowerCase()
}

fun List<Span>.concatItems(resources: Resources): Spannable {
    var string = ""
    forEach { string += it.text.concat() }
    val spannable = SpannableString(string)
    var position = 0
    forEach { span ->
        val start = position
        val end = position + span.text.concat().length
        spannable.setSpan(
                ForegroundColorSpan(resources.getColor(span.color)),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        position = end
    }
    return spannable
}

fun String.copyToClipboard(context: Context) {
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboardManager.primaryClip = ClipData.newPlainText("", this)
}