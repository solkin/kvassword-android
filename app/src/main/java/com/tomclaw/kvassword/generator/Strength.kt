package com.tomclaw.kvassword.generator

enum class StrengthLevel { WEAK, FAIR, GOOD, STRONG }

enum class CrackTime { INSTANT, MINUTES, HOURS, DAYS, YEARS, CENTURIES, AGES }

/**
 * Maps entropy in bits to a qualitative rating, a 1..5 meter and a coarse
 * crack-time bucket. Crack time assumes a fast offline attacker (1e10 guesses/s)
 * and the average case of 2^(bits-1) guesses — deliberately conservative.
 */
object Strength {

    const val MAX_BARS = 5

    /**
     * Effective entropy against a generic brute-force attacker who does not know
     * the generator: length × log2(size of the character classes actually used).
     * This is the standard password-meter model and reflects real crack cost far
     * better than assuming the attacker knows the exact scheme.
     */
    fun charsetBits(password: String): Double {
        if (password.isEmpty()) return 0.0
        var space = 0
        if (password.any { it in 'a'..'z' }) space += 26
        if (password.any { it in 'A'..'Z' }) space += 26
        if (password.any { it in 'а'..'я' || it == 'ё' }) space += 33
        if (password.any { it in 'А'..'Я' || it == 'Ё' }) space += 33
        if (password.any { it.isDigit() }) space += 10
        if (password.any { !it.isLetterOrDigit() }) space += 33
        if (space == 0) space = 26
        return password.length * (Math.log(space.toDouble()) / Math.log(2.0))
    }

    fun level(bits: Double): StrengthLevel = when {
        bits < 40 -> StrengthLevel.WEAK
        bits < 60 -> StrengthLevel.FAIR
        bits < 80 -> StrengthLevel.GOOD
        else -> StrengthLevel.STRONG
    }

    fun bars(bits: Double): Int = when {
        bits < 28 -> 1
        bits < 45 -> 2
        bits < 60 -> 3
        bits < 80 -> 4
        else -> 5
    }

    fun crackTime(bits: Double, guessesPerSecond: Double = 1e10): CrackTime {
        val seconds = Math.pow(2.0, bits - 1) / guessesPerSecond
        return when {
            seconds < 60 -> CrackTime.INSTANT
            seconds < HOUR -> CrackTime.MINUTES
            seconds < DAY -> CrackTime.HOURS
            seconds < YEAR -> CrackTime.DAYS
            seconds < YEAR * 100 -> CrackTime.YEARS
            seconds < YEAR * 100_000 -> CrackTime.CENTURIES
            else -> CrackTime.AGES
        }
    }

    private const val HOUR = 3600.0
    private const val DAY = 86_400.0
    private const val YEAR = 31_536_000.0
}
