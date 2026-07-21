package com.tomclaw.kvassword

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.GsonBuilder
import com.tomclaw.kvassword.generator.CUSTOM_PRESET_ID
import com.tomclaw.kvassword.generator.EntropyEstimator
import com.tomclaw.kvassword.generator.GrammarRepository
import com.tomclaw.kvassword.generator.LetterCase
import com.tomclaw.kvassword.generator.Mask
import com.tomclaw.kvassword.generator.MaskParser
import com.tomclaw.kvassword.generator.PasswordGenerator
import com.tomclaw.kvassword.generator.RandomEntropySource
import com.tomclaw.kvassword.generator.StrengthPreset
import com.tomclaw.kvassword.generator.Token
import kotlin.math.roundToInt

/**
 * Visual, block-based editor for a custom password mask. Each block is a card
 * (Word / Digits / Symbols / Text) with its own controls; a live coloured
 * preview updates on every change. Saving stores the mask and makes it active.
 */
class MaskBuilderActivity : AppCompatActivity() {

    private enum class BlockType { WORD, DIGITS, SYMBOLS, TEXT }

    private class BlockSpec(
        var type: BlockType,
        var length: Int = 4,
        var case: LetterCase = LetterCase.CAPITALIZED,
        var count: Int = 2,
        var text: String = "-"
    ) {
        fun toToken(): Token = when (type) {
            BlockType.WORD -> Token.Word(length, length, case)
            BlockType.DIGITS -> Token.Digits(count)
            BlockType.SYMBOLS -> Token.Symbols(count)
            BlockType.TEXT -> Token.Literal(text)
        }
    }

    private val specs = ArrayList<BlockSpec>()
    private val gson = GsonBuilder().create()

    private lateinit var settings: Settings
    private lateinit var generator: PasswordGenerator
    private lateinit var container: android.view.ViewGroup
    private lateinit var emptyView: View
    private lateinit var previewText: TextView
    private val bananalytics by lazy { (application as App).bananalytics }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = Settings(this)
        setContentView(R.layout.activity_mask_builder)
        findViewById<View>(android.R.id.content).applySystemBarsPadding()

        val grammar = GrammarRepository(assets, gson).load(settings.language)
        val source = RandomEntropySource()
        generator = PasswordGenerator(RandomWord(grammar, source), source, EntropyEstimator(grammar))

        container = findViewById(R.id.blocks_container)
        emptyView = findViewById(R.id.blocks_empty)
        previewText = findViewById(R.id.mask_preview_text)

        loadInitialSpecs()

        findViewById<MaterialButton>(R.id.mask_back).setOnClickListener { finish() }
        findViewById<MaterialButton>(R.id.mask_preview_refresh).setOnClickListener { updatePreview() }
        findViewById<MaterialButton>(R.id.add_word).setOnClickListener { addBlock(BlockSpec(BlockType.WORD)) }
        findViewById<MaterialButton>(R.id.add_digits).setOnClickListener { addBlock(BlockSpec(BlockType.DIGITS)) }
        findViewById<MaterialButton>(R.id.add_symbols).setOnClickListener { addBlock(BlockSpec(BlockType.SYMBOLS, count = 1)) }
        findViewById<MaterialButton>(R.id.add_text).setOnClickListener { addBlock(BlockSpec(BlockType.TEXT)) }
        findViewById<MaterialButton>(R.id.mask_save).setOnClickListener { save() }

