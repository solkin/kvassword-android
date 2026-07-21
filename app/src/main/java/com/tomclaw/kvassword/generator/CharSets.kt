package com.tomclaw.kvassword.generator

/**
 * Character classes used by the generator, with "exclude look-alikes" variants
 * for passwords that will be read aloud or typed by hand.
 */
object CharSets {

    // 0 is already never produced (digits start at 1); dropping 1 as well when
    // avoiding look-alikes, since 1 collides with l / I.
    const val DIGITS = "123456789"
    const val DIGITS_UNAMBIGUOUS = "23456789"

    const val SYMBOLS = "!@#\$%&*+=?"

    /** Letters that are easy to confuse in generated words. */
    val AMBIGUOUS_LETTERS = charArrayOf('l', 'I', 'O', 'o', 'L', 'i')

    fun digits(excludeSimilar: Boolean): String =
        if (excludeSimilar) DIGITS_UNAMBIGUOUS else DIGITS

    fun hasAmbiguousLetters(text: String): Boolean =
        text.any { c -> AMBIGUOUS_LETTERS.any { it == c } }
}
