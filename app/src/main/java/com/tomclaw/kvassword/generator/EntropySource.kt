package com.tomclaw.kvassword.generator

import java.security.SecureRandom
import java.util.Random

/**
 * Source of randomness for generation. Abstracted so the same mask engine can
 * later be driven by a deterministic KDF stream (stateless "password by phrase")
 * instead of a real RNG.
 */
interface EntropySource {
    fun nextInt(bound: Int): Int
}

class RandomEntropySource(private val random: Random = SecureRandom()) : EntropySource {
    override fun nextInt(bound: Int): Int = random.nextInt(bound)
}

fun EntropySource.pick(text: String): String {
    val i = nextInt(text.length)
    return text.substring(i, i + 1)
}
