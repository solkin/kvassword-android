package com.tomclaw.kvassword.generator

/**
 * Semantic type of a password chunk. Drives the colour legend: each type gets
 * one consistent colour so a password reads as word / digit / symbol at a glance.
 */
enum class ChunkType { WORD, DIGIT, SYMBOL, LITERAL }

enum class LetterCase { CAPITALIZED, LOWER, UPPER, AS_IS }

/**
 * A single element of a password mask. A [Mask] is an ordered list of tokens;
 * the same list expresses the strength presets, the word generator and any
 * user-defined mask.
 */
sealed class Token {

    data class Word(
        val minLength: Int,
        val maxLength: Int = minLength,
        val case: LetterCase = LetterCase.CAPITALIZED
    ) : Token()

    data class Digits(val count: Int = 1) : Token()

    data class Symbols(val count: Int = 1) : Token()

    data class Literal(val text: String) : Token()
}
