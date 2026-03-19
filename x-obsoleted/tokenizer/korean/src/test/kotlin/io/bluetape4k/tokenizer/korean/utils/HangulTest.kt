package io.bluetape4k.tokenizer.korean.utils

import io.bluetape4k.logging.KLogging
import io.bluetape4k.tokenizer.korean.TestBase
import io.bluetape4k.tokenizer.korean.utils.Hangul.HangulChar
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows


class HangulTest: TestBase() {

    companion object: KLogging()

    @Test
    fun `decompose KoreanChar with coda`() {
        Hangul.decomposeHangul('간') shouldBeEqualTo HangulChar('ㄱ', 'ㅏ', 'ㄴ')
        Hangul.decomposeHangul('관') shouldBeEqualTo HangulChar('ㄱ', 'ㅘ', 'ㄴ')
        Hangul.decomposeHangul('꼃') shouldBeEqualTo HangulChar('ㄲ', 'ㅕ', 'ㅀ')
    }

    @Test
    fun `decompose KoreanChar without coda`() {
        Hangul.decomposeHangul('가') shouldBeEqualTo HangulChar('ㄱ', 'ㅏ', ' ')
        Hangul.decomposeHangul('과') shouldBeEqualTo HangulChar('ㄱ', 'ㅘ', ' ')
        Hangul.decomposeHangul('껴') shouldBeEqualTo HangulChar('ㄲ', 'ㅕ', ' ')
    }

    @Test
    fun `decomposeHangul with invalid char`() {
        assertThrows<AssertionError> {
            Hangul.decomposeHangul('ㅋ')
        }

        assertThrows<AssertionError> {
            Hangul.decomposeHangul('ㅏ')
        }

        assertThrows<AssertionError> {
            Hangul.decomposeHangul('ㅀ')
        }
    }

    @Test
    fun `hasCoda with Coda`() {
        Hangul.hasCoda('갈').shouldBeTrue()
        Hangul.hasCoda('갉').shouldBeTrue()
    }

    @Test
    fun `hasCoda without Coda`() {
        Hangul.hasCoda('가').shouldBeFalse()
        Hangul.hasCoda('ㄱ').shouldBeFalse()
        Hangul.hasCoda('ㅘ').shouldBeFalse()
        Hangul.hasCoda('ㅀ').shouldBeFalse()
        Hangul.hasCoda(' ').shouldBeFalse()
    }

    @Test
    fun `compose KoreanChar with coda`() {
        Hangul.composeHangul('ㄱ', 'ㅏ', 'ㄷ') shouldBeEqualTo '갇'
        Hangul.composeHangul('ㄲ', 'ㅑ', 'ㅀ') shouldBeEqualTo '꺓'
        Hangul.composeHangul('ㅊ', 'ㅘ', 'ㄴ') shouldBeEqualTo '촨'
    }

    @Test
    fun `compose KoreanChar without coda`() {
        Hangul.composeHangul('ㄱ', 'ㅏ', ' ') shouldBeEqualTo '가'
        Hangul.composeHangul('ㄲ', 'ㅑ', ' ') shouldBeEqualTo '꺄'
        Hangul.composeHangul('ㅊ', 'ㅘ', ' ') shouldBeEqualTo '촤'
    }

    @Test
    fun `compose only vowel`() {
        assertThrows<AssertionError> {
            Hangul.composeHangul(' ', 'ㅏ', ' ')
        }

        assertThrows<AssertionError> {
            Hangul.composeHangul('ㄲ', ' ', ' ')
        }

        assertThrows<AssertionError> {
            Hangul.composeHangul(' ', ' ', 'ㄴ')
        }
    }
}
