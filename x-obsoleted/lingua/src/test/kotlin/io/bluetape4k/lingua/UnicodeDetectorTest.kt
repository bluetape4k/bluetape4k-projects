package io.bluetape4k.lingua

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.util.*

class UnicodeDetectorTest: AbstractLinguaTest() {

    companion object: KLogging()

    private val unicodeDetector = UnicodeDetector()

    @Nested
    inner class ContainsAll {

        @ParameterizedTest
        @CsvSource(
            "내가 말하는 것이 무슨 언어일까?, ko, true",
            "私が話している言葉は何ですか？, ja, false",       // 한자가 들어가 있으므로 모두는 아니다.
            "아라카토, ja, false",
            "我说的是什么语言?, zh, true",
            "What language am I speaking?, en, true",
            "ภาษาที่ฉันพูดคืออะไร?, th, true",
        )
        fun `모든 문자가 예상되는 문자에 해당하는가`(text: String, language: String, containsAll: Boolean) {
            val locale = Locale.of(language)
            unicodeDetector.containsAll(text, locale) shouldBeEqualTo containsAll
        }
    }

    @Nested
    inner class ContainsAny {

        @ParameterizedTest
        @CsvSource(
            "내가 말하는 것이 무슨 언어일까? Korean or English or Japanese, ko, true",
            "私が話している言葉は何ですか？, ja, true",       // 한자가 들어가 있으므로 모두는 아니다.
            "아라카토 ですか？, ja, true",
            "我说的是什么语言? 니파로마, zh, true",
            "What language am I speaking? 안녕하세요, en, true",
            "ภาษาที่ฉันพูดคืออะไร? Hello world, th, true",
        )
        fun `모든 문자가 예상되는 문자에 해당하는가`(text: String, language: String, containsAny: Boolean) {
            val locale = Locale.of(language)
            unicodeDetector.containsAny(text, locale) shouldBeEqualTo containsAny
        }
    }
}
