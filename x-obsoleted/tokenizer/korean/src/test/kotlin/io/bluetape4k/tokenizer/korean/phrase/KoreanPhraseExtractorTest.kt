package io.bluetape4k.tokenizer.korean.phrase

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.tokenizer.korean.KoreanProcessor.tokenize
import io.bluetape4k.tokenizer.korean.TestBase
import io.bluetape4k.tokenizer.korean.normalizer.KoreanNormalizer
import io.bluetape4k.tokenizer.korean.phrase.KoreanPhraseExtractor.collapsePos
import io.bluetape4k.tokenizer.korean.phrase.KoreanPhraseExtractor.extractPhrases
import io.bluetape4k.tokenizer.korean.tokenizer.KoreanToken
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.KoreanParticle
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Modifier
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Noun
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeLessThan
import org.junit.jupiter.api.Test
import java.io.Serializable
import kotlin.system.measureTimeMillis

class KoreanPhraseExtractorTest: TestBase() {

    companion object: KLogging()

    data class SampleTextPair(
        val text: String,
        val phrases: String,
    ): Serializable

    private val spamText = "레알 시발 저거 카지노 포르노 야동 보다가 개빡쳤음"
    private val superLongText = "허니버터칩정규직크리스마스".repeat(50)

    @Test
    fun `collapse KoreanPos sequence`() {
        var actual = collapsePos(
            listOf(
                KoreanToken("N", Noun, 0, 1),
                KoreanToken("N", Noun, 1, 1)
            )
        ).joinToString("/")
        var expected = "N(Noun: 0, 1)/N(Noun: 1, 1)"
        actual shouldBeEqualTo expected

        actual = collapsePos(
            listOf(
                KoreanToken("X", KoreanParticle, 0, 1),
                KoreanToken("m", Modifier, 1, 1),
                KoreanToken("N", Noun, 2, 1)
            )
        ).joinToString("/")
        expected = "X(KoreanParticle: 0, 1)/mN(Noun: 1, 2)"
        actual shouldBeEqualTo expected

        actual = collapsePos(
            listOf(
                KoreanToken("m", Modifier, 0, 1),
                KoreanToken("X", KoreanParticle, 1, 1),
                KoreanToken("N", Noun, 2, 1)
            )
        ).joinToString("/")
        expected = "m(Noun: 0, 1)/X(KoreanParticle: 1, 1)/N(Noun: 2, 1)"
        actual shouldBeEqualTo expected


        actual = collapsePos(
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
        val phrases = extractPhrases(tokenize("성탄절 쇼핑 성탄절 쇼핑 성탄절 쇼핑 성탄절 쇼핑"), filterSpam = false)

        val expected = phrases.map { it.text }.distinct()
        val actual = phrases.map { it.text }
        actual shouldBeEqualTo expected
    }

    @Test
    fun `should extract long noun-only phrases in reasonable time`() {
        assertExtraction(superLongText, "허니버터칩(Noun: 0, 5), 정규직(Noun: 5, 3), 크리스마스(Noun: 8, 5)")

        val tokens = tokenize(superLongText)

        measureTimeMillis { extractPhrases(tokens) } shouldBeLessThan 10_000
    }

    @Test
    fun `should extract the example set`() {
        fun phraseExtractor(text: String): String {
            val normalized = KoreanNormalizer.normalize(text)
            val tokens = tokenize(normalized)
            return extractPhrases(tokens).joinToString("/")
        }
        assertExamples("current_phrases.txt", log) { phraseExtractor(it) }
    }

    @Test
    fun `문단 구분 예제 - 단문`() {
        val text = """"@user: @user 내가 얼굴이 아가쟈나..ㅎ" 동네사람들...!"""

        val normalized = KoreanNormalizer.normalize(text)
        val tokens = tokenize(normalized)
        log.debug { "tokens: ${tokens.joinToString()}" }

        val phrases = extractPhrases(tokens).joinToString("/")

        val expected = "얼굴(Noun: 17, 2)/아가(Noun: 21, 2)/동네사람들(Noun: 30, 5)/동네(Noun: 30, 2)/사람들(Noun: 32, 3)"
        phrases shouldBeEqualTo expected
    }

    @Test
    fun `should filter out spam and profane words`() {
        extractPhrases(tokenize(spamText), filterSpam = false).size shouldBeGreaterThan 5

        extractPhrases(
            tokenize(spamText),
            filterSpam = true
        ).joinToString(", ") shouldBeEqualTo "레알(Noun: 0, 2), 저거(Noun: 6, 2)"
    }


    @Test
    fun `should detect numbers with special chars`() {
        assertExtraction(
            "트위터 25.2% 상승.",
            "트위터(Noun: 0, 3), 트위터 25.2%(Noun: 0, 9), 트위터 25.2% 상승(Noun: 0, 12), 25.2%(Noun: 4, 5), 상승(Noun: 10, 2)"
        )

        assertExtraction("짜장면 3400원.", "짜장면(Noun: 0, 3), 짜장면 3400원(Noun: 0, 9), 3400원(Noun: 4, 5)")

        assertExtraction(
            "떡볶이 3,444,231원 + 400원.",
            "떡볶이(Noun: 0, 3), 떡볶이 3,444,231원(Noun: 0, 14), 400원(Noun: 17, 4), 3,444,231원(Noun: 4, 10)"
        )

        assertExtraction(
            "트위터 $200으로 상승",
            "트위터(Noun: 0, 3), 트위터 $200(Noun: 0, 8), 상승(Noun: 11, 2), $200(Noun: 4, 4)"
        )

        assertExtraction(
            "1,200.34원. 1,200.34엔. 1,200.34옌. 1,200.34위안.",
            "1,200.34원(Noun: 0, 9), 1,200.34엔(Noun: 11, 9), 1,200.34옌(Noun: 22, 9), 1,200.34위안(Noun: 33, 10)"
        )

        assertExtraction(
            "200달러 3위 3000유로",
            "200달러(Noun: 0, 5), 200달러 3위(Noun: 0, 8), 200달러 3위 3000유로(Noun: 0, 15), 3000유로(Noun: 9, 6)"
        )
    }

    private fun assertExtraction(s: String, expected: String) {
        val tokens = tokenize(s)
        val actual = extractPhrases(tokens).joinToString(", ")

        actual shouldBeEqualTo expected
    }
}
