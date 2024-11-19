package io.bluetape4k.tokenizer.utils

import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class DictionaryProviderTest {

    companion object: KLogging() {
        private const val BASE_PATH = "dictionary"
        private const val NOUN_PATH = "$BASE_PATH/noun/nouns.txt"
        private const val FOREIGN_PATH = "$BASE_PATH/noun/foreign.txt"
        private const val FREQ_PATH = "$BASE_PATH/freq/entity-freq.txt.gz"
        private const val BLOCK_PATH = "$BASE_PATH/block/block.txt"
    }

    @Test
    fun `명사 사전 로드하기 as CharArraySet`() = runTest {
        val dictionary = DictionaryProvider.readWords(NOUN_PATH, FOREIGN_PATH)

        dictionary.contains("각광").shouldBeTrue()
        dictionary.contains("없는명사다").shouldBeFalse()
    }

    @Test
    fun `명사 사전 로드하기 as Set`() = runTest {
        val dictionary = DictionaryProvider.readWordsAsSet(NOUN_PATH, FOREIGN_PATH)

        dictionary.contains("각광").shouldBeTrue()
        dictionary.contains("없는명사다").shouldBeFalse()
    }

    @Test
    fun `없는 파일 로드하면 예외가 발생한다`() = runTest {
        assertFailsWith<IllegalStateException> {
            DictionaryProvider.readWords("$BASE_PATH/noun/non-exists.txt")
        }
    }

    @Test
    fun `압축된 파일 로드하기`() {
        val dictionary = DictionaryProvider.readWordFreqs(FREQ_PATH)
        dictionary.shouldNotBeEmpty()
    }

    @Test
    fun `금칙어 사전 로드하기`() = runTest {
        val dictionary = DictionaryProvider.readWords(BLOCK_PATH)

        dictionary.contains("씨불").shouldBeTrue()
        dictionary.contains("씨발").shouldBeTrue()
        dictionary.contains("히로뽕").shouldBeTrue()

        dictionary.contains("하늘").shouldBeFalse()
    }
}
