package com.tomclaw.kvassword.generator

/**
 * Named masks. Strength presets replace the old hard-coded Normal/Good/Strong
 * recipes; site presets satisfy common site rules. Everything is expressed as
 * a [Mask] over the one engine.
 */
enum class StrengthPreset(val id: String, val mask: Mask) {
    NORMAL("normal", MaskParser.parse("W{6}d{2}")),
    GOOD("good", MaskParser.parse("W{4}dW{4}s")),
    STRONG("strong", MaskParser.parse("W{3}dW{3}sW{3}"));

    companion object {
        fun byId(id: String?): StrengthPreset =
            entries.firstOrNull { it.id == id } ?: GOOD
    }
}

enum class SitePreset(val id: String, val mask: Mask) {
    DEFAULT("default", StrengthPreset.GOOD.mask),
    NO_SYMBOLS("no_symbols", MaskParser.parse("W{4}dW{4}d")),
    ALPHANUMERIC("alnum", MaskParser.parse("W{5}d{2}W{4}")),
    PASSPHRASE("passphrase", MaskParser.parse("W{4}-W{4}-W{4}")),
    PIN("pin", MaskParser.parse("d{6}"));
}

/** Single word, length 4..8, capitalized — the old "nickname". */
val WORD_PRESET: Mask = Mask(listOf(Token.Word(minLength = 4, maxLength = 8)))

/** Id used for a user-defined mask. */
const val CUSTOM_PRESET_ID = "custom"

/**
 * Resolves a preset id (strength, site or "custom") back to a [Mask].
 * Used by the quick-settings tile, which only stores the last preset id.
 */
fun maskForPreset(id: String, customMask: String): Mask {
    StrengthPreset.entries.firstOrNull { it.id == id }?.let { return it.mask }
    SitePreset.entries.firstOrNull { it.id == id }?.let { return it.mask }
    if (id == CUSTOM_PRESET_ID && customMask.isNotBlank()) return MaskParser.parse(customMask)
    return StrengthPreset.GOOD.mask
}
