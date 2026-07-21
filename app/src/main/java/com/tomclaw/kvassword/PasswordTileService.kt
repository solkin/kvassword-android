package com.tomclaw.kvassword

import android.annotation.TargetApi
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.google.gson.GsonBuilder
import com.tomclaw.kvassword.generator.EntropyEstimator
import com.tomclaw.kvassword.generator.GrammarRepository
import com.tomclaw.kvassword.generator.PasswordGenerator
import com.tomclaw.kvassword.generator.RandomEntropySource
import com.tomclaw.kvassword.generator.maskForPreset

/**
 * Quick Settings tile: generates a password with the last-used preset and copies
 * it to the clipboard, straight from the notification shade without opening the app.
 */
@RequiresApi(Build.VERSION_CODES.N)
@TargetApi(Build.VERSION_CODES.N)
class PasswordTileService : TileService() {

    override fun onClick() {
        super.onClick()
        val settings = Settings(this)
        val gson = GsonBuilder().create()
        val grammar = GrammarRepository(assets, gson).load(settings.language)
        val source = RandomEntropySource()
        val randomWord = RandomWord(grammar, source)
        val generator = PasswordGenerator(randomWord, source, EntropyEstimator(grammar))

        val mask = maskForPreset(settings.lastPreset, settings.customMask)
        val password = generator.generate(mask, settings.excludeSimilar).plain

        copyToClipboard(password)
        Toast.makeText(this, R.string.tile_copied, Toast.LENGTH_SHORT).show()
    }

    private fun copyToClipboard(text: String) {
        val manager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        manager.setPrimaryClip(ClipData.newPlainText("password", text))
    }
}
