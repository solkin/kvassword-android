package com.tomclaw.kvassword

import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.support.annotation.RawRes
import android.support.design.widget.BottomNavigationView
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.ViewSwitcher
import com.google.gson.GsonBuilder
import net.hockeyapp.android.CrashManager
import net.hockeyapp.android.metrics.MetricsManager
import java.io.InputStreamReader
import java.util.Random

class MainActivity : AppCompatActivity() {

    private lateinit var random: Random
    private lateinit var randomWord: RandomWord

    private var button: Button? = null
    private var password: TextView? = null
    private var strength: RadioGroup? = null
    private var switcher: ViewSwitcher? = null
    private var navigation: BottomNavigationView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val restoredPassword = savedInstanceState?.getCharSequence(KEY_PASSWORD)

        initDictionary()

        button = findViewById(R.id.button)
        password = findViewById(R.id.password)
        strength = findViewById(R.id.pass_strength)
        switcher = findViewById(R.id.switcher)
        navigation = findViewById(R.id.bottom_navigation)

        listOf<View>(
                findViewById(R.id.pass_normal),
                findViewById(R.id.pass_normal_description),
                findViewById(R.id.pass_good),
                findViewById(R.id.pass_good_description),
                findViewById(R.id.pass_strong),
                findViewById(R.id.pass_strong_description)
        ).forEach { it.setOnClickListener { onClick(it) } }

        button?.setOnClickListener { onClick(it) }
        password?.setOnClickListener {
            password?.text.toString().copyToClipboard(context = applicationContext)
            switcher?.let {
                Snackbar.make(it, R.string.copied, Snackbar.LENGTH_SHORT).show()
                playCopySound()
            }
        }

        navigation?.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.password -> switcher?.displayedChild = 0
                R.id.nickname -> switcher?.displayedChild = 1
            }
            true
        }

        if (restoredPassword == null) {
            generate()
        } else {
            password?.text = restoredPassword
        }

        MetricsManager.register(application)
    }

    public override fun onResume() {
        super.onResume()
        checkForCrashes()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putCharSequence(KEY_PASSWORD, password?.text)
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

    private fun onClick(view: View) {
        when (view.id) {
            R.id.pass_normal, R.id.pass_normal_description -> {
                strength?.check(R.id.pass_normal)
            }
            R.id.pass_good, R.id.pass_good_description -> {
                strength?.check(R.id.pass_good)
            }
            R.id.pass_strong, R.id.pass_strong_description -> {
                strength?.check(R.id.pass_strong)
            }
        }
        generate()
        playClickSound()
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
            R.id.pass_normal -> listOf(
                    Span(R.color.color1, randomWord.nextWord(6).toFirstUpper()),
                    Span(
                            R.color.color2,
                            random.digit(),
                            random.digit()
                    )
            )
            R.id.pass_good -> listOf(
                    Span(R.color.color1, randomWord.nextWord(3).toFirstUpper()),
                    Span(R.color.color2, random.digit()),
                    Span(R.color.color3, randomWord.nextWord(3).toFirstUpper()),
                    Span(R.color.color4, random.symbol())
            )
            R.id.pass_strong -> listOf(
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
            R.id.pass_normal -> "normal"
            R.id.pass_good -> "good"
            R.id.pass_strong -> "strong"
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

private const val KEY_PASSWORD = "password"