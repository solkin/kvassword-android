package com.tomclaw.kvassword

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

/**
 * Practice screen: the password is hidden and the user retypes it from memory.
 * The matched-prefix count is shown live and turns into a success state on a
 * full match — reinforcing recall right after generation.
 */
class MemorizeActivity : AppCompatActivity() {

    private lateinit var target: String
    private var revealed = false

    private lateinit var targetView: TextView
    private lateinit var input: TextInputEditText
    private lateinit var progress: TextView
    private lateinit var progressIcon: ImageView
    private lateinit var showButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        target = intent.getStringExtra(EXTRA_PASSWORD).orEmpty()
        setContentView(R.layout.activity_memorize)

        targetView = findViewById(R.id.memorize_target)
        input = findViewById(R.id.memorize_input)
        progress = findViewById(R.id.memorize_progress)
        progressIcon = findViewById(R.id.memorize_progress_icon)
        showButton = findViewById(R.id.memorize_show)

        findViewById<MaterialButton>(R.id.memorize_back).setOnClickListener { finish() }
        findViewById<MaterialButton>(R.id.memorize_done).setOnClickListener { finish() }
        showButton.setOnClickListener {
            revealed = !revealed
            renderTarget()
        }

        input.doAfterTextChanged { updateProgress(it?.toString().orEmpty()) }

        renderTarget()
        updateProgress("")
    }

    private fun renderTarget() {
        targetView.text = if (revealed) {
            target
        } else {
            "•".repeat(target.length).toCharArray().joinToString(" ")
        }
        showButton.setText(if (revealed) R.string.memorize_done else R.string.memorize_show)
    }

    private fun updateProgress(typed: String) {
        val matched = commonPrefixLength(typed, target)
        val complete = typed == target && target.isNotEmpty()
        if (complete) {
            progress.text = getString(R.string.memorize_success)
            progress.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.md_success))
            progressIcon.visibility = View.VISIBLE
        } else {
            progress.text = getString(R.string.memorize_progress, matched, target.length)
            progress.setTextColor(getColorFromAttr(com.google.android.material.R.attr.colorOnSurfaceVariant))
            progressIcon.visibility = View.INVISIBLE
        }
    }

    private fun commonPrefixLength(a: String, b: String): Int {
        val n = minOf(a.length, b.length)
        var i = 0
        while (i < n && a[i] == b[i]) i++
        return i
    }

    private fun getColorFromAttr(attr: Int): Int {
        val value = android.util.TypedValue()
        theme.resolveAttribute(attr, value, true)
        return value.data
    }

    companion object {
        const val EXTRA_PASSWORD = "password"
    }
}
