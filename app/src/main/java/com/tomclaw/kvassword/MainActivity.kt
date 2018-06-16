package com.tomclaw.kvassword

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.content.res.Resources
import android.media.MediaPlayer
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import com.google.gson.GsonBuilder
import net.hockeyapp.android.CrashManager
import net.hockeyapp.android.metrics.MetricsManager
import java.io.IOException
import java.io.InputStreamReader
import java.util.Random


class MainActivity : AppCompatActivity() {

    private lateinit var random: Random
    private lateinit var randomWord: RandomWord

    private var button: Button? = null
    private var password: TextView? = null
    private var strength: RadioGroup? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        button = findViewById(R.id.button)
        password = findViewById(R.id.password)
        strength = findViewById(R.id.pass_strength)

        random = Random(System.currentTimeMillis())

        val reader = InputStreamReader(assets.open("grammar.json"))
        val grammar: Grammar
        try {
            val gson = GsonBuilder().create()
            grammar = gson.fromJson(reader, Grammar::class.java)
        } finally {
            try {
                reader.close()
            } catch (ignored: IOException) {
            }
        }
        randomWord = RandomWord(grammar)

        strength?.setOnCheckedChangeListener { _, _ ->
            generate()
            playClickSound()
        }
        button?.setOnClickListener {
            generate()
            playClickSound()
        }

        generate()

        MetricsManager.register(application)
    }

    private fun playClickSound() {
        MediaPlayer.create(applicationContext, R.raw.click).apply {
            setOnCompletionListener { it.release() }
            start()
        }
    }

    public override fun onResume() {
        super.onResume()
        checkForCrashes()
    }

    private fun generate() {
        val passItems = when (strength?.checkedRadioButtonId) {
            R.id.pass_good -> listOf(
                    Span(R.color.color1, randomWord.nextWord(6).toFirstUpper()),
                    Span(
                            R.color.color2,
                            randomDigit(),
                            randomDigit()
                    )
            )
            R.id.pass_strong -> listOf(
                    Span(R.color.color1, randomWord.nextWord(3).toFirstUpper()),
                    Span(R.color.color2, randomDigit()),
                    Span(R.color.color3, randomWord.nextWord(3).toFirstUpper()),
                    Span(R.color.color4, randomSymbol())
            )
            R.id.pass_very_strong -> listOf(
                    Span(R.color.color1, randomWord.nextWord(3).toFirstUpper()),
                    Span(R.color.color2, randomDigit()),
                    Span(R.color.color3, randomWord.nextWord(3).toFirstUpper()),
                    Span(
                            R.color.color4,
                            randomSymbol(),
                            randomDigit()
                    ),
                    Span(R.color.color5, randomWord.nextWord(3).toUpperCase())
            )
            else -> throw IllegalStateException("Invalid selection")
        }
        val pass = passItems.concatItems(resources)
        password?.text = pass
        copyStringToClipboard(context = this, string = pass.toString())

        trackPasswordStrength()
    }

    private fun randomDigit(): String = (1 + random.nextInt(9)).toString()

    private fun randomSymbol(): String {
        val symbols = "!@#\$%&*+=?"
        val i = random.nextInt(symbols.length)
        return symbols.substring(i, i + 1)
    }

    private fun checkForCrashes() {
        CrashManager.register(this)
    }

    private fun trackPasswordStrength() {
        when (strength?.checkedRadioButtonId) {
            R.id.pass_good -> "good"
            R.id.pass_strong -> "strong"
            R.id.pass_very_strong -> "very_strong"
            else -> null
        }?.let {
            val properties = hashMapOf("strength" to it)
            MetricsManager.trackEvent("Generate Password", properties)
        }
    }
}

private fun List<Span>.concatItems(resources: Resources): Spannable {
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

private fun Array<out String>.concat(): String {
    var string = ""
    forEach { string += it }
    return string
}

private fun String.toFirstUpper(): String {
    return substring(0, 1).toUpperCase() + substring(1).toLowerCase()
}

private fun copyStringToClipboard(context: Context, string: String, toastText: Int = 0) {
    val clipboardManager = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
    clipboardManager.primaryClip = ClipData.newPlainText("", string)
    if (toastText > 0) {
        Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show()
    }
}
