package io.bluetape4k.tokenizer.utils

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.StringReader

/**
 * CharacterUtils 테스트
 */
class CharacterUtilsTest {
    companion object: KLogging()

    private val charUtils = CharacterUtils.getInstance()

    @Test
    fun `getInstance returns singleton`() {
        val instance1 = CharacterUtils.getInstance()
        val instance2 = CharacterUtils.getInstance()

        instance1 shouldBeEqualTo instance2
    }

    @Test
    fun `codePointAt with CharSequence`() {
        // ASCII 문자
        charUtils.codePointAt("Hello", 0) shouldBeEqualTo 'H'.code
        charUtils.codePointAt("Hello", 1) shouldBeEqualTo 'e'.code

        // Unicode 문자 (한글)
        charUtils.codePointAt("한글", 0) shouldBeEqualTo '한'.code
        charUtils.codePointAt("한글", 1) shouldBeEqualTo '글'.code
    }

    @Test
    fun `codePointAt with CharArray`() {
        val chars = "Hello".toCharArray()

        charUtils.codePointAt(chars, 0, chars.size) shouldBeEqualTo 'H'.code
        charUtils.codePointAt(chars, 1, chars.size) shouldBeEqualTo 'e'.code
    }

    @Test
    fun `codePointAt with surrogate pair`() {
        // 이모지 (😀)는 surrogate pair로 표현됨
        val emoji = "😀"
        charUtils.codePointAt(emoji, 0) shouldBeEqualTo 0x1F600 // grinning face emoji
    }

    @Test
    fun `codePointCount - 글자 수 계산`() {
        // ASCII 문자
        charUtils.codePointCount("Hello") shouldBeEqualTo 5

        // 한글
        charUtils.codePointCount("한글") shouldBeEqualTo 2

        // 이모지 (surrogate pair)
        charUtils.codePointCount("😀😁") shouldBeEqualTo 2

        // 혼합
        charUtils.codePointCount("A한😀") shouldBeEqualTo 3
    }

    @Test
    fun `toLowerCase for byte array`() {
        val buffer = "HELLO".toCharArray()

        charUtils.toLowerCase(buffer, 0, buffer.size)

        String(buffer) shouldBeEqualTo "hello"
    }

    @Test
    fun `toLowerCase with partial range`() {
        val buffer = "HELLO WORLD".toCharArray()

        charUtils.toLowerCase(buffer, 0, 5)

        String(buffer) shouldBeEqualTo "hello WORLD"
    }

    @Test
    fun `toUpperCase`() {
        val buffer = "hello".toCharArray()

        charUtils.toUpperCase(buffer, 0, buffer.size)

        String(buffer) shouldBeEqualTo "HELLO"
    }

    @Test
    fun `toUpperCase with partial range`() {
        val buffer = "hello world".toCharArray()

        charUtils.toUpperCase(buffer, 6, buffer.size)

        String(buffer) shouldBeEqualTo "hello WORLD"
    }

    @Test
    fun `toCodePoints`() {
        val src = "Hello".toCharArray()
        val dest = IntArray(src.size)

        val count = charUtils.toCodePoints(src, 0, src.size, dest, 0)

        count shouldBeEqualTo 5
        dest[0] shouldBeEqualTo 'H'.code
        dest[1] shouldBeEqualTo 'e'.code
        dest[2] shouldBeEqualTo 'l'.code
        dest[3] shouldBeEqualTo 'l'.code
        dest[4] shouldBeEqualTo 'o'.code
    }

    @Test
    fun `toCodePoints with surrogate pair`() {
        val src = "😀".toCharArray() // 2 chars (surrogate pair)
        val dest = IntArray(2)

        val count = charUtils.toCodePoints(src, 0, src.size, dest, 0)

        count shouldBeEqualTo 1
        dest[0] shouldBeEqualTo 0x1F600
    }

    @Test
    fun `toChars`() {
        val src = intArrayOf('H'.code, 'e'.code, 'l'.code, 'l'.code, 'o'.code)
        val dest = CharArray(5)

        val count = charUtils.toChars(src, 0, src.size, dest, 0)

        count shouldBeEqualTo 5
        String(dest) shouldBeEqualTo "Hello"
    }

