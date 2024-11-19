package io.bluetape4k.tokenizer.korean.utils

import io.bluetape4k.tokenizer.korean.TestBase
import io.bluetape4k.tokenizer.korean.tokenizer.KoreanToken
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Josa
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Modifier
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Noun
import io.bluetape4k.tokenizer.korean.utils.KoreanSubstantive.collapseNouns
import io.bluetape4k.tokenizer.korean.utils.KoreanSubstantive.isKoreanNameVariation
import io.bluetape4k.tokenizer.korean.utils.KoreanSubstantive.isKoreanNumber
import io.bluetape4k.tokenizer.korean.utils.KoreanSubstantive.isName
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class KoreanSubstantiveTest: TestBase() {

    private val josaEuns = charArrayOf('은', '이', '을', '과', '아')
    private val josaGas = charArrayOf('는', '가', '를', '와', '야', '여', '라')

    @Test
    fun `is Josa attachable`() {
        // 애플은
        for (josa in josaEuns) {
            KoreanSubstantive.isJosaAttachable('플', josa).shouldBeTrue()
        }

        //애플가
        for (josa in josaGas) {
            KoreanSubstantive.isJosaAttachable('플', josa).shouldBeFalse()
        }

        // 애프은
        for (josa in josaEuns) {
            KoreanSubstantive.isJosaAttachable('프', josa).shouldBeFalse()
        }

        //애프가
        for (josa in josaGas) {
            KoreanSubstantive.isJosaAttachable('프', josa).shouldBeTrue()
        }
    }

    @Test
    fun `isName should return false if input length less than 3`() {
        isName("김").shouldBeFalse()
        isName("관진").shouldBeFalse()
    }

    @Test
    fun `isName should correctly identify 3-char person names`() {
        isName("문재인").shouldBeTrue()
        isName("손나은").shouldBeTrue()
        isName("손석희").shouldBeTrue()
        isName("강철중").shouldBeTrue()

        isName("개루루").shouldBeFalse()
        isName("사측의").shouldBeFalse()
        isName("사다리").shouldBeFalse()
        isName("철지난").shouldBeFalse()
        isName("수용액").shouldBeFalse()
        isName("눈맞춰").shouldBeFalse()
    }

    @Test
    fun `isName should correctly identify 4-char person names`() {
        isName("독고영재").shouldBeTrue()
        isName("제갈경준").shouldBeTrue()

        isName("배권제형").shouldBeFalse()
    }

    @Test
    fun `isKoreanNumber by korean number text`() {
        isKoreanNumber("천이백만이십오").shouldBeTrue()
        isKoreanNumber("이십").shouldBeTrue()
        isKoreanNumber("오").shouldBeTrue()
        isKoreanNumber("삼").shouldBeTrue()
        isKoreanNumber("일천").shouldBeTrue()
        isKoreanNumber("일백육십팔").shouldBeTrue()
    }

    @Test
    fun `isKoreanNumber should return false if the text is not a Korean number`() {
        isKoreanNumber("영삼").shouldBeFalse()
        isKoreanNumber("이정").shouldBeFalse()
        isKoreanNumber("조삼모사").shouldBeFalse()
    }

    @Test
    fun `isKoreanNameVariation should correctly identify removed null consonats`() {
        isKoreanNameVariation("호혀니").shouldBeTrue()
        isKoreanNameVariation("혜지니").shouldBeTrue()
        isKoreanNameVariation("빠수니").shouldBeTrue()
        isKoreanNameVariation("은벼리").shouldBeTrue()
        isKoreanNameVariation("귀여미").shouldBeTrue()
        isKoreanNameVariation("루하니").shouldBeTrue()
        isKoreanNameVariation("이오니").shouldBeTrue()

        isKoreanNameVariation("이").shouldBeFalse()

        isKoreanNameVariation("장미").shouldBeFalse()
        isKoreanNameVariation("별이").shouldBeFalse()
        isKoreanNameVariation("꼬치").shouldBeFalse()
        isKoreanNameVariation("꽃이").shouldBeFalse()
        isKoreanNameVariation("팔티").shouldBeFalse()
        isKoreanNameVariation("감미").shouldBeFalse()
        isKoreanNameVariation("고미").shouldBeFalse()

        isKoreanNameVariation("가라찌").shouldBeFalse()
        isKoreanNameVariation("귀요미").shouldBeFalse()
        isKoreanNameVariation("사람이").shouldBeFalse()
        isKoreanNameVariation("사람이니").shouldBeFalse()
        isKoreanNameVariation("유하기").shouldBeFalse()

        isKoreanNameVariation("사랑이").shouldBeFalse()
        isKoreanNameVariation("도가니").shouldBeFalse()
    }

    @Test
    fun `collapseNous should collapse single-length nouns correctly`() {

        var tokens = collapseNouns(
            listOf(
                KoreanToken("마", Noun, 0, 1), KoreanToken("코", Noun, 1, 1), KoreanToken("토", Noun, 2, 1)
            )
        )
        var expected = listOf(KoreanToken("마코토", Noun, 0, 3, unknown = true))

        tokens shouldBeEqualTo expected

        tokens = collapseNouns(
            listOf(
                KoreanToken("마", Noun, 0, 1),
                KoreanToken("코", Noun, 1, 1),
                KoreanToken("토", Noun, 2, 1),
                KoreanToken("를", Josa, 3, 1)
            )
        )
        expected = listOf(
            KoreanToken("마코토", Noun, 0, 3, unknown = true), KoreanToken("를", Josa, 3, 1)
        )

        tokens shouldBeEqualTo expected


        tokens = collapseNouns(
            listOf(
                KoreanToken("개", Modifier, 0, 1),
                KoreanToken("마", Noun, 1, 1),
                KoreanToken("코", Noun, 2, 1),
                KoreanToken("토", Noun, 3, 1)
            )
        )
        expected = listOf(
            KoreanToken("개", Modifier, 0, 1), KoreanToken("마코토", Noun, 1, 3, unknown = true)
        )
        tokens shouldBeEqualTo expected

        tokens = collapseNouns(
            listOf(
                KoreanToken("마", Noun, 0, 1),
                KoreanToken("코", Noun, 1, 1),
                KoreanToken("토", Noun, 2, 1),
                KoreanToken("사람", Noun, 3, 2)
            )
        )
        expected = listOf(
            KoreanToken("마코토", Noun, 0, 3, unknown = true), KoreanToken("사람", Noun, 3, 2)
        )
        tokens shouldBeEqualTo expected

        tokens = collapseNouns(
            listOf(
                KoreanToken("마", Noun, 0, 1),
                KoreanToken("코", Noun, 1, 1),
                KoreanToken("사람", Noun, 2, 2),
                KoreanToken("토", Noun, 4, 1)
            )
        )
        expected = listOf(
            KoreanToken("마코", Noun, 0, 2, unknown = true), KoreanToken("사람", Noun, 2, 2), KoreanToken("토", Noun, 4, 1)
        )
        tokens shouldBeEqualTo expected
    }
}
