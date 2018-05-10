package com.tomclaw.kvassword

import java.util.Arrays
import java.util.Random

class RandomWord(private val grammar: Grammar) {

    fun nextWord(wordLength: Int): String {
        if (wordLength < 3 || wordLength > 15) {
            throw IllegalArgumentException("Word length error, words must be between 3 and 15 characters long.")
        }
        return generateRandomWord(wordLength)
    }

    private fun generateRandomWord(wordLength: Int): String {
        var randomWord: String
        randomWord = grammar.startBiGram[indexGenerator(grammar.startBiGram.size)]
        var flag = 0
        var count = 0
        var previousWord: String
        while (randomWord.length != wordLength) {
            previousWord = randomWord
            randomWord = addCharacter(
                    grammar.startBiGram,
                    wordLength,
                    randomWord,
                    grammar.lookupBiGram,
                    grammar.nextCharLookup,
                    flag
            )
            if (previousWord == randomWord) {
                count++
            } else {
                flag = 0
            }
            if (count == 5 && randomWord.length > 3) {
                flag = 1
                count++
            } else if (count == 20) {
                randomWord = grammar.startBiGram[indexGenerator(grammar.startBiGram.size)]
                count = 0
            }
        }
        return randomWord
    }

    private fun addCharacter(
            startBiGram: Array<String>,
            desiredLength: Int,
            currentWord: String,
            lookupBiGram: Array<String>,
            nextCharLookup: Array<Array<Array<String>>>,
            startFlag: Int
    ): String {
        var word = currentWord
        var flag = startFlag
        var mainIndex = getLookupIndex(word, lookupBiGram)
        var type = 0
        if (word.length == desiredLength - 1) {
            type = 1
        }
        while (mainIndex < 0 || mainIndex > 263 || nextCharLookup[mainIndex][type].isEmpty()) {
            if (word.length == 2) {
                return startBiGram[indexGenerator(startBiGram.size)]
            }
            if (flag == 1) {
                word = backtrack(word, 2)
                flag = 0
            } else {
                word = backtrack(word, 1)
            }
            mainIndex = getLookupIndex(word, lookupBiGram)
            if (type == 1) {
                type = 0
            }
        }
        return word + getNextCharacter(type, mainIndex, nextCharLookup)
    }

    private fun indexGenerator(arrayLength: Int): Int {
        val theIndex: Int
        val generator = Random()
        theIndex = generator.nextInt(arrayLength)
        return theIndex
    }

    private fun getNextCharacter(
            type: Int,
            mainIndex: Int,
            theCharacterVault: Array<Array<Array<String>>>
    ): String {
        val nextChar: String
        val i = indexGenerator(theCharacterVault[mainIndex][type].size)
        nextChar = theCharacterVault[mainIndex][type][i]
        return nextChar
    }

    private fun backtrack(theWord: String, numberChars: Int): String {
        var theWord = theWord
        theWord = theWord.substring(0, theWord.length - numberChars)
        return theWord
    }

    private fun getLookupIndex(theWord: String, lookupArray: Array<String>): Int {
        val lookupCharacters = theWord.substring(theWord.length - 2)
        return Arrays.asList(*lookupArray).indexOf(lookupCharacters)
    }
}
