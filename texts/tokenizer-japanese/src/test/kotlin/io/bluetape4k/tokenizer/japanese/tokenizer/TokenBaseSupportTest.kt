package io.bluetape4k.tokenizer.japanese.tokenizer

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.tokenizer.japanese.AbstractTokenizerTest
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class TokenBaseSupportTest: AbstractTokenizerTest() {

    companion object: KLogging()

    // "お寿司が食べたい。" 토큰화 결과:
    // お: 接頭詞 (접두사)
    // 寿司: 名詞 (명사)
    // が: 助詞 (조사)
    // 食べ: 動詞 (동사)
    // たい: 助動詞 (조동사)
    // 。: 記号 (기호)
    private val tokens by lazy {
        JapaneseTokenizer.tokenize("お寿司が食べたい。").also { tokens ->
            tokens.forEach { log.debug { "token=${it.surface}: ${it.allFeatures}" } }
        }
    }

    private fun findToken(surface: String) = tokens.first { it.surface == surface }

    @Test
    fun `isNoun - 명사 판별`() {
        findToken("寿司").isNoun().shouldBeTrue()
        findToken("食べ").isNoun().shouldBeFalse()
        findToken("が").isNoun().shouldBeFalse()
    }

    @Test
    fun `isVerb - 동사 판별`() {
        findToken("食べ").isVerb().shouldBeTrue()
        findToken("寿司").isVerb().shouldBeFalse()
        findToken("たい").isVerb().shouldBeFalse()
    }

    @Test
    fun `isNounOrVerb - 명사 또는 동사 판별`() {
        findToken("寿司").isNounOrVerb().shouldBeTrue()
        findToken("食べ").isNounOrVerb().shouldBeTrue()
        findToken("が").isNounOrVerb().shouldBeFalse()
    }

    @Test
    fun `isJosa - 조사 판별`() {
        findToken("が").isJosa().shouldBeTrue()
        findToken("寿司").isJosa().shouldBeFalse()
    }

    @Test
    fun `isConjugate - 조동사 판별`() {
        findToken("たい").isConjugate().shouldBeTrue()
        findToken("食べ").isConjugate().shouldBeFalse()
    }

    @Test
    fun `isPunctuation - 기호 판별`() {
        findToken("。").isPunctuation().shouldBeTrue()
        findToken("寿司").isPunctuation().shouldBeFalse()
    }

    @Test
    fun `isAdjective - 형용사 판별`() {
        // "美しい花" -> 美しい: 形容詞
        val adjTokens = JapaneseTokenizer.tokenize("美しい花")
        adjTokens.first { it.surface == "美しい" }.isAdjective().shouldBeTrue()
        adjTokens.first { it.surface == "花" }.isAdjective().shouldBeFalse()
    }
}
