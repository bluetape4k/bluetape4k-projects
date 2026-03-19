package io.bluetape4k.tokenizer.korean.stemmer

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.tokenizer.korean.TestBase
import io.bluetape4k.tokenizer.korean.tokenizer.KoreanToken
import io.bluetape4k.tokenizer.korean.tokenizer.KoreanTokenizer
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Adjective
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Eomi
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Exclamation
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.PreEomi
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Verb
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContainSame
import org.junit.jupiter.api.Test

class KoreanStemmerTest: TestBase() {

    companion object: KLogging()

    @Test
    fun `should stem to Adjective`() {
        val tokens = listOf(
            KoreanToken("아", Exclamation, 0, 1),
            KoreanToken("이럴", Adjective, 1, 2),
            KoreanToken("수", PreEomi, 3, 1),
            KoreanToken("가", Eomi, 4, 1)
        )

        val expected = listOf(
            KoreanToken("아", Exclamation, 0, 1),
            KoreanToken("이럴수가", Adjective, 1, 4, stem = "이렇다")
        )
        val actual = KoreanStemmer.stem(tokens)
        log.debug { "stemmed=$actual" }
        actual shouldContainSame expected
    }

    @Test
    fun `가느다란 원형`() {
        val tokens = KoreanTokenizer.tokenizeTopN("가느다란").flatMap { it.first() }
        val actual = KoreanStemmer.stem(tokens)

        actual shouldBeEqualTo listOf(
            KoreanToken("가느다란", Verb, 0, 4, stem = "갈다")
        )
    }
}
