package com.tomclaw.kvassword

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView
import com.google.gson.GsonBuilder
import java.io.IOException
import java.io.InputStreamReader
import java.util.Random

class MainActivity : AppCompatActivity() {

    private lateinit var random: Random
    private lateinit var randomWord: RandomWord

    private var button: Button? = null
    private var password: TextView? = null
    private var strength: RadioGroup? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        button = findViewById(R.id.button)
        password = findViewById(R.id.password)
        strength = findViewById(R.id.pass_strength)

        random = Random(System.currentTimeMillis())

        val reader = InputStreamReader(assets.open("grammar.json"))
        val grammar: Grammar
        try {
            val gson = GsonBuilder().create()
            grammar = gson.fromJson(reader, Grammar::class.java)
        } finally {
            try {
                reader.close()
            } catch (ignored: IOException) {
            }
        }
        randomWord = RandomWord(grammar)

        button?.setOnClickListener {
            generate()
        }

        generate()
    }

    private fun generate() {
        val pass = when (strength?.checkedRadioButtonId) {
            R.id.pass_good -> listOf(
                    randomWord.nextWord(4).toFirstUpper(),
                    randomDigit(),
                    randomDigit()
            )
            R.id.pass_strong -> listOf(
                    randomWord.nextWord(3).toFirstUpper(),
                    randomDigit(),
                    randomWord.nextWord(3).toFirstUpper(),
                    randomSymbol()
            )
            R.id.pass_very_strong -> listOf(
                    randomWord.nextWord(3).toFirstUpper(),
                    randomDigit(),
                    randomWord.nextWord(3).toFirstUpper(),
                    randomSymbol(),
                    randomDigit(),
                    randomWord.nextWord(3).toUpperCase()
            )
            else -> throw IllegalStateException("Invalid selection")
        }
        password?.text = pass.concatItems()
    }

    private fun randomDigit(): String = random.nextInt(10).toString()

    private fun randomSymbol(): String {
        val symbols = "!@#\$%&*-+=:?"
        val i = random.nextInt(symbols.length)
        return symbols.substring(i, i + 1)
    }
}

private fun List<String>.concatItems(): kotlin.String {
    val string = StringBuilder()
    forEach { string.append(it) }
    return string.toString()
}

private fun String.toFirstUpper(): String {
    return substring(0, 1).toUpperCase() + substring(1).toLowerCase()
}
