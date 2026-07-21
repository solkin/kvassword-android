package com.tomclaw.kvassword.generator

/**
 * An ordered recipe for a password: which blocks, in which order, of which size.
 * Strength presets, the word generator and any user mask are all just [Mask]s.
 */
data class Mask(val tokens: List<Token>) {

    fun toMaskString(): String = buildString {
        for (token in tokens) {
            when (token) {
                is Token.Word -> {
                    append(
                        when (token.case) {
                            LetterCase.CAPITALIZED -> 'W'
                            LetterCase.LOWER -> 'w'
                            LetterCase.UPPER -> 'U'
                            LetterCase.AS_IS -> 'W'
                        }
                    )
                    append(rangeString(token.minLength, token.maxLength))
                }
                is Token.Digits -> append("d").append(countString(token.count))
                is Token.Symbols -> append("s").append(countString(token.count))
                is Token.Literal -> append(token.text)
            }
        }
    }

    private fun rangeString(min: Int, max: Int): String =
        if (max > min) "{$min-$max}" else "{$min}"

    private fun countString(count: Int): String =
        if (count > 1) "{$count}" else ""

    companion object {
        fun parse(input: String): Mask = MaskParser.parse(input)
    }
}
