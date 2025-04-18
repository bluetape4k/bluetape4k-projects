package io.bluetape4k.ahocorasick.trie

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import kotlin.random.Random

class TrieTest {

    companion object: KLogging() {
        val ALPHABET = listOf("abc", "bcd", "cde")
        val PRONOUNS = listOf("hers", "his", "she", "he")
        val FOOD = listOf("veal", "cauliflower", "broccoli", "tomatoes")
        val GREEK_LETTERS = listOf("Alpha", "Beta", "Gamma")
        val UNICODE = listOf("turning", "once", "again", "börkü")
    }

    @Test
    fun `keyword and text are same`() {
        val trie = Trie.builder()
            .addKeyword(ALPHABET[0])
            .build()

        val emits = trie.parseText(ALPHABET[0])
        checkEmit(emits.firstOrNull(), 0, 2, ALPHABET[0])
    }

    @Test
    fun `keyword and text are the same first match`() {
        val trie = Trie.builder()
            .addKeyword(ALPHABET[0])
            .build()

        val firstMatch = trie.firstMatch(ALPHABET[0])
        checkEmit(firstMatch, 0, 2, ALPHABET[0])
    }

    @Test
    fun `text is longer than keyword`() {
        val trie = Trie.builder()
            .addKeyword(ALPHABET[0])
            .build()

        val emits = trie.parseText(" " + ALPHABET[0])
        checkEmit(emits.firstOrNull(), 1, 3, ALPHABET[0])
    }

    @Test
    fun `text is longer than keyword first match`() {
        val trie = Trie.builder()
            .addKeyword(ALPHABET[0])
            .build()

        val emits = trie.firstMatch(" " + ALPHABET[0])
        checkEmit(emits, 1, 3, ALPHABET[0])
    }

    @Test
    fun `various keywords on match`() {
        val trie = Trie.builder()
            .addKeywords(ALPHABET)
            .build()

        val emits = trie.parseText("bcd")
        checkEmit(emits.firstOrNull(), 0, 2, "bcd")
    }

    @Test
    fun `various keywords first match`() {
        val trie = Trie.builder()
            .addKeywords(ALPHABET)
            .build()

        val emits = trie.firstMatch("bcd")
        checkEmit(emits, 0, 2, "bcd")
    }

    @Test
    fun `ushers test and stop on hit`() {
        val trie = Trie.builder()
            .addKeywords(PRONOUNS)
            .stopOnHit()
            .build()

        val emits = trie.parseText("ushers")
        emits.size shouldBeEqualTo 1
        checkEmit(emits.firstOrNull(), 2, 3, "he")
    }

    @Test
    fun `ushers test stop on hit skip first one`() {
        val trie = Trie.builder()
            .addKeywords(PRONOUNS)
            .stopOnHit()
            .build()

        val testEmitHandler = object: AbstractStatefulEmitHandler() {
            var first = true
            override fun emit(emit: Emit): Boolean {
                if (first) {
                    first = false
                    return false
                }
                addEmit(emit)
                return true
            }
        }
        trie.parseText("ushers", testEmitHandler)
        val emits = testEmitHandler.emits
        emits.size shouldBeEqualTo 1
        checkEmit(emits.firstOrNull(), 1, 3, "she")
    }

    @Test
    fun `ushers test`() {
        val trie = Trie.builder()
            .addKeywords(PRONOUNS)
            .build()

        val emits = trie.parseText("ushers")
        emits.size shouldBeEqualTo 3
        checkEmit(emits[0], 2, 3, "he")
        checkEmit(emits[1], 1, 3, "she")
        checkEmit(emits[2], 2, 5, "hers")
    }

    @Test
    fun `ushers test with capital keywords`() {
        val trie = Trie.builder()
            .ignoreCase()
            .addKeywords(PRONOUNS.map { it.lowercase() })
            .build()

        val emits = trie.parseText("ushers")
        emits.size shouldBeEqualTo 3
        checkEmit(emits[0], 2, 3, "he")
        checkEmit(emits[1], 1, 3, "she")
        checkEmit(emits[2], 2, 5, "hers")
    }

    @Test
    fun `ushers test first match`() {
        val trie = Trie.builder()
            .addKeywords(PRONOUNS)
            .build()

        val emits = trie.firstMatch("ushers")
        checkEmit(emits, 2, 3, "he")
    }

    @Test
    fun `ushers test by callback`() {
        val trie = Trie.builder()
            .addKeywords(PRONOUNS)
            .build()

        val emits = mutableListOf<Emit>()
        val emitHandler = EmitHandler { emit -> emits.add(emit) }
        trie.runParseText("ushers", emitHandler)

        emits.size shouldBeEqualTo 3
        checkEmit(emits[0], 2, 3, "he")
        checkEmit(emits[1], 1, 3, "she")
        checkEmit(emits[2], 2, 5, "hers")
    }

    @Test
    fun `mis leading test`() {
        val trie = Trie.builder()
            .addKeyword("hers")
            .build()

        val emits = trie.parseText("h he her hers")
        checkEmit(emits.firstOrNull(), 9, 12, "hers")
    }