        renderBlocks()
        updatePreview()
    }

    private fun loadInitialSpecs() {
        val tokens = MaskParser.parse(settings.customMask).tokens.ifEmpty { StrengthPreset.GOOD.mask.tokens }
        for (token in tokens) {
            specs.add(
                when (token) {
                    is Token.Word -> BlockSpec(BlockType.WORD, length = token.minLength.coerceIn(3, 12), case = token.case)
                    is Token.Digits -> BlockSpec(BlockType.DIGITS, count = token.count.coerceIn(1, 6))
                    is Token.Symbols -> BlockSpec(BlockType.SYMBOLS, count = token.count.coerceIn(1, 6))
                    is Token.Literal -> BlockSpec(BlockType.TEXT, text = token.text)
                }
            )
        }
    }

    private fun addBlock(spec: BlockSpec) {
        specs.add(spec)
        renderBlocks()
        updatePreview()
    }

    private fun renderBlocks() {
        for (i in container.childCount - 1 downTo 0) {
            if (container.getChildAt(i).id != R.id.blocks_empty) container.removeViewAt(i)
        }
        emptyView.visibility = if (specs.isEmpty()) View.VISIBLE else View.GONE

        for (index in specs.indices) {
            val spec = specs[index]
            val layout = when (spec.type) {
                BlockType.WORD -> R.layout.block_word
                BlockType.TEXT -> R.layout.block_text
                else -> R.layout.block_count
            }
            val card = layoutInflater.inflate(layout, container, false)
            bindCommon(card, index)
            when (spec.type) {
                BlockType.WORD -> bindWord(card, spec)
                BlockType.DIGITS -> bindCount(card, spec, R.string.mask_block_digits)
                BlockType.SYMBOLS -> bindCount(card, spec, R.string.mask_block_symbols)
                BlockType.TEXT -> bindText(card, spec)
            }
            container.addView(card)
        }
    }

    private fun bindCommon(card: View, index: Int) {
        val up = card.findViewById<ImageButton>(R.id.block_up)
        val down = card.findViewById<ImageButton>(R.id.block_down)
        up.isEnabled = index > 0
        up.alpha = if (index > 0) 1f else 0.3f
        down.isEnabled = index < specs.size - 1
        down.alpha = if (index < specs.size - 1) 1f else 0.3f
        up.setOnClickListener { move(index, index - 1) }
        down.setOnClickListener { move(index, index + 1) }
        card.findViewById<ImageButton>(R.id.block_delete).setOnClickListener {
            specs.removeAt(index)
            renderBlocks()
            updatePreview()
        }
    }

    private fun bindWord(card: View, spec: BlockSpec) {
        val value = card.findViewById<TextView>(R.id.word_len_value)
        val slider = card.findViewById<Slider>(R.id.word_len_slider)
        value.text = spec.length.toString()
        slider.value = spec.length.coerceIn(3, 12).toFloat()
        slider.addOnChangeListener { _, v, fromUser ->
            value.text = v.roundToInt().toString()
            if (fromUser) {
                spec.length = v.roundToInt()
                updatePreview()
            }
        }
        val group = card.findViewById<MaterialButtonToggleGroup>(R.id.word_case_group)
        group.check(
            when (spec.case) {
                LetterCase.LOWER -> R.id.wcase_lower
                LetterCase.UPPER -> R.id.wcase_upper
                else -> R.id.wcase_cap
            }
        )
        group.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            spec.case = when (checkedId) {
                R.id.wcase_lower -> LetterCase.LOWER
                R.id.wcase_upper -> LetterCase.UPPER
                else -> LetterCase.CAPITALIZED
            }
            updatePreview()
        }
    }

    private fun bindCount(card: View, spec: BlockSpec, titleRes: Int) {
        card.findViewById<TextView>(R.id.block_title).setText(titleRes)
        val value = card.findViewById<TextView>(R.id.count_value)
        val slider = card.findViewById<Slider>(R.id.count_slider)
        value.text = spec.count.toString()
        slider.value = spec.count.coerceIn(1, 6).toFloat()
        slider.addOnChangeListener { _, v, fromUser ->
            value.text = v.roundToInt().toString()
            if (fromUser) {
                spec.count = v.roundToInt()
                updatePreview()
            }
        }
    }

    private fun bindText(card: View, spec: BlockSpec) {
        val input = card.findViewById<TextInputEditText>(R.id.text_input)
        input.setText(spec.text)
        input.doAfterTextChanged {
            spec.text = it?.toString().orEmpty()
            updatePreview()
        }
    }

    private fun move(from: Int, to: Int) {
        if (to < 0 || to >= specs.size) return
        val spec = specs.removeAt(from)
        specs.add(to, spec)
        renderBlocks()
        updatePreview()
    }

    private fun currentMask(): Mask = Mask(specs.map { it.toToken() })

    private fun updatePreview() {
        if (specs.isEmpty()) {
            previewText.text = ""
            return
        }
        val result = generator.generate(currentMask(), settings.excludeSimilar)
        previewText.text = result.toSpannable(this)
    }

    private fun save() {
        val mask = currentMask()
        settings.customMask = if (mask.tokens.isEmpty()) "" else mask.toMaskString()
        if (mask.tokens.isNotEmpty()) settings.sitePreset = CUSTOM_PRESET_ID
        bananalytics.trackEvent("Save Mask", "blocks", mask.tokens.size.toString())
        setResult(RESULT_OK)
        finish()
    }
}
