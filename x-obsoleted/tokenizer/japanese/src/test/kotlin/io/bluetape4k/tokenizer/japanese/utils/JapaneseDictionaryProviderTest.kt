package io.bluetape4k.tokenizer.japanese.utils

import io.bluetape4k.logging.KLogging
import io.bluetape4k.tokenizer.japanese.AbstractTokenizerTest
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.Test

class JapaneseDictionaryProviderTest: AbstractTokenizerTest() {

    companion object: KLogging()

    @Test
    fun `금칙어 사전 로드하기`() {
        val blockwords = JapaneseDictionaryProvider.blockWordDictionary
        blockwords.shouldNotBeEmpty()
    }

    @Test
    fun `금칙어 사전에 등록된 단어 검증`() {
        val blockwords = JapaneseDictionaryProvider.blockWordDictionary
        blockwords.contains("한국어").shouldBeFalse()
        blockwords.contains("性器").shouldBeTrue()
    }

    @Test
    fun `금칙어 동적 추가 삭제`() {
        val blockwords = JapaneseDictionaryProvider.blockWordDictionary

        val newWord = "19禁"
        val newWord2 = "29禁"

        // Add blockword to dictionary
        blockwords.contains(newWord).shouldBeFalse()
        blockwords.contains(newWord2).shouldBeFalse()
        JapaneseDictionaryProvider.addBlockwords(listOf(newWord, newWord2))
        blockwords.contains(newWord).shouldBeTrue()
        blockwords.contains(newWord2).shouldBeTrue()

        // Remove blockword from dictionary
        JapaneseDictionaryProvider.removeBlockwords(listOf(newWord, newWord2))

        blockwords.contains(newWord).shouldBeFalse()
        blockwords.contains(newWord2).shouldBeFalse()
    }
}
