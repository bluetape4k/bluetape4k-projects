package io.bluetape4k.ahocorasick.trie

import io.bluetape4k.collections.eclipse.fastListOf
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.Test

class KeywordProcessorTest {

    companion object: KLogging()

    val trie = Trie.builder()
        .addKeywords("NYC")
        .addKeywords("APPL")
        .addKeywords("java_2e", "java programming")
        .addKeywords("PM", "product manager")
        .build()

    val text = "I am a PM for a java_2e platform working from APPL, NYC"

    @Test
    fun `extract keywords`() {
        val emits = trie.parseText(text)
        log.debug { "emits=$emits" }

        emits shouldBeEqualTo fastListOf(
            Emit(7, 8, "PM"),
            Emit(16, 22, "java_2e"),
            Emit(46, 49, "APPL"),
            Emit(52, 54, "NYC")
        )
    }

    @Test
    fun `tokenize keywords`() {
        val tokens = trie.tokenize(text)
        log.debug { "tokens=$tokens" }

        tokens shouldHaveSize 8
    }

    @Test
    fun `replace keywords`() {
        val map = mapOf(
            "APPL" to "Apple",
            "NYC" to "New york",
            "java_2e" to "java programming",
            "PM" to "product manager"
        )

        val replaced = trie.replace(text, map)
        replaced shouldBeEqualTo "I am a product manager for a java programming platform working from Apple, New york"
    }
}
