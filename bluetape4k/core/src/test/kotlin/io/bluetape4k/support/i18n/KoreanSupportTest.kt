package io.bluetape4k.support.i18n

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.EMPTY_STRING
import io.bluetape4k.support.emptyCharArray
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class KoreanSupportTest {

    companion object: KLogging()

    @Test
    fun `한글 완성형이 포함된 문자열 감지`() {
        "한글".containKorean().shouldBeTrue()
        "한글abc".containKorean().shouldBeTrue()
        "abc한글".containKorean().shouldBeTrue()
        "한abc글".containKorean().shouldBeTrue()
        "한¡™£¢글".containKorean().shouldBeTrue()
    }

    @Test
    fun `한글 자모가 포함된 문자열 감지`() {
        "ㄱㄴㄷ".containKorean().shouldBeTrue()
        "ㅏㅓㅗ".containKorean().shouldBeTrue()
        "abcㄱ".containKorean().shouldBeTrue()
        "ㄱabc".containKorean().shouldBeTrue()
        "¡™£¢ㅎㄱ".containKorean().shouldBeTrue()
        "¡™£¢한".containKorean().shouldBeTrue()
    }

    @Test
    fun `한글이 없는 문자열`() {
        "abc".containKorean().shouldBeFalse()
        "123".containKorean().shouldBeFalse()
        "".containKorean().shouldBeFalse()
        " ".containKorean().shouldBeFalse()
        " \t ".containKorean().shouldBeFalse()
        "¡™£¢".containKorean().shouldBeFalse()
    }

    @Test
    fun `한글 자모와 완성형 경계값 검사`() {
        "\u3131".containKorean().shouldBeTrue()
        "\u3163".containKorean().shouldBeTrue()
        "\uAC00".containKorean().shouldBeTrue()
        "\uD7A3".containKorean().shouldBeTrue()
    }

    @Test
    fun `한글 자소 분해`() {
        val result = "한국".getJasoLetter()
        log.debug { "한국 -> $result" }
        result shouldBeEqualTo "ㅎㅏㄴㄱㅜㄱ"
    }

    @Test
    fun `종성이 없는 한글 자소 분해`() {
        val result = "가나다".getJasoLetter()
        log.debug { "가나다 -> $result" }
        result shouldBeEqualTo "ㄱㅏㄴㅏㄷㅏ"
    }

    @Test
    fun `혼합 문자열 자소 분해 시 비한글 문자 유지`() {
        val result = "한국abc".getJasoLetter()
        log.debug { "한국abc -> $result" }
        result shouldBeEqualTo "ㅎㅏㄴㄱㅜㄱabc"

        "한abc국".getJasoLetter() shouldBeEqualTo "ㅎㅏㄴabcㄱㅜㄱ"
        "abc한국".getJasoLetter() shouldBeEqualTo "abcㅎㅏㄴㄱㅜㄱ"
    }

    @Test
    fun `빈 문자열 자소 분해`() {
        "".getJasoLetter() shouldBeEqualTo EMPTY_STRING
        " ".getJasoLetter() shouldBeEqualTo EMPTY_STRING
        " \t ".getJasoLetter() shouldBeEqualTo EMPTY_STRING
    }

    @Test
    fun `초성 추출`() {
        val result = "대한민국".getChosung()
        log.debug { "대한민국 -> ${result.contentToString()}" }
        result shouldBeEqualTo charArrayOf('ㄷ', 'ㅎ', 'ㅁ', 'ㄱ')
    }

    @Test
    fun `혼합 문자열 초성 추출 시 비한글 무시`() {
        val result = "대한abc민국".getChosung()
        log.debug { "대한abc민국 -> ${result.contentToString()}" }
        result shouldBeEqualTo charArrayOf('ㄷ', 'ㅎ', 'ㅁ', 'ㄱ')
    }

    @Test
    fun `빈 문자열 초성 추출`() {
        "".getChosung() shouldBeEqualTo emptyCharArray
        " ".getChosung() shouldBeEqualTo emptyCharArray
        " \t ".getChosung() shouldBeEqualTo emptyCharArray
        "¡™£¢".getChosung() shouldBeEqualTo emptyCharArray
    }
}
