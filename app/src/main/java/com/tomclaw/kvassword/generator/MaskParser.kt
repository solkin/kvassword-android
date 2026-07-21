package com.tomclaw.kvassword.generator

/**
 * Parses the mask mini-language into a [Mask].
 *
 * Tokens:
 *  - `W` / `w` / `U` — word, Capitalized / lower / UPPER; optional `{n}` or `{a-b}` length
 *  - `d` — digit(s); optional `{n}` count
 *  - `s` — symbol(s); optional `{n}` count
 *  - any other run of characters — inserted literally (e.g. `-`, `_`, `.`)
 *
 * Example: `W{3}dW{3}s` → Word(3) + digit + Word(3) + symbol.
 */
object MaskParser {

    private const val CONTROL = "WwUds"
    private const val DEFAULT_WORD_LENGTH = 3

    fun parse(input: String): Mask {
        val tokens = ArrayList<Token>()
        var i = 0
        while (i < input.length) {
            when (val c = input[i]) {
                'W', 'w', 'U' -> {
                    val count = readCount(input, i + 1)
                    val case = when (c) {
                        'W' -> LetterCase.CAPITALIZED
                        'w' -> LetterCase.LOWER
                        else -> LetterCase.UPPER
                    }
                    val min = count.min ?: DEFAULT_WORD_LENGTH
                    val max = count.max ?: min
                    tokens.add(Token.Word(min.coerceIn(3, 15), max.coerceIn(3, 15), case))
                    i = count.next
                }
                'd' -> {
                    val count = readCount(input, i + 1)
                    tokens.add(Token.Digits((count.min ?: 1).coerceAtLeast(1)))
                    i = count.next
                }
                's' -> {
                    val count = readCount(input, i + 1)
                    tokens.add(Token.Symbols((count.min ?: 1).coerceAtLeast(1)))
                    i = count.next
                }
                else -> {
                    val start = i
                    while (i < input.length && input[i] !in CONTROL) i++
                    tokens.add(Token.Literal(input.substring(start, i)))
                }
            }
        }
        return Mask(tokens)
    }

    private data class Count(val min: Int?, val max: Int?, val next: Int)

    private fun readCount(s: String, idx: Int): Count {
        if (idx >= s.length || s[idx] != '{') return Count(null, null, idx)
        val close = s.indexOf('}', idx)
        if (close < 0) return Count(null, null, idx)
        val body = s.substring(idx + 1, close)
        val next = close + 1
        val dash = body.indexOf('-')
        return if (dash >= 0) {
            Count(
                body.substring(0, dash).trim().toIntOrNull(),
                body.substring(dash + 1).trim().toIntOrNull(),
                next
            )
        } else {
            val n = body.trim().toIntOrNull()
            Count(n, n, next)
        }
    }
}
