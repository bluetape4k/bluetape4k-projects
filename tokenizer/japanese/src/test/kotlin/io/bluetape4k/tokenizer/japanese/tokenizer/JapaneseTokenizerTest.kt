package io.bluetape4k.tokenizer.japanese.tokenizer

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.tokenizer.japanese.AbstractTokenizerTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.Test

class JapaneseTokenizerTest: AbstractTokenizerTest() {

    companion object: KLogging()

    @Test
    fun `tokenize japanese text and filter noun`() {
        val text = "私は、日本語の勉強をしています。"
        val tokens = JapaneseTokenizer.tokenize(text)
        tokens.forEach {
            log.debug { "token=${it.surface}: ${it.allFeatures}, ${it.position}" }
        }

        val nouns = JapaneseTokenizer.filterNoun(tokens).map { it.surface }
        nouns shouldHaveSize 3
        nouns shouldBeEqualTo listOf("私", "日本語", "勉強")
    }
}
