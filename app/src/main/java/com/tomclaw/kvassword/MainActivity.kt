package com.tomclaw.kvassword

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import android.widget.ViewFlipper
import androidx.annotation.RawRes
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import com.google.android.material.snackbar.Snackbar
import com.google.gson.GsonBuilder
import com.tomclaw.bananalytics.Bananalytics
import com.tomclaw.kvassword.generator.CUSTOM_PRESET_ID
import com.tomclaw.kvassword.generator.EntropyEstimator
import com.tomclaw.kvassword.generator.GrammarRepository
import com.tomclaw.kvassword.generator.LetterCase
import com.tomclaw.kvassword.generator.Mask
import com.tomclaw.kvassword.generator.MaskParser
import com.tomclaw.kvassword.generator.PasswordGenerator
import com.tomclaw.kvassword.generator.RandomEntropySource
import com.tomclaw.kvassword.generator.SitePreset
import com.tomclaw.kvassword.generator.Strength
import com.tomclaw.kvassword.generator.StrengthLevel
import com.tomclaw.kvassword.generator.StrengthPreset
import com.tomclaw.kvassword.generator.Token
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private val gson = GsonBuilder().create()
    private lateinit var settings: Settings
    private lateinit var bananalytics: Bananalytics

    private lateinit var randomWord: RandomWord
    private lateinit var entropy: EntropyEstimator
    private lateinit var generator: PasswordGenerator

    private val handler = Handler(Looper.getMainLooper())
    private val clipboardCleaner = Runnable { clearClipboard() }

    private var suppress = false
    private val siteMasks = HashMap<Int, Pair<String, Mask>>()

    private val maskBuilderLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) onMaskChanged()
    }

    // Views
    private lateinit var flipper: ViewFlipper
    private lateinit var navigation: BottomNavigationView
    private lateinit var coordinator: CoordinatorLayout
    private lateinit var password: TextView
    private lateinit var word: TextView
    private lateinit var strengthGroup: MaterialButtonToggleGroup
    private lateinit var sitePresets: ChipGroup
    private lateinit var strengthLevel: TextView
    private lateinit var strengthSummary: TextView
    private lateinit var strengthBars: View
    private lateinit var wordLengthSlider: Slider
    private lateinit var wordLengthValue: TextView
    private lateinit var wordCaseGroup: MaterialButtonToggleGroup
    private lateinit var wordOptions: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        suppress = true

        settings = Settings(this)
        bananalytics = (application as App).bananalytics

        setContentView(R.layout.activity_main)
        findViewById<View>(android.R.id.content).applySystemBarsPadding(top = true, bottom = false)
        initEngine()
        bindViews()
        setupNavigation()
        setupPasswordTab()
        setupWordTab()
        setupSettingsTab()

        if (savedInstanceState != null) {
            password.text = savedInstanceState.getCharSequence(KEY_PASSWORD)
            word.text = savedInstanceState.getCharSequence(KEY_WORD)
            updateMeter(savedInstanceState.getDouble(KEY_BITS))
            val nav = savedInstanceState.getInt(KEY_NAVIGATION, 0)
            navigation.selectedItemId = navItemId(nav)
        } else {
            generatePassword()
            generateWord()
        }

        suppress = false

        bananalytics.trackEvent("start")
    }

    private fun initEngine() {
        val grammar = GrammarRepository(assets, gson).load(settings.language)
        val source = RandomEntropySource()
        randomWord = RandomWord(grammar, source)
        entropy = EntropyEstimator(grammar)
        generator = PasswordGenerator(randomWord, source, entropy)
    }

    private fun bindViews() {
        flipper = findViewById(R.id.flipper)
        navigation = findViewById(R.id.bottom_navigation)
        coordinator = findViewById(R.id.coordinator)
        password = findViewById(R.id.password)
        word = findViewById(R.id.word)
        strengthGroup = findViewById(R.id.pass_strength)
        sitePresets = findViewById(R.id.site_presets)
        strengthLevel = findViewById(R.id.strength_level)
        strengthSummary = findViewById(R.id.strength_summary)
        strengthBars = findViewById(R.id.strength_bars)
        wordLengthSlider = findViewById(R.id.word_length)
        wordLengthValue = findViewById(R.id.word_length_value)
        wordCaseGroup = findViewById(R.id.word_case)
        wordOptions = findViewById(R.id.word_options)
    }

    private fun setupNavigation() {
        navigation.setOnItemSelectedListener { item ->
            val position = when (item.itemId) {
                R.id.password -> 0
                R.id.word -> 1
                R.id.settings -> 2
                else -> return@setOnItemSelectedListener false
            }
            if (flipper.displayedChild != position) flipper.displayedChild = position
            true
        }
    }

    // ---- Password tab ----

    private fun setupPasswordTab() {
        findViewById<MaterialButton>(R.id.next_password).setOnClickListener {
            generatePassword()
            playClickSound()
        }
        findViewById<View>(R.id.password_card).setOnClickListener { copyPassword() }
        findViewById<MaterialButton>(R.id.memorize).setOnClickListener { openMemorize() }

        buildSiteChips()
        // Exactly one source is active at a time: a strength preset OR a site chip.
        if (settings.sitePreset.isEmpty()) {
            strengthGroup.check(strengthButtonId(settings.strengthPreset))
        }

        strengthGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked || suppress) return@addOnButtonCheckedListener
            suppress = true
            sitePresets.clearCheck()
            suppress = false
            settings.strengthPreset = strengthPresetId(checkedId)
            settings.sitePreset = ""
            generatePassword()
        }

        sitePresets.setOnCheckedStateChangeListener { _, checkedIds ->
            if (suppress) return@setOnCheckedStateChangeListener
            val checked = checkedIds.firstOrNull()
            suppress = true
            if (checked != null) {
                strengthGroup.clearChecked()
                settings.sitePreset = siteMasks[checked]?.first ?: ""
            } else {
                strengthGroup.check(strengthButtonId(settings.strengthPreset))
                settings.sitePreset = ""
            }
            suppress = false
            generatePassword()
        }
    }

    private fun buildSiteChips() {
        sitePresets.removeAllViews()
        siteMasks.clear()
        val items = mutableListOf(
            Triple(SitePreset.NO_SYMBOLS.id, R.string.preset_no_symbols, SitePreset.NO_SYMBOLS.mask),
            Triple(SitePreset.ALPHANUMERIC.id, R.string.preset_alnum, SitePreset.ALPHANUMERIC.mask),
            Triple(SitePreset.PASSPHRASE.id, R.string.preset_passphrase, SitePreset.PASSPHRASE.mask),
            Triple(SitePreset.PIN.id, R.string.preset_pin, SitePreset.PIN.mask)
        )
        val custom = settings.customMask
        if (custom.isNotBlank()) {
            val mask = MaskParser.parse(custom)
            if (mask.tokens.isNotEmpty()) {
                items.add(Triple(CUSTOM_PRESET_ID, R.string.preset_custom, mask))
            }
        }
        for ((id, label, mask) in items) {
            val chip = layoutInflater.inflate(R.layout.chip_filter, sitePresets, false) as Chip
            chip.id = View.generateViewId()
            chip.text = getString(label)
            siteMasks[chip.id] = id to mask
            if (settings.sitePreset == id) chip.isChecked = true
            sitePresets.addView(chip)
        }
    }

    private fun activeMask(): Mask {
        val checkedChip = sitePresets.checkedChipId
        if (checkedChip != View.NO_ID) {
            siteMasks[checkedChip]?.let { return it.second }
        }
        return StrengthPreset.byId(settings.strengthPreset).mask
    }

    private fun activePresetId(): String {
        val checkedChip = sitePresets.checkedChipId
        if (checkedChip != View.NO_ID) {
            siteMasks[checkedChip]?.let { return it.first }
        }
        return settings.strengthPreset
    }

    private fun generatePassword() {
        val result = generator.generate(activeMask(), settings.excludeSimilar)
        password.text = result.toSpannable(this)
        updateMeter(Strength.charsetBits(result.plain))
        settings.lastPreset = activePresetId()
        trackPasswordStrength(activePresetId())
    }

    private fun updateMeter(bits: Double) {
        lastBits = bits
        val levelText = getString(
            when (Strength.level(bits)) {
                StrengthLevel.WEAK -> R.string.strength_weak
                StrengthLevel.FAIR -> R.string.strength_fair
                StrengthLevel.GOOD -> R.string.strength_good
                StrengthLevel.STRONG -> R.string.strength_strong
            }
        )
        val crackText = getString(
            when (Strength.crackTime(bits)) {
                com.tomclaw.kvassword.generator.CrackTime.INSTANT -> R.string.crack_instant
                com.tomclaw.kvassword.generator.CrackTime.MINUTES -> R.string.crack_minutes
                com.tomclaw.kvassword.generator.CrackTime.HOURS -> R.string.crack_hours
                com.tomclaw.kvassword.generator.CrackTime.DAYS -> R.string.crack_days
                com.tomclaw.kvassword.generator.CrackTime.YEARS -> R.string.crack_years
                com.tomclaw.kvassword.generator.CrackTime.CENTURIES -> R.string.crack_centuries
                com.tomclaw.kvassword.generator.CrackTime.AGES -> R.string.crack_ages
            }
        )
        strengthLevel.text = levelText
        strengthSummary.text = getString(
            R.string.meter_summary,
            getString(R.string.bits_format, bits.roundToInt()),
            crackText
        )

        val filled = Strength.bars(bits)
        val on = ContextCompat.getColor(this, R.color.md_success)
        val off = ContextCompat.getColor(this, R.color.md_outline_variant)
        val bars = strengthBars as? android.view.ViewGroup ?: return
        for (i in 0 until bars.childCount) {
            bars.getChildAt(i).backgroundTintList =
                ColorStateList.valueOf(if (i < filled) on else off)
        }
    }

    // ---- Word tab ----

    private fun setupWordTab() {
        findViewById<MaterialButton>(R.id.next_word).setOnClickListener {
            generateWord()
            playClickSound()
        }
        findViewById<View>(R.id.word_card).setOnClickListener { copyWord() }
        findViewById<MaterialButton>(R.id.word_options_toggle).setOnClickListener {
            wordOptions.visibility =
                if (wordOptions.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        val storedLength = settings.wordLength.coerceIn(4, 12)
        wordLengthSlider.value = storedLength.toFloat()
        wordLengthValue.text = storedLength.toString()
        wordLengthSlider.addOnChangeListener { _, value, fromUser ->
            wordLengthValue.text = value.roundToInt().toString()
            if (fromUser && !suppress) {
                settings.wordLength = value.roundToInt()
                generateWord()
            }
        }

        wordCaseGroup.check(wordCaseButtonId(settings.wordCase))
        wordCaseGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked || suppress) return@addOnButtonCheckedListener
            settings.wordCase = wordCaseId(checkedId)
            generateWord()
        }
    }

    private fun wordMask(): Mask {
        val length = settings.wordLength
        val case = when (settings.wordCase) {
            "lower" -> LetterCase.LOWER
            "upper" -> LetterCase.UPPER
            else -> LetterCase.CAPITALIZED
        }
        return Mask(listOf(Token.Word(length, length, case)))
    }

    private fun generateWord() {
        val result = generator.generate(wordMask(), settings.excludeSimilar)
        word.text = result.toSpannable(this)
        bananalytics.trackEvent("Generate Word", "case", settings.wordCase)
    }

    // ---- Settings tab ----

    private fun setupSettingsTab() {
        val langGroup = findViewById<ChipGroup>(R.id.lang_group)
        findViewById<Chip>(if (settings.language == "ru") R.id.chip_ru else R.id.chip_en).isChecked = true
        langGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (suppress) return@setOnCheckedStateChangeListener
            val lang = when (checkedIds.firstOrNull()) {
                R.id.chip_ru -> "ru"
                R.id.chip_en -> "en"
                else -> return@setOnCheckedStateChangeListener
            }
            if (lang != settings.language) {
                settings.language = lang
                initEngine()
                generatePassword()
                generateWord()
                bananalytics.trackEvent("Change Language", "language", lang)
            }
        }

        val excludeSwitch = findViewById<MaterialSwitch>(R.id.switch_exclude)
        excludeSwitch.isChecked = settings.excludeSimilar
        excludeSwitch.setOnCheckedChangeListener { _, checked ->
            if (suppress) return@setOnCheckedChangeListener
            settings.excludeSimilar = checked
            generatePassword()
            generateWord()
            bananalytics.trackEvent("Exclude Similar", "enabled", checked.toString())
        }

        updateMaskSummary()
        findViewById<View>(R.id.custom_mask_row).setOnClickListener {
            maskBuilderLauncher.launch(Intent(this, MaskBuilderActivity::class.java))
        }

        val autoClearSwitch = findViewById<MaterialSwitch>(R.id.switch_autoclear)
        autoClearSwitch.isChecked = settings.autoClearClipboard
        autoClearSwitch.setOnCheckedChangeListener { _, checked ->
            settings.autoClearClipboard = checked
            bananalytics.trackEvent("Auto Clear Clipboard", "enabled", checked.toString())
        }

        val soundSwitch = findViewById<MaterialSwitch>(R.id.switch_sound)
        soundSwitch.isChecked = settings.soundEnabled
        soundSwitch.setOnCheckedChangeListener { _, checked ->
            settings.soundEnabled = checked
            bananalytics.trackEvent("Sound", "enabled", checked.toString())
        }

        findViewById<TextView>(R.id.app_version).text = provideVersion()
        findViewById<View>(R.id.rate_app).setOnClickListener { onRateAppClick() }
        findViewById<View>(R.id.all_projects).setOnClickListener { onAllProjectsClick() }
    }

    private fun updateMaskSummary() {
        findViewById<TextView>(R.id.custom_mask_summary).text =
            if (settings.customMask.isBlank()) getString(R.string.custom_mask_none) else settings.customMask
    }

    private fun onMaskChanged() {
        updateMaskSummary()
        suppress = true
        buildSiteChips()
        if (settings.sitePreset == CUSTOM_PRESET_ID) strengthGroup.clearChecked()
        suppress = false
        generatePassword()
    }

    // ---- Clipboard / sound ----

    private fun copyPassword() {
        copy(password.text.toString())
        bananalytics.trackEvent("Copy Password", "preset", activePresetId())
    }

    private fun copyWord() {
        copy(word.text.toString())
        bananalytics.trackEvent("Copy Word")
    }

    private fun copy(text: String) {
        text.copyToClipboard(applicationContext)
        Snackbar.make(coordinator, R.string.copied, Snackbar.LENGTH_SHORT).show()
        playCopySound()
        handler.removeCallbacks(clipboardCleaner)
        if (settings.autoClearClipboard) {
            handler.postDelayed(clipboardCleaner, Settings.CLIPBOARD_CLEAR_DELAY_MS)
        }
    }

    private fun clearClipboard() {
        val manager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        manager.setPrimaryClip(ClipData.newPlainText("", ""))
    }

    private fun openMemorize() {
        val plain = password.text.toString()
        if (plain.isEmpty()) return
        bananalytics.trackEvent("Open Memorize")
        startActivity(Intent(this, MemorizeActivity::class.java).putExtra(MemorizeActivity.EXTRA_PASSWORD, plain))
    }

    private fun playClickSound() {
        if (settings.soundEnabled) playSound(R.raw.click)
    }

    private fun playCopySound() {
        if (settings.soundEnabled) playSound(R.raw.copy)
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

    // ---- Analytics / links ----

    private fun onRateAppClick() {
        val appPackageName = packageName
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackageName")))
        } catch (ex: android.content.ActivityNotFoundException) {
            startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName"))
            )
        }
        bananalytics.trackEvent("Open rate app")
    }

    private fun onAllProjectsClick() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=pub:TomClaw+Software")))
        } catch (ex: android.content.ActivityNotFoundException) {
            startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/developer?id=TomClaw+Software"))
            )
        }
        bananalytics.trackEvent("Open all projects")
    }

    private fun trackPasswordStrength(presetId: String) {
        bananalytics.trackEvent("Generate Password", "strength", presetId)
    }

    private fun provideVersion(): String {
        try {
            val info = packageManager.getPackageInfo(packageName, 0)
            val version: Long = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                @Suppress("DEPRECATION")
                info.versionCode.toLong()
            } else {
                info.longVersionCode
            }
            return resources.getString(R.string.app_version, info.versionName, version)
        } catch (ignored: PackageManager.NameNotFoundException) {
        }
        return ""
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putCharSequence(KEY_PASSWORD, password.text)
        outState.putCharSequence(KEY_WORD, word.text)
        outState.putDouble(KEY_BITS, lastBits)
        outState.putInt(KEY_NAVIGATION, flipper.displayedChild)
    }

    private var lastBits: Double = 0.0

    private fun navItemId(position: Int): Int = when (position) {
        1 -> R.id.word
        2 -> R.id.settings
        else -> R.id.password
    }

    private fun strengthButtonId(presetId: String): Int = when (StrengthPreset.byId(presetId)) {
        StrengthPreset.NORMAL -> R.id.pass_normal
        StrengthPreset.GOOD -> R.id.pass_good
        StrengthPreset.STRONG -> R.id.pass_strong
    }

    private fun strengthPresetId(buttonId: Int): String = when (buttonId) {
        R.id.pass_normal -> StrengthPreset.NORMAL.id
        R.id.pass_strong -> StrengthPreset.STRONG.id
        else -> StrengthPreset.GOOD.id
    }

    private fun wordCaseButtonId(case: String): Int = when (case) {
        "lower" -> R.id.case_lower
        "upper" -> R.id.case_upper
        else -> R.id.case_capitalized
    }

    private fun wordCaseId(buttonId: Int): String = when (buttonId) {
        R.id.case_lower -> "lower"
        R.id.case_upper -> "upper"
        else -> "cap"
    }

    override fun onDestroy() {
        handler.removeCallbacks(clipboardCleaner)
        super.onDestroy()
    }

    private companion object {
        const val KEY_PASSWORD = "password"
        const val KEY_WORD = "word"
        const val KEY_BITS = "bits"
        const val KEY_NAVIGATION = "navigation"
    }
}
