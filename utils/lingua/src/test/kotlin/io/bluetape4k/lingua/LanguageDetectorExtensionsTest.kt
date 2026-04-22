package io.bluetape4k.lingua

import com.github.pemistahl.lingua.api.Language
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class LanguageDetectorExtensionsTest: AbstractLinguaTest() {

    private val detector = allLanguageDetector {
        withMinimumRelativeDistance(0.0)
    }
    private val mixedText = "Parlez-vous français? Ich spreche nur ein bisschen Deutsch. A little bit is better than nothing."

    @Test
    fun `blank 입력이면 빈 집합을 반환한다`() {
        detector.detectAllLanguagesOf("   ") shouldBeEqualTo emptySet()
    }

    @Test
    fun `단일 언어면 singleton set을 반환한다`() {
        detector.detectAllLanguagesOf("Hello world") shouldBeEqualTo setOf(Language.ENGLISH)
    }

    @Test
    fun `혼합 언어면 모든 검출 언어를 집합으로 반환한다`() {
        detector.detectAllLanguagesOf(mixedText) shouldBeEqualTo setOf(
            Language.FRENCH,
            Language.GERMAN,
            Language.ENGLISH,
        )
    }

    @Test
    fun `영어와 한국어가 섞이면 모든 검출 언어를 집합으로 반환한다`() {
        detector.detectAllLanguagesOf("Hello 안녕") shouldBeEqualTo setOf(
            Language.ENGLISH,
            Language.KOREAN,
        )
    }

    @Test
    fun `세 개 언어가 섞이면 모든 검출 언어를 집합으로 반환한다`() {
        detector.detectAllLanguagesOf("Hello 안녕 こんにちは") shouldBeEqualTo setOf(
            Language.ENGLISH,
            Language.KOREAN,
            Language.JAPANESE,
        )
    }

    @Test
    fun `인식할 수 없는 입력이면 빈 집합을 반환한다`() {
        detector.detectAllLanguagesOf("🔥🎉🧪") shouldBeEqualTo emptySet()
    }
}