    @Test
    fun `food recipes`() {
        val trie = Trie.builder()
            .addKeywords(FOOD)
            .build()

        val emits = trie.parseText("2 cauliflowers, 3 tomatoes, 4 slices of veal, 100g broccoli")
        emits.size shouldBeEqualTo 4
        checkEmit(emits[0], 2, 12, "cauliflower")
        checkEmit(emits[1], 18, 25, "tomatoes")
        checkEmit(emits[2], 40, 43, "veal")
        checkEmit(emits[3], 51, 58, "broccoli")
    }

    @Test
    fun `food recipes first match`() {
        val trie = Trie.builder()
            .addKeywords(FOOD)
            .build()

        val firstMatch = trie.firstMatch("2 cauliflowers, 3 tomatoes, 4 slices of veal, 100g broccoli")
        checkEmit(firstMatch, 2, 12, "cauliflower")
    }

    @Test
    fun `long and short overlapping match`() {
        val trie = Trie.builder()
            .addKeyword("he")
            .addKeyword("hehehehe")
            .build()

        val emits = trie.parseText("hehehehehe")

        emits.size shouldBeEqualTo 7
        checkEmit(emits[0], 0, 1, "he")
        checkEmit(emits[1], 2, 3, "he")
        checkEmit(emits[2], 4, 5, "he")
        checkEmit(emits[3], 6, 7, "he")
        checkEmit(emits[4], 0, 7, "hehehehe")
        checkEmit(emits[5], 8, 9, "he")
        checkEmit(emits[6], 2, 9, "hehehehe")
    }

    @Test
    fun `non overlapping`() {
        val trie = Trie.builder()
            .ignoreOverlaps()
            .addKeyword("ab")
            .addKeyword("cba")
            .addKeyword("ababc")
            .build()

        val emits = trie.parseText("ababcbab")

        emits.size shouldBeEqualTo 2
        checkEmit(emits[0], 0, 4, "ababc")
        checkEmit(emits[1], 6, 7, "ab")
    }

    @Test
    fun `non overlapping first match`() {
        val trie = Trie.builder()
            .ignoreOverlaps()
            .addKeyword("ab")
            .addKeyword("cba")
            .addKeyword("ababc")
            .build()

        val firstMatch = trie.firstMatch("ababcbab")
        checkEmit(firstMatch, 0, 4, "ababc")
    }

    @Test
    fun `contains match`() {
        val trie = Trie.builder()
            .addKeyword("ab")
            .addKeyword("cba")
            .addKeyword("ababc")
            .build()

        trie.containsMatch("ababcbab").shouldBeTrue()
    }

    @Test
    fun `start of churchill speech`() {
        val trie = Trie.builder()
            .ignoreOverlaps()
            .addKeywords("T", "u", "ur", "r", "urn", "ni", "i", "in", "n", "urning")
            .build()

        val emits = trie.parseText("Turning")
        emits.size shouldBeEqualTo 2
        checkEmit(emits[0], 0, 0, "T")
        checkEmit(emits[1], 1, 6, "urning")
    }

    @Test
    fun `partial match`() {
        val trie = Trie.builder()
            .onlyWholeWords()
            .addKeyword("sugar")
            .build()

        val emits = trie.parseText("sugarcane sugarcane sugar canesugar") // left, middle, right test
        emits.size shouldBeEqualTo 1
        checkEmit(emits[0], 20, 24, "sugar")
    }

    @Test
    fun `partial match first match`() {
        val trie = Trie.builder()
            .onlyWholeWords()
            .addKeyword("sugar")
            .build()

        val firstMatch = trie.firstMatch("sugarcane sugarcane sugar canesugar") // left, middle, right test
        checkEmit(firstMatch, 20, 24, "sugar")
    }

    @Test
    fun `tokenize full sentence`() {
        val trie = Trie.builder()
            .addKeywords(GREEK_LETTERS)
            .build()

        val tokens = trie.tokenize("Hear: Alpha team first, Beta from the rear, Gamma in reserve")

        tokens.size shouldBeEqualTo 7
        tokens[0].fragment shouldBeEqualTo "Hear: "
        tokens[1].fragment shouldBeEqualTo "Alpha"
        tokens[2].fragment shouldBeEqualTo " team first, "
        tokens[3].fragment shouldBeEqualTo "Beta"
        tokens[4].fragment shouldBeEqualTo " from the rear, "
        tokens[5].fragment shouldBeEqualTo "Gamma"
        tokens[6].fragment shouldBeEqualTo " in reserve"
    }

    @Test
    fun `string index out of bounds exception`() {
        val trie = Trie.builder()
            .ignoreCase()
            .onlyWholeWords()
            .addKeywords(UNICODE)
            .build()

        val emits = trie.parseText("TurninG OnCe AgAiN BÖRKÜ")

        emits.size shouldBeEqualTo 4

        checkEmit(emits[0], 0, 6, "turning")
        checkEmit(emits[1], 8, 11, "once")
        checkEmit(emits[2], 13, 17, "again")
        checkEmit(emits[3], 19, 23, "börkü")
    }

