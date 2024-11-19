package io.bluetape4k.tokenizer.korean.utils

import io.bluetape4k.logging.KLogging
import io.bluetape4k.tokenizer.korean.TestBase
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Noun
import io.bluetape4k.tokenizer.utils.CharArraySet
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.Test

class KoreanDictionaryProviderTest: TestBase() {

    companion object: KLogging()

    @Test
    fun `사전 로드하기`() {
        val nouns: CharArraySet = KoreanDictionaryProvider.koreanDictionary[Noun]!!
        nouns.shouldNotBeEmpty()
    }

    @Test
    fun `단어를 사전에 추가하기`() {
        val nonExistentWord = "없는명사다"

        val nouns = KoreanDictionaryProvider.koreanDictionary[Noun]!!

        nouns.contains(nonExistentWord).shouldBeFalse()
        nouns.contains("각광").shouldBeTrue()

        KoreanDictionaryProvider.addWordsToDictionary(Noun, listOf(nonExistentWord))
        nouns.contains(nonExistentWord).shouldBeTrue()
    }

    @Test
    fun `load frequency`() {
        KoreanDictionaryProvider.koreanEntityFreq.shouldNotBeEmpty()
    }
}
