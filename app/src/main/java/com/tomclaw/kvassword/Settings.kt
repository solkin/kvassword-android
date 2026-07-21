package com.tomclaw.kvassword

import android.content.Context
import android.content.SharedPreferences
import com.tomclaw.kvassword.generator.GrammarRepository
import com.tomclaw.kvassword.generator.StrengthPreset
import java.util.UUID

/**
 * Persisted user preferences, backed by SharedPreferences.
 */
class Settings(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    var language: String
        get() = prefs.getString(KEY_LANGUAGE, defaultLanguage()) ?: defaultLanguage()
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()

    var excludeSimilar: Boolean
        get() = prefs.getBoolean(KEY_EXCLUDE_SIMILAR, false)
        set(value) = prefs.edit().putBoolean(KEY_EXCLUDE_SIMILAR, value).apply()

    var autoClearClipboard: Boolean
        get() = prefs.getBoolean(KEY_AUTO_CLEAR, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_CLEAR, value).apply()

    var soundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND, value).apply()

    var customMask: String
        get() = prefs.getString(KEY_CUSTOM_MASK, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CUSTOM_MASK, value).apply()

    var lastPreset: String
        get() = prefs.getString(KEY_LAST_PRESET, StrengthPreset.GOOD.id) ?: StrengthPreset.GOOD.id
        set(value) = prefs.edit().putString(KEY_LAST_PRESET, value).apply()

    var strengthPreset: String
        get() = prefs.getString(KEY_STRENGTH_PRESET, StrengthPreset.GOOD.id) ?: StrengthPreset.GOOD.id
        set(value) = prefs.edit().putString(KEY_STRENGTH_PRESET, value).apply()

    var sitePreset: String
        get() = prefs.getString(KEY_SITE_PRESET, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SITE_PRESET, value).apply()

    var wordLength: Int
        get() = prefs.getInt(KEY_WORD_LENGTH, 6)
        set(value) = prefs.edit().putInt(KEY_WORD_LENGTH, value).apply()

    var wordCase: String
        get() = prefs.getString(KEY_WORD_CASE, "cap") ?: "cap"
        set(value) = prefs.edit().putString(KEY_WORD_CASE, value).apply()

    /** Stable per-install identifier for analytics. */
    val deviceId: String
        get() {
            var id = prefs.getString(KEY_DEVICE_ID, null)
            if (id == null) {
                id = UUID.randomUUID().toString()
                prefs.edit().putString(KEY_DEVICE_ID, id).apply()
            }
            return id
        }

    private fun defaultLanguage(): String = GrammarRepository.DEFAULT_LANGUAGE

    companion object {
        private const val NAME = "clever_password"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_EXCLUDE_SIMILAR = "exclude_similar"
        private const val KEY_AUTO_CLEAR = "auto_clear_clipboard"
        private const val KEY_SOUND = "sound_enabled"
        private const val KEY_CUSTOM_MASK = "custom_mask"
        private const val KEY_LAST_PRESET = "last_preset"
        private const val KEY_STRENGTH_PRESET = "strength_preset"
        private const val KEY_SITE_PRESET = "site_preset"
        private const val KEY_WORD_LENGTH = "word_length"
        private const val KEY_WORD_CASE = "word_case"
        private const val KEY_DEVICE_ID = "device_id"

        const val CLIPBOARD_CLEAR_DELAY_MS = 30_000L
    }
}
