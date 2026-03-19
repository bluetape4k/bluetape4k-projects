package io.bluetape4k.tokenizer.japanese

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.tokenizer.japanese.tokenizer.isNoun
import io.bluetape4k.tokenizer.model.blockwordRequestOf
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.Test

class JapaneseProcessorTest: AbstractTokenizerTest() {

    companion object: KLogging()

    @Test
    fun `tokenize - 형태소 분석`() {
        val tokens = JapaneseProcessor.tokenize("お寿司が食べたい。")
        tokens.shouldNotBeEmpty()
        tokens.forEach { log.debug { "token=${it.surface}: ${it.allFeatures}" } }
    }

    @Test
    fun `tokenize - 빈 문자열`() {
        val tokens = JapaneseProcessor.tokenize("")
        tokens.shouldBeEmpty()
    }

    @Test
    fun `filterNoun - 명사 필터링`() {
        val tokens = JapaneseProcessor.tokenize("私は、日本語の勉強をしています。")
        val nouns = JapaneseProcessor.filterNoun(tokens).map { it.surface }

        nouns shouldHaveSize 3
        nouns shouldBeEqualTo listOf("私", "日本語", "勉強")
    }

    @Test
    fun `filter - 커스텀 필터링`() {
        val tokens = JapaneseProcessor.tokenize("お寿司が食べたい。")
        val nouns = JapaneseProcessor.filter(tokens) { it.isNoun() }

        nouns shouldHaveSize 1
        nouns.first().surface shouldBeEqualTo "寿司"
    }

    @Test
    fun `findBlockwords - 금칙어 검색`() {
        val blockwords = JapaneseProcessor.findBlockwords("ホモの男性を理解できない")
        blockwords.shouldNotBeEmpty()
        blockwords.map { it.surface } shouldContain "ホモ"
    }

    @Test
    fun `findBlockwords - 금칙어 없음`() {
        val blockwords = JapaneseProcessor.findBlockwords("私は、日本語の勉強をしています。")
        blockwords.shouldBeEmpty()
    }

    @Test
    fun `maskBlockwords - 금칙어 마스킹`() {
        val request = blockwordRequestOf("ホモの男性を理解できない")
        val response = JapaneseProcessor.maskBlockwords(request)

        response.blockwordExists.shouldBeTrue()
        log.debug { "maskedText=${response.maskedText}" }
    }

    @Test
    fun `maskBlockwords - 금칙어 없는 경우`() {
        val request = blockwordRequestOf("私は、日本語の勉強をしています。")
        val response = JapaneseProcessor.maskBlockwords(request)

        response.blockwordExists.shouldBeFalse()
    }

    @Test
    fun `addBlockwords and removeBlockwords - 사전 관리`() {
        // Kuromoji가 단일 명사로 인식하는 단어 사용
        val customWord = "東京"
        val dictionary = io.bluetape4k.tokenizer.japanese.utils.JapaneseDictionaryProvider.blockWordDictionary

        val wasBlockword = dictionary.contains(customWord)
        try {
            // 커스텀 금칙어 추가
            JapaneseProcessor.addBlockwords(listOf(customWord))
            dictionary.contains(customWord).shouldBeTrue()

            val blockwords = JapaneseProcessor.findBlockwords("これは${customWord}です")
            blockwords.shouldNotBeEmpty()
            blockwords.map { it.surface } shouldContain customWord

            // 금칙어 제거
            JapaneseProcessor.removeBlockwords(listOf(customWord))
            dictionary.contains(customWord).shouldBeFalse()
        } finally {
            if (!wasBlockword) {
                JapaneseProcessor.removeBlockwords(listOf(customWord))
            }
        }
    }
}