    @Test
    fun `toChars with code point`() {
        val src = intArrayOf(0x1F600) // grinning face emoji
        val dest = CharArray(2)

        val count = charUtils.toChars(src, 0, src.size, dest, 0)

        count shouldBeEqualTo 2 // surrogate pair
        String(dest, 0, count) shouldBeEqualTo "😀"
    }

    @Test
    fun `newCharacterBuffer`() {
        val buffer = CharacterUtils.newCharacterBuffer(10)

        buffer.buffer.size shouldBeEqualTo 10
        buffer.offset shouldBeEqualTo 0
        buffer.length shouldBeEqualTo 0
    }

    @Test
    fun `newCharacterBuffer minimum size`() {
        assertThrows<IllegalArgumentException> {
            CharacterUtils.newCharacterBuffer(1)
        }
    }

    @Test
    fun `fill buffer completely`() {
        val buffer = CharacterUtils.newCharacterBuffer(10)
        val reader = StringReader("HelloWorld")

        val filled = charUtils.fill(buffer, reader, 10)

        filled.shouldBeTrue()
        buffer.length shouldBeEqualTo 10
        String(buffer.buffer, buffer.offset, buffer.length) shouldBeEqualTo "HelloWorld"
    }

    @Test
    fun `fill buffer partially`() {
        val buffer = CharacterUtils.newCharacterBuffer(20)
        val reader = StringReader("Hello")

        val filled = charUtils.fill(buffer, reader, 20)

        filled.shouldBeFalse()
        buffer.length shouldBeEqualTo 5
        String(buffer.buffer, buffer.offset, buffer.length) shouldBeEqualTo "Hello"
    }

    @Test
    fun `fill with numChars parameter`() {
        val buffer = CharacterUtils.newCharacterBuffer(10)
        val reader = StringReader("HelloWorld")

        val filled = charUtils.fill(buffer, reader, 5)

        filled.shouldBeTrue()
        buffer.length shouldBeEqualTo 5
        String(buffer.buffer, buffer.offset, buffer.length) shouldBeEqualTo "Hello"
    }

    @Test
    fun `CharacterBuffer reset`() {
        val buffer = CharacterUtils.newCharacterBuffer(10)
        buffer.offset = 5
        buffer.length = 3
        buffer.lastTrailingHighSurrogate = 'A'

        buffer.reset()

        buffer.offset shouldBeEqualTo 0
        buffer.length shouldBeEqualTo 0
        buffer.lastTrailingHighSurrogate shouldBeEqualTo 0.toChar()
    }

    @Test
    fun `offsetByCodePoints forward`() {
        val text = "Hello World".toCharArray()

        val result = charUtils.offsetByCodePoints(text, 0, text.size, 0, 5)

        result shouldBeEqualTo 5
    }

    @Test
    fun `offsetByCodePoints backward`() {
        val text = "Hello World".toCharArray()

        val result = charUtils.offsetByCodePoints(text, 0, text.size, 10, -5)

        result shouldBeEqualTo 5
    }

    @Test
    fun `offsetByCodePoints with surrogate pair`() {
        val text = "A😀B".toCharArray() // A(1) + emoji(2) + B(1)

        // emoji는 code point 1개이지만 char 2개
        val result = charUtils.offsetByCodePoints(text, 0, text.size, 0, 2)

        // A(1) + emoji(2) = index 3
        result shouldBeEqualTo 3
    }

    @Test
    fun `readFully reads all data`() {
        val reader = StringReader("Hello")
        val dest = CharArray(5)

        val read = CharacterUtils.readFully(reader, dest, 0, 5)

        read shouldBeEqualTo 5
        String(dest) shouldBeEqualTo "Hello"
    }

    @Test
    fun `readFully with partial read`() {
        val reader = StringReader("Hi")
        val dest = CharArray(5)

        val read = CharacterUtils.readFully(reader, dest, 0, 5)

        read shouldBeEqualTo 2
        String(dest, 0, read) shouldBeEqualTo "Hi"
    }

    @Test
    fun `complex unicode text`() {
        // 다양한 유니코드 문자가 섞인 텍스트
        val text = "Hello 세상 😀🌍"

        // H(1), e(2), l(3), l(4), o(5), (6), 세(7), 상(8), (9), 😀(10), 🌍(11)
        charUtils.codePointCount(text) shouldBeEqualTo 11

        // 첫 6개 code points (Hello + space)
        val first6 = text.substring(0, 6) // "Hello "
        charUtils.codePointCount(first6) shouldBeEqualTo 6
    }
}
