package com.tomclaw.kvassword

import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.support.annotation.RawRes
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView
import com.google.gson.GsonBuilder
import net.hockeyapp.android.CrashManager
import net.hockeyapp.android.metrics.MetricsManager
import java.io.InputStreamReader
import java.util.Random

class MainActivity : AppCompatActivity() {

    private lateinit var random: Random
    private lateinit var randomWord: RandomWord

    private var rootView: View? = null
    private var button: Button? = null
    private var password: TextView? = null
    private var strength: RadioGroup? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initDictionary()

        rootView = findViewById(R.id.root_view)
        button = findViewById(R.id.button)
        password = findViewById(R.id.password)
        strength = findViewById(R.id.pass_strength)

        strength?.setOnCheckedChangeListener { _, _ ->
            generate()
            playClickSound()
        }
        button?.setOnClickListener {
            generate()
            playClickSound()
        }
        password?.setOnClickListener {
            toString().copyToClipboard(context = applicationContext)
            rootView?.let {
                Snackbar.make(it, R.string.copied, Snackbar.LENGTH_SHORT).show()
                playCopySound()
            }
        }

        generate()

        MetricsManager.register(application)
    }

    public override fun onResume() {
        super.onResume()
        checkForCrashes()
    }

    private fun initDictionary() {
        random = Random(System.currentTimeMillis())

        val reader = InputStreamReader(assets.open("grammar.json"))
        val grammar: Grammar
        try {
            val gson = GsonBuilder().create()
            grammar = gson.fromJson(reader, Grammar::class.java)
        } finally {
            try {
                reader.close()
            } catch (ignored: Throwable) {
            }
        }
        randomWord = RandomWord(grammar)
    }

    private fun playClickSound() {
        playSound(R.raw.click)
    }

    private fun playCopySound() {
        playSound(R.raw.copy)
    }

    private fun playSound(@RawRes sound: Int) {
        try {
            val uri = Uri.parse("android.resource://$packageName/$sound")
            MediaPlayer().apply {
                setAudioStreamType(AudioManager.STREAM_SYSTEM)
                setDataSource(applicationContext, uri)
                setOnCompletionListener { it.release() }
                prepare()
                start()
            }
        } catch (ignored: Throwable) {
        }
    }

    private fun generate() {
        val passItems = when (strength?.checkedRadioButtonId) {
            R.id.pass_good -> listOf(
                    Span(R.color.color1, randomWord.nextWord(6).toFirstUpper()),
                    Span(
                            R.color.color2,
                            random.digit(),
                            random.digit()
                    )
            )
            R.id.pass_strong -> listOf(
                    Span(R.color.color1, randomWord.nextWord(3).toFirstUpper()),
                    Span(R.color.color2, random.digit()),
                    Span(R.color.color3, randomWord.nextWord(3).toFirstUpper()),
                    Span(R.color.color4, random.symbol())
            )
            R.id.pass_very_strong -> listOf(
                    Span(R.color.color1, randomWord.nextWord(3).toFirstUpper()),
                    Span(R.color.color2, random.digit()),
                    Span(R.color.color3, randomWord.nextWord(3).toFirstUpper()),
                    Span(
                            R.color.color4,
                            random.symbol(),
                            random.digit()
                    ),
                    Span(R.color.color5, randomWord.nextWord(3).toUpperCase())
            )
            else -> throw IllegalStateException("Invalid selection")
        }
        val pass = passItems.concatItems(resources)
        password?.text = pass

        trackPasswordStrength()
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

    private fun checkForCrashes() {
        CrashManager.register(this)
    }
}
