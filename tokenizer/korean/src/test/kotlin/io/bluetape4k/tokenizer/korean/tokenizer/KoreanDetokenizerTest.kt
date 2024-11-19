package io.bluetape4k.tokenizer.korean.tokenizer

import io.bluetape4k.logging.KLogging
import io.bluetape4k.tokenizer.korean.KoreanProcessor.detokenize
import io.bluetape4k.tokenizer.korean.TestBase
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
import org.junit.jupiter.api.Test

class KoreanDetokenizerTest: TestBase() {

    companion object: KLogging()

    @Test
    fun `should detokenize the input text`() = runTest {

        var actual = detokenize(listOf("연세", "대학교", "보건", "대학원", "에", "오신", "것", "을", "환영", "합니다", "!"))
        actual shouldBeEqualTo "연세대학교 보건 대학원에 오신것을 환영합니다!"

        actual = detokenize(listOf("와", "!!!", "iPhone", "6+", "가", ",", "드디어", "나왔다", "!"))
        actual shouldContain "와!!! iPhone 6+"
        actual shouldContain "가, 드디어 나왔다!"

        actual = detokenize(listOf("뭐", "완벽", "하진", "않", "지만", "그럭저럭", "쓸", "만", "하군", "..."))
        actual shouldBeEqualTo "뭐 완벽하진 않지만 그럭저럭 쓸 만하군..."
    }

    @Test
    fun `should detokenize the edge cases`() = runTest {
        detokenize(listOf("")) shouldBeEqualTo ""
        detokenize(emptyList()) shouldBeEqualTo ""
        detokenize(listOf("완벽")) shouldBeEqualTo "완벽"
        detokenize(listOf("이")) shouldBeEqualTo "이"
        detokenize(listOf("이", "제품을", "사용하겠습니다")) shouldBeEqualTo "이 제품을 사용하겠습니다"
    }
}
