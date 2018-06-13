package com.tomclaw.kvassword

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView
import com.google.gson.GsonBuilder
import java.io.IOException
import java.io.InputStreamReader
import java.util.Random

class MainActivity : AppCompatActivity() {

    private lateinit var random: Random
    private lateinit var randomWord: RandomWord

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val button = findViewById<Button>(R.id.button)
        val password = findViewById<TextView>(R.id.password)

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

        button.setOnClickListener {
            generate(password)
        }

        generate(password)
    }

    private fun generate(text: TextView) {
        val pass = randomWord.nextWord(3).toFirstUpper() +
                randomDigit() + randomWord.nextWord(3).toFirstUpper() + randomSymbol() +
                randomWord.nextWord(3).toUpperCase()
        text.text = pass
    }

    private fun randomDigit(): String = random.nextInt(10).toString()

    private fun randomSymbol(): String {
        val symbols = "!@#\$%&*-+=:?"
        val i = random.nextInt(symbols.length)
        return symbols.substring(i, i + 1)
    }
}

private fun String.toFirstUpper(): String {
    return substring(0, 1).toUpperCase() + substring(1).toLowerCase()
}
