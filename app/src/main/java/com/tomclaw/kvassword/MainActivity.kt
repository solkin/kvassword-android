package com.tomclaw.kvassword

import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.support.annotation.ColorRes
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

    private var nextPassword: Button? = null
    private var nextNickname: Button? = null
    private var password: TextView? = null
    private var nickname: TextView? = null
    private var strength: RadioGroup? = null
    private var switcher: ViewSwitcher? = null
    private var navigation: BottomNavigationView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val restoredPassword = savedInstanceState?.getCharSequence(KEY_PASSWORD)
        val restoredNickname = savedInstanceState?.getCharSequence(KEY_NICKNAME)
        val selectedItemId = savedInstanceState?.getInt(KEY_NAVIGATION) ?: NAVIGATION_INVALID

        initDictionary()

        nextPassword = findViewById(R.id.next_password)
        nextNickname = findViewById(R.id.next_nickname)
        password = findViewById(R.id.password)
        nickname = findViewById(R.id.nickname)
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
        ).forEach { it.setOnClickListener { onNextPasswordClick(it) } }

        nextPassword?.setOnClickListener { onNextPasswordClick(it) }
        nextNickname?.setOnClickListener { onNextNicknameClick() }
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

        if (selectedItemId >= 0) navigation?.selectedItemId = selectedItemId

        if (restoredPassword == null) {
            generatePassword()
            generateNickname()
        } else {
            password?.text = restoredPassword
            nickname?.text = restoredNickname
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
        outState.putCharSequence(KEY_NICKNAME, nickname?.text)
        outState.putInt(KEY_NAVIGATION, navigation?.selectedItemId ?: NAVIGATION_INVALID)
    }

    private fun initDictionary() {
        random = Random(System.currentTimeMillis())

        val reader = InputStreamReader(assets.open(DICTIONARY))
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

    private fun onNextPasswordClick(view: View) {
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
        generatePassword()
        playClickSound()
    }

    private fun onNextNicknameClick() {
        generateNickname()
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

    private fun generatePassword() {
        val passItems = when (strength?.checkedRadioButtonId) {
            R.id.pass_normal -> listOf(
                    randomWord.nextWord(6).toFirstUpper().toSpan(R.color.color1),
                    Span(
                            R.color.color2,
                            random.digit(),
                            random.digit()
                    )
            )
            R.id.pass_good -> listOf(
                    randomWord.nextWord(3).toFirstUpper().toSpan(R.color.color1),
                    random.digit().toSpan(R.color.color2),
                    randomWord.nextWord(3).toFirstUpper().toSpan(R.color.color3),
                    random.symbol().toSpan(R.color.color4)
            )
            R.id.pass_strong -> listOf(
                    randomWord.nextWord(3).toFirstUpper().toSpan(R.color.color1),
                    random.digit().toSpan(R.color.color2),
                    randomWord.nextWord(3).toFirstUpper().toSpan(R.color.color3),
                    Span(
                            R.color.color4,
                            random.symbol(),
                            random.digit()
                    ),
                    randomWord.nextWord(3).toUpperCase().toSpan(R.color.color5)
            )
            else -> throw IllegalStateException("Invalid selection")
        }
        val pass = passItems.concatItems(resources)
        password?.text = pass

        trackPasswordStrength()
    }

    private fun generateNickname() {
        val nickLength = 5 + random.nextInt(5)
        nickname?.text = randomWord.nextWord(nickLength)
                .toFirstUpper()
                .toSpan(R.color.color1)
                .toList()
                .concatItems(resources)

        MetricsManager.trackEvent("Generate Nickname")
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

    private fun String.toSpan(@ColorRes color: Int) = Span(color, this)

    private fun Span.toList() = listOf(this)

}

private const val DICTIONARY = "grammar.json"
private const val NAVIGATION_INVALID = -1

private const val KEY_PASSWORD = "password"
private const val KEY_NICKNAME = "nickname"
private const val KEY_NAVIGATION = "navigation"