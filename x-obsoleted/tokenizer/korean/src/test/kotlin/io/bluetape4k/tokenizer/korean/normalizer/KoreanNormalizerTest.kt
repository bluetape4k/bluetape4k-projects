package io.bluetape4k.tokenizer.korean.normalizer

import io.bluetape4k.tokenizer.korean.TestBase
import io.bluetape4k.tokenizer.korean.normalizer.KoreanNormalizer.correctTypo
import io.bluetape4k.tokenizer.korean.normalizer.KoreanNormalizer.normalize
import io.bluetape4k.tokenizer.korean.normalizer.KoreanNormalizer.normalizeCodaN
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class KoreanNormalizerTest: TestBase() {

    @Test
    fun `should normalize ㅋㅋ ㅎㅎ ㅠㅠ ㅜㅜ chunks`() {

        val expected = "안돼ㅋㅋㅋ내 심장을 가격했어ㅋㅋㅋ"
        val actual = normalize("안됔ㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋ내 심장을 가격했엌ㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋ")
        actual shouldBeEqualTo expected

        normalize("무의식중에 손들어버려섴ㅋㅋㅋㅋ") shouldBeEqualTo "무의식중에 손들어버려서ㅋㅋㅋ"
        normalize("기억도 나지아낳ㅎㅎㅎ") shouldBeEqualTo "기억도 나지아나ㅎㅎㅎ"
        normalize("근데비싸서못머구뮤ㅠㅠ") shouldBeEqualTo "근데비싸서못먹음ㅠㅠ"

        normalize("미친 존잘니뮤ㅠㅠㅠㅠ") shouldBeEqualTo "미친 존잘님ㅠㅠㅠ"
        normalize("만나무ㅜㅜㅠ") shouldBeEqualTo "만남ㅜㅜㅠ"
        normalize("가루ㅜㅜㅜㅜ") shouldBeEqualTo "가루ㅜㅜㅜ"

        normalize("유성우ㅠㅠㅠ") shouldBeEqualTo "유성우ㅠㅠㅠ"

        normalize("예뿌ㅠㅠ") shouldBeEqualTo "예뻐ㅠㅠ"
        normalize("고수야고수ㅠㅠㅠ") shouldBeEqualTo "고수야고수ㅠㅠㅠ"

        normalize("안돼ㅋㅋㅋㅋㅋ") shouldBeEqualTo "안돼ㅋㅋㅋ"
    }

    @Test
    fun `should normalize repeated chunks`() {
        normalize("땡큐우우우우우") shouldBeEqualTo "땡큐우우우"
        normalize("구오오오오오오오오옹오오오") shouldBeEqualTo "구오오오옹오오오"
    }

    @Test
    fun `should normalize repeated 2-letters`() {
        normalize("훌쩍훌쩍훌쩍훌쩍훌쩍훌쩍훌쩍훌쩍훌쩍훌쩍훌쩍훌쩍훌쩍") shouldBeEqualTo "훌쩍훌쩍"
        normalize("ㅋㅎㅋㅎㅋㅎㅋㅎㅋㅎㅋㅎ") shouldBeEqualTo "ㅋㅎㅋㅎ"
    }

    @Test
    fun `should normalize repeated 3-letters`() {
        normalize("사브작사브작사브작사브작사브작사브작사브작사브작") shouldBeEqualTo "사브작사브작"
        normalize("ㅋㅋㅎㅋㅋㅎㅋㅋㅎㅋㅋㅎㅋㅋㅎㅋㅋㅎ") shouldBeEqualTo "ㅋㅋㅎㅋㅋㅎ"
    }

    @Test
    fun `should not normalize non-Korean chunks`() {
        val expected = "http://11111.cccccom soooooooo !!!!!!!!!!!!!!!"
        val actual = normalize("http://11111.cccccom soooooooo !!!!!!!!!!!!!!!")
        actual shouldBeEqualTo expected
    }

    @Test
    fun `should have correctTypo integrated`() {
        normalize("가쟝 용기있는 사람이 머굼 되는거즤") shouldBeEqualTo "가장 용기있는 사람이 먹음 되는거지"
    }

    @Test
    fun `should have normalizeCodaN integrated`() {
        normalize("오노딘가") shouldBeEqualTo "오노디인가"
        normalize("관곈지") shouldBeEqualTo "관계인지"
        normalize("생각하는건데") shouldBeEqualTo "생각하는건데"
        normalize("생각컨대") shouldBeEqualTo "생각컨대"
    }

    @Test
    fun `should normalize coda N nouns`() {
        normalizeCodaN("오노딘가") shouldBeEqualTo "오노디인가"
        normalizeCodaN("소린가") shouldBeEqualTo "소리인가"

        normalizeCodaN("버슨가") shouldBeEqualTo "버스인가"
        normalizeCodaN("보슨지") shouldBeEqualTo "보스인지"

        normalizeCodaN("쵸킨데") shouldBeEqualTo "쵸킨데"
    }

    @Test
    fun `should not normalize if the input is known in the dictionary`() {
        normalizeCodaN("누군가") shouldBeEqualTo "누군가"
        normalizeCodaN("군가") shouldBeEqualTo "군가"
    }

    @Test
    fun `should not normalize if the input is an adjective or a verb`() {
        normalizeCodaN("가는건데") shouldBeEqualTo "가는건데"
        normalizeCodaN("곤란한데") shouldBeEqualTo "곤란한데"
        normalizeCodaN("생각하는건데") shouldBeEqualTo "생각하는건데"
        normalizeCodaN("졸린데") shouldBeEqualTo "졸린데"
    }

    @Test
    fun `should correct typos`() {
        correctTypo("가쟝 용기있는 사람이 머굼 되는거즤") shouldBeEqualTo "가장 용기있는 사람이 먹음 되는거지"
        correctTypo("만듀 먹것니? 먹겄서? 먹즤?") shouldBeEqualTo "만두 먹겠니? 먹겠어? 먹지?"
    }
}
