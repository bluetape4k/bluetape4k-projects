package io.bluetape4k.tokenizer.japanese.tokenizer

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.tokenizer.japanese.AbstractTokenizerTest
import org.amshove.kluent.shouldBeEmpty
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

    @Test
    fun `빈 문자열 토큰화`() {
        val tokens = JapaneseTokenizer.tokenize("")
        tokens.shouldBeEmpty()
    }

    @Test
    fun `동사 필터링`() {
        val text = "お寿司が食べたい。"
        val tokens = JapaneseTokenizer.tokenize(text)
        val verbs = JapaneseTokenizer.filter(tokens) { it.isVerb() }

        verbs shouldHaveSize 1
        verbs.first().surface shouldBeEqualTo "食べ"
    }

    @Test
    fun `커스텀 predicate로 필터링`() {
        val text = "お寿司が食べたい。"
        val tokens = JapaneseTokenizer.tokenize(text)

        // 기호만 필터링
        val punctuations = JapaneseTokenizer.filter(tokens) { it.isPunctuation() }
        punctuations shouldHaveSize 1
        punctuations.first().surface shouldBeEqualTo "。"
    }
}
