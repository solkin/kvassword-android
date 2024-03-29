package com.tomclaw.kvassword

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.ViewFlipper
import androidx.annotation.ColorRes
import androidx.annotation.RawRes
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.gson.GsonBuilder
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.analytics.Analytics.trackEvent
import com.microsoft.appcenter.crashes.Crashes
import com.tomclaw.kvassword.bananalytics.Bananalytics
import com.tomclaw.kvassword.bananalytics.InfoProvider
import java.io.InputStreamReader
import java.util.Locale
import java.util.Random


class MainActivity : AppCompatActivity() {

    private lateinit var random: Random
    private lateinit var randomWord: RandomWord
    private val gson = GsonBuilder().create()

    private lateinit var bananalytics: Bananalytics

    private var nextPassword: Button? = null
    private var nextNickname: Button? = null
    private var password: TextView? = null
    private var nickname: TextView? = null
    private var strength: RadioGroup? = null
    private var flipper: ViewFlipper? = null
    private var navigation: BottomNavigationView? = null
    private var coordinator: CoordinatorLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bananalytics = Bananalytics(filesDir, InfoProvider(this), gson)

        setContentView(R.layout.activity_main)

        initDictionary()

        val restoredPassword = savedInstanceState?.getCharSequence(KEY_PASSWORD)
        val restoredNickname = savedInstanceState?.getCharSequence(KEY_NICKNAME)
        val selectedItemId = savedInstanceState?.getInt(KEY_NAVIGATION) ?: NAVIGATION_INVALID

        nextPassword = findViewById(R.id.next_password)
        nextNickname = findViewById(R.id.next_nickname)
        password = findViewById(R.id.password)
        nickname = findViewById(R.id.nickname)
        strength = findViewById(R.id.pass_strength)
        flipper = findViewById(R.id.flipper)
        navigation = findViewById(R.id.bottom_navigation)
        coordinator = findViewById(R.id.coordinator)

        findViewById<TextView>(R.id.app_version).text = provideVersion()
        findViewById<TextView>(R.id.rate_app).setOnClickListener { onRateAppClick() }
        findViewById<TextView>(R.id.all_projects).setOnClickListener { onAllProjectsClick() }

        flipper?.initFadeAnimations()

        listOf<View>(
            findViewById(R.id.pass_normal),
            findViewById(R.id.pass_normal_description),
            findViewById(R.id.pass_good),
            findViewById(R.id.pass_good_description),
            findViewById(R.id.pass_strong),
            findViewById(R.id.pass_strong_description)
        ).forEach { view -> view.setOnClickListener { onNextPasswordClick(it) } }

        nextPassword?.setOnClickListener { onNextPasswordClick(it) }
        nextNickname?.setOnClickListener { onNextNicknameClick() }
        password?.copyClickListener()
        nickname?.copyClickListener()

        navigation?.setOnItemSelectedListener { item ->
            val position = when (item.itemId) {
                R.id.password -> 0
                R.id.nickname -> 1
                R.id.information -> 2
                else -> throw IllegalStateException()
            }
            if (flipper?.displayedChild != position) {
                flipper?.displayedChild = position
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

        register(application)
        bananalytics.trackEvent("start")
    }

    fun register(application: Application) {
        val appIdentifier = getAppIdentifier(application.applicationContext)
        require(!(appIdentifier == null || appIdentifier.isEmpty())) { "AppCenter app identifier was not configured correctly in manifest or build configuration." }
        AppCenter.start(getApplication(), appIdentifier, Analytics::class.java, Crashes::class.java)
    }

    private fun getAppIdentifier(context: Context): String? {
        val appIdentifier = getManifestString(context, APP_IDENTIFIER_KEY)
        require(!TextUtils.isEmpty(appIdentifier)) { "AppCenter app identifier was not configured correctly in manifest or build configuration." }
        return appIdentifier
    }

    @Suppress("SameParameterValue")
    private fun getManifestString(context: Context, key: String): String? {
        return getManifestBundle(context).getString(key)
    }

    private fun getManifestBundle(context: Context): Bundle {
        return try {
            context.packageManager.getApplicationInfo(
                context.packageName,
                PackageManager.GET_META_DATA
            ).metaData
        } catch (e: PackageManager.NameNotFoundException) {
            throw RuntimeException(e)
        }
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

    private fun onRateAppClick() {
        val appPackageName = packageName
        try {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=$appPackageName")
                )
            )
        } catch (ex: android.content.ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")
                )
            )
        }
        trackEvent("Open rate app")
        bananalytics.trackEvent("Open rate app")
    }

    private fun onAllProjectsClick() {
        try {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://search?q=pub:TomClaw+Software")
                )
            )
        } catch (ex: android.content.ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/developer?id=TomClaw+Software")
                )
            )
        }
        trackEvent("Open all projects")
        bananalytics.trackEvent("Open all projects")
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
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
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
                randomWord.nextWord(3).toUpperCase(Locale.getDefault()).toSpan(R.color.color5)
            )
            else -> throw IllegalStateException("Invalid selection")
        }
        val pass = passItems.concatItems(resources)
        password?.text = pass

        trackPasswordStrength()
    }

    private fun generateNickname() {
        val nickLength = 4 + random.nextInt(5)
        nickname?.text = randomWord.nextWord(nickLength)
            .toFirstUpper()
            .toSpan(R.color.color1)
            .toList()
            .concatItems(resources)
        trackEvent("Generate Nickname")
        bananalytics.trackEvent("Generate Nickname")
    }

    private fun trackPasswordStrength() {
        when (strength?.checkedRadioButtonId) {
            R.id.pass_normal -> "normal"
            R.id.pass_good -> "good"
            R.id.pass_strong -> "strong"
            else -> null
        }?.let {
            val properties = hashMapOf("strength" to it)
            trackEvent("Generate Password", properties)
            bananalytics.trackEvent("Generate Password", gson.toJson(properties))
        }
    }

    private fun provideVersion(): String {
        try {
            val info = packageManager.getPackageInfo(packageName, 0)
            val version: Long
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                @Suppress("DEPRECATION")
                version = info.versionCode.toLong()
            } else {
                version = info.longVersionCode
            }
            return resources.getString(R.string.app_version, info.versionName, version)
        } catch (ignored: PackageManager.NameNotFoundException) {
        }
        return ""
    }

    private fun String.toSpan(@ColorRes color: Int) = Span(color, this)

    private fun Span.toList() = listOf(this)

    private fun TextView.copyClickListener() {
        setOnClickListener {
            (it as TextView?)?.text.toString().copyToClipboard(context = applicationContext)
            coordinator?.let { cl ->
                Snackbar.make(cl, R.string.copied, Snackbar.LENGTH_SHORT).show()
                playCopySound()
            }
        }
    }

    private fun ViewFlipper.initFadeAnimations() {
        inAnimation = AlphaAnimation(0f, 1f).apply {
            interpolator = DecelerateInterpolator()
            startOffset = 100
            duration = 200
        }

        outAnimation = AlphaAnimation(1f, 0f).apply {
            interpolator = AccelerateInterpolator()
            duration = 200
        }
    }

}

private const val DICTIONARY = "grammar.json"
private const val NAVIGATION_INVALID = -1

private const val KEY_PASSWORD = "password"
private const val KEY_NICKNAME = "nickname"
private const val KEY_NAVIGATION = "navigation"

private const val APP_IDENTIFIER_KEY = "appcenter.app_identifier"