    @Test
    fun `test ignorecase`() {
        val trie = Trie.builder()
            .ignoreCase()
            .onlyWholeWords()
            .addKeywords(UNICODE)
            .build()

        val emits = trie.parseText("TurninG OnCe AgAiN BÖRKÜ")

        emits.size shouldBeEqualTo 4

        checkEmit(emits[0], 0, 6, "turning")
        checkEmit(emits[1], 8, 11, "once")
        checkEmit(emits[2], 13, 17, "again")
        checkEmit(emits[3], 19, 23, "börkü")
    }

    @Test
    fun `test ignorecase first match`() {
        val trie = Trie.builder()
            .ignoreCase()
            .onlyWholeWords()
            .addKeywords(UNICODE)
            .build()

        val firstMatch = trie.firstMatch("TurninG OnCe AgAiN BÖRKÜ")

        checkEmit(firstMatch, 0, 6, "turning")
    }

    @Test
    fun `tokenize Tokens in sequence`() {
        val trie = Trie.builder()
            .addKeywords(GREEK_LETTERS)
            .build()

        val tokens = trie.tokenize("Alpha Beta Gamma")
        log.debug { "tokens=$tokens" }
        tokens.size shouldBeEqualTo 5   // 2 space
    }

    @Test
    fun `zero length`() {
        val trie = Trie.builder()
            .ignoreCase()
            .ignoreOverlaps()
            .onlyWholeWords()
            .addKeyword("")
            .build()

        val sentence = """
                  |Try a natural lip and subtle bronzer to keep all the focus
                  |on those big bright eyes with NARS Eyeshadow Duo in Rated R And the winner is...
                  |Boots No7 Advanced Renewal Anti-ageing Glycolic Peel Kit (${'$'}25 amazon.com) won
                  |most-appealing peel.
                  """.trimMargin()

        val tokens = trie.tokenize(sentence)

        tokens.size shouldBeEqualTo 1
        tokens[0].fragment shouldBeEqualTo sentence
    }

    @Test
    fun `parse unicode text 1`() {
        val target = "LİKE THIS"  // The second character ('İ') is Unicode, which was read by AC as a 2-byte char
        target.substring(5, 9) shouldBeEqualTo "THIS"

        val trie = Trie.builder()
            .ignoreCase()
            .onlyWholeWords()
            .addKeyword("this")
            .build()

        val emits = trie.parseText(target)
        emits.size shouldBeEqualTo 1
        checkEmit(emits[0], 5, 8, "this")
    }

    @Test
    fun `parse unicode text 2`() {
        val target = "LİKE THIS"  // The second character ('İ') is Unicode, which was read by AC as a 2-byte char
        target.substring(5, 9) shouldBeEqualTo "THIS"

        val trie = Trie.builder()
            .ignoreCase()
            .onlyWholeWords()
            .addKeyword("this")
            .build()

        val firstMatch = trie.firstMatch(target)
        checkEmit(firstMatch, 5, 8, "this")
    }

    @Test
    fun `partial match whitespace`() {
        val trie = Trie.builder()
            .onlyWholeWordsWhiteSpaceSeparated()
            .addKeyword("#sugar-123")
            .build()

        val emits = trie.parseText("#sugar-123 #sugar-1234")
        emits.size shouldBeEqualTo 1
        checkEmit(emits.firstOrNull(), 0, 9, "#sugar-123")
    }

    @Test
    fun `large string`() {
        val interval = 100
        val textSize = 1_000_000
        val keyword = FOOD[2]
        val text = randomNumbers(textSize)

        injectKeyword(text, keyword, interval)

        val trie = Trie.builder()
            .onlyWholeWords()
            .addKeyword(keyword)
            .build()

        val emits = trie.parseText(text)
        emits.size shouldBeEqualTo textSize / interval
    }

    private fun randomInt(min: Int, max: Int): Int = Random.nextInt(min, max)

    private fun randomNumbers(count: Int): StringBuilder {
        if (count <= 0) {
            return StringBuilder()
        }

        return StringBuilder(count).apply {
            repeat(count) {
                append(randomInt(0, 10))
            }
        }
    }

    private fun injectKeyword(source: StringBuilder, keyword: String, interval: Int) {
        val length = source.length
        for (i in 0 until length step interval) {
            source.replace(i, i + keyword.length, keyword)
        }
    }

    private fun checkEmit(emit: Emit?, expectedStart: Int, expectedEnd: Int, expectedKeyword: String) {
        log.trace { "start=$expectedStart, end=$expectedEnd, keyword=$expectedKeyword, emit=$emit" }
        emit.shouldNotBeNull()
        emit.start shouldBeEqualTo expectedStart
        emit.end shouldBeEqualTo expectedEnd
        emit.keyword shouldBeEqualTo expectedKeyword
    }
}
