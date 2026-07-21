package com.tomclaw.kvassword.generator

import android.content.res.AssetManager
import com.google.gson.Gson
import com.tomclaw.kvassword.Grammar
import java.io.IOException
import java.io.InputStreamReader

/**
 * Loads the bigram grammar for a given language from assets. Each language ships
 * as `grammar_<code>.json`; unknown codes fall back to English.
 */
class GrammarRepository(
    private val assets: AssetManager,
    private val gson: Gson
) {

    fun load(language: String): Grammar {
        val stream = try {
            assets.open("grammar_$language.json")
        } catch (e: IOException) {
            assets.open("grammar_$DEFAULT_LANGUAGE.json")
        }
        InputStreamReader(stream).use { reader ->
            return gson.fromJson(reader, Grammar::class.java)
        }
    }

    companion object {
        const val DEFAULT_LANGUAGE = "en"
        val LANGUAGES = listOf("en", "ru")
    }
}
