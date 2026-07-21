package com.tomclaw.kvassword.generator

import com.tomclaw.kvassword.Grammar

/**
 * Honest-ish entropy estimate for generated passwords.
 *
 * Word entropy is derived from the grammar itself: the average branching factor
 * of the bigram model (mean log2 of the number of next-character options) is the
 * per-step entropy the generator actually produces. That is far more truthful
 * than a naive length×alphabet estimate, which wildly overstates pseudo-words.
 */
class EntropyEstimator(grammar: Grammar) {

    private val startBits: Double = log2(grammar.startBiGram.size.coerceAtLeast(1))
    private val avgStepBits: Double

    init {
        var sum = 0.0
        var count = 0
        for (entry in grammar.nextCharLookup) {
            val middle = entry.getOrNull(0)
            if (middle != null && middle.isNotEmpty()) {
                sum += log2(middle.size)
                count++
            }
        }
        // Slight discount: backtracking on dead ends removes some choice.
        avgStepBits = (if (count > 0) sum / count else 2.0) * 0.9
    }

    fun wordBits(length: Int): Double =
        if (length <= 2) startBits else startBits + (length - 2) * avgStepBits

    fun digitBits(count: Int, alphabetSize: Int): Double = count * log2(alphabetSize)

    fun symbolBits(count: Int, alphabetSize: Int): Double = count * log2(alphabetSize)

    companion object {
        fun log2(n: Int): Double = Math.log(n.toDouble()) / LN_2
        private val LN_2 = Math.log(2.0)
    }
}
