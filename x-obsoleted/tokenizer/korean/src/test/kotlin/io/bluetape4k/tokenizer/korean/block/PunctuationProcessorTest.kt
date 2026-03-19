package io.bluetape4k.tokenizer.korean.block

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class PunctuationProcessorTest {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 3
    }

    private val punctuationProcessor = PunctuationProcessor()

    @Test
    fun `중간 punctuation 제거하기`() = runTest {
        val actual = "중.@.고등#학교에서... 너는 뭐_했니? 난_1학년^^ 확.성기 섹.스 찌~~~찌~~~~~뽕"
        val punctuationRemoved = "중고등학교에서... 너는 뭐했니? 난1학년^^ 확성기 섹스 찌찌뽕"

        val removePunctuation = punctuationProcessor.removePunctuation(actual)
        log.trace { "remove punctuation=$removePunctuation" }
        removePunctuation shouldBeEqualTo punctuationRemoved
    }


    @Test
    fun `중간 punctuation이 여러개일 때`() = runTest {
        val actual = "중.@.고등#학교에서... 너는 뭐~.~했니? 난~ 1학년^^"
        val punctuationRemoved = "중고등학교에서... 너는 뭐했니? 난~ 1학년^^"

        val removePunctuation = punctuationProcessor.removePunctuation(actual)
        log.trace { "remove punctuation=$removePunctuation" }
        removePunctuation shouldBeEqualTo punctuationRemoved
    }

    @Disabled("Punctuation 이 중간에 있다면, 공백은 무시하고 제거되어야한다")
    @Test
    fun `중간 punctuation이 여러 개이고 공백이 있을 때는 처리를 못한다`() = runTest {
        val actual = "중.@.고등#학교에서... 너는 뭐 ~ . ~ 했니? 난~ 1학년^^"
        val punctuationRemoved = "중고등학교에서... 너는 뭐했니? 난~ 1학년^^"

        val removePunctuation = punctuationProcessor.removePunctuation(actual)
        log.trace { "remove punctuation=$removePunctuation" }
        removePunctuation shouldBeEqualTo punctuationRemoved
    }
}
