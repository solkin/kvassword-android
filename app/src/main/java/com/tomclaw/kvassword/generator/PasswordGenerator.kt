package com.tomclaw.kvassword.generator

import com.tomclaw.kvassword.RandomWord
import java.util.Locale

data class PasswordChunk(val type: ChunkType, val text: String)

data class GeneratedPassword(val chunks: List<PasswordChunk>, val entropyBits: Double) {
    val plain: String get() = buildString { chunks.forEach { append(it.text) } }
}

/**
 * Turns a [Mask] into a concrete password using the word model and an
 * [EntropySource], tracking entropy as it goes. The single entry point for
 * password, word and quick-tile generation.
 */
class PasswordGenerator(
    private val randomWord: RandomWord,
    private val source: EntropySource,
    private val entropy: EntropyEstimator
) {

    fun generate(mask: Mask, excludeSimilar: Boolean = false): GeneratedPassword {
        val chunks = ArrayList<PasswordChunk>()
        var bits = 0.0
        for (token in mask.tokens) {
            when (token) {
                is Token.Word -> {
                    val length = pickLength(token.minLength, token.maxLength)
                    val text = word(length, token.case, excludeSimilar)
                    chunks.add(PasswordChunk(ChunkType.WORD, text))
                    bits += entropy.wordBits(length)
                }
                is Token.Digits -> {
                    val alphabet = CharSets.digits(excludeSimilar)
                    val text = buildString { repeat(token.count) { append(source.pick(alphabet)) } }
                    chunks.add(PasswordChunk(ChunkType.DIGIT, text))
                    bits += entropy.digitBits(token.count, alphabet.length)
                }
                is Token.Symbols -> {
                    val text = buildString { repeat(token.count) { append(source.pick(CharSets.SYMBOLS)) } }
                    chunks.add(PasswordChunk(ChunkType.SYMBOL, text))
                    bits += entropy.symbolBits(token.count, CharSets.SYMBOLS.length)
                }
                is Token.Literal -> chunks.add(PasswordChunk(ChunkType.LITERAL, token.text))
            }
        }
        return GeneratedPassword(chunks, bits)
    }

    private fun pickLength(min: Int, max: Int): Int =
        if (max > min) min + source.nextInt(max - min + 1) else min

    private fun word(length: Int, case: LetterCase, excludeSimilar: Boolean): String {
        var text = applyCase(randomWord.nextWord(length), case)
        if (excludeSimilar) {
            var tries = 0
            while (tries < MAX_WORD_RETRIES && CharSets.hasAmbiguousLetters(text)) {
                text = applyCase(randomWord.nextWord(length), case)
                tries++
            }
        }
        return text
    }

    private fun applyCase(raw: String, case: LetterCase): String {
        val locale = Locale.getDefault()
        return when (case) {
            LetterCase.CAPITALIZED ->
                raw.substring(0, 1).uppercase(locale) + raw.substring(1).lowercase(locale)
            LetterCase.LOWER -> raw.lowercase(locale)
            LetterCase.UPPER -> raw.uppercase(locale)
            LetterCase.AS_IS -> raw
        }
    }

    private companion object {
        const val MAX_WORD_RETRIES = 12
    }
}
