package io.bluetape4k.tokenizer.korean.phrase

import io.bluetape4k.logging.KLogging
import io.bluetape4k.tokenizer.korean.KoreanProcessor
import io.bluetape4k.tokenizer.korean.TestBase
import io.bluetape4k.tokenizer.korean.normalizer.KoreanNormalizer
import io.bluetape4k.tokenizer.korean.tokenizer.KoreanToken
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.KoreanParticle
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Modifier
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Noun
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeLessThan
import org.amshove.kluent.shouldContainSame
import org.junit.jupiter.api.Test
import java.io.Serializable
import kotlin.system.measureTimeMillis

class NounPhraseExtractorTest: TestBase() {

    companion object: KLogging()

    data class SampleTextPair(
        val text: String,
        val phrases: String,
    ): Serializable

    val superLongText = "허니버터칩정규직크리스마스".repeat(50)

    @Test
    fun `collapse KoreanPos sequence`() {
        var actual = NounPhraseExtractor.collapsePos(
            listOf(
                KoreanToken("N", Noun, 0, 1),
                KoreanToken("N", Noun, 1, 1)
            )
        ).joinToString("/")
        var expected = "N(Noun: 0, 1)/N(Noun: 1, 1)"
        actual shouldBeEqualTo expected

        actual = NounPhraseExtractor.collapsePos(
            listOf(
                KoreanToken("X", KoreanParticle, 0, 1),
                KoreanToken("m", Modifier, 1, 1),
                KoreanToken("N", Noun, 2, 1)
            )
        ).joinToString("/")
        expected = "X(KoreanParticle: 0, 1)/mN(Noun: 1, 2)"
        actual shouldBeEqualTo expected

        actual = NounPhraseExtractor.collapsePos(
            listOf(
                KoreanToken("m", Modifier, 0, 1),
                KoreanToken("X", KoreanParticle, 1, 1),
                KoreanToken("N", Noun, 2, 1)
            )
        ).joinToString("/")
        expected = "m(Noun: 0, 1)/X(KoreanParticle: 1, 1)/N(Noun: 2, 1)"
        actual shouldBeEqualTo expected


        actual = NounPhraseExtractor.collapsePos(
            listOf(
                KoreanToken("m", Modifier, 0, 1),
                KoreanToken("N", Noun, 1, 1),
                KoreanToken("X", KoreanParticle, 2, 1)
            )
        ).joinToString("/")
        expected = "mN(Noun: 0, 2)/X(KoreanParticle: 2, 1)"
        actual shouldBeEqualTo expected
    }

    @Test
    fun `should not deduplicate phrases`() {
        val tokenized = KoreanProcessor.tokenizeForNoun("성탄절 쇼핑 성탄절 쇼핑 성탄절 쇼핑 성탄절 쇼핑")
        val phrases = NounPhraseExtractor.extractPhrases(tokenized)

        val expected = phrases.map { it.text }.distinct()
        val actual = phrases.map { it.text }
        actual shouldContainSame expected
    }

    @Test
    fun `should extract long noun-only phrases in reasonable time`() {
        assertExtraction(superLongText, "허니버터칩(Noun: 0, 5), 정규직(Noun: 5, 3), 크리스마스(Noun: 8, 5)")

        val tokens = KoreanProcessor.tokenize(superLongText)

        measureTimeMillis {
            NounPhraseExtractor.extractPhrases(tokens)
        } shouldBeLessThan 10_000
    }

    @Test
    fun `should extract the example set`() {
        fun phraseExtractor(text: String): String {
            val normalized = KoreanNormalizer.normalize(text)
            val tokens = io.bluetape4k.tokenizer.korean.KoreanProcessor.tokenizeForNoun(normalized)
            return NounPhraseExtractor.extractPhrases(tokens).joinToString("/")
        }
        assertExamples("current_phrases.txt", log) { phraseExtractor(it) }
    }

    @Test
    fun `should detect numbers with special chars`() {
        assertExtraction(
            "트위터 25.2% 상승.",
            "트위터(Noun: 0, 3), 25.2%(Noun: 4, 5), 상승(Noun: 10, 2)"
        )

        assertExtraction("짜장면 3400원.", "짜장면(Noun: 0, 3), 3400원(Noun: 4, 5)")

        assertExtraction(
            "떡볶이 3,444,231원 + 400원.",
            "떡볶이(Noun: 0, 3), 3,444,231원(Noun: 4, 10), 400원(Noun: 17, 4)"
        )

        assertExtraction(
            "트위터 $200으로 상승",
            "트위터(Noun: 0, 3), $200(Noun: 4, 4), 상승(Noun: 11, 2)"
        )

        assertExtraction(
            "1,200.34원. 1,200.34엔. 1,200.34옌. 1,200.34위안.",
            "1,200.34원(Noun: 0, 9), 1,200.34엔(Noun: 11, 9), 1,200.34옌(Noun: 22, 9), 1,200.34위안(Noun: 33, 10)"
        )

        assertExtraction(
            "떡 만두국",
            "떡(Noun: 0, 1), 만두국(Noun: 2, 3)"
        )
    }

    private fun assertExtraction(s: String, expected: String) {
        val tokens = KoreanProcessor.tokenizeForNoun(s)
        val actual = NounPhraseExtractor.extractPhrases(tokens).joinToString(", ")

        actual shouldBeEqualTo expected
    }
}
