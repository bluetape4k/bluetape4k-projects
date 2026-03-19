package io.bluetape4k.ahocorasick.trie

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class TrieEdgeCaseTest {
    companion object: KLogging()

    @Test
    fun `빈 문자열 키워드 처리`() {
        val trie =
            Trie
                .builder()
                .addKeyword("")
                .build()

        val emits = trie.parseText("hello world")

        // 빈 문자열은 처리되지 않아야 함
        emits.shouldBeEmpty()
    }

    @Test
    fun `빈 텍스트 파싱`() {
        val trie =
            Trie
                .builder()
                .addKeywords("abc", "def")
                .build()

        val emits = trie.parseText("")

        emits.shouldBeEmpty()
    }

    @Test
    fun `tokenize 빈 텍스트`() {
        val trie =
            Trie
                .builder()
                .addKeywords("abc", "def")
                .build()

        val tokens = trie.tokenize("")

        tokens.shouldBeEmpty()
    }

    @Test
    fun `containsMatch 빈 텍스트`() {
        val trie =
            Trie
                .builder()
                .addKeywords("abc")
                .build()

        trie.containsMatch("").shouldBeFalse()
    }

    @Test
    fun `firstMatch 빈 텍스트`() {
        val trie =
            Trie
                .builder()
                .addKeywords("abc")
                .build()

        val firstMatch = trie.firstMatch("")

        firstMatch.shouldBeNull()
    }

    @Test
    fun `키워드가 없는 Trie`() {
        val trie = Trie.builder().build()

        val emits = trie.parseText("hello world")

        emits.shouldBeEmpty()
    }

    @Test
    fun `특수 문자 키워드`() {
        val trie =
            Trie
                .builder()
                .addKeywords("@#$", "!!!", "C++", "C#")
                .build()

        val emits = trie.parseText("Hello @#$ world!!! C++ and C#")

        emits.size shouldBeEqualTo 4
    }

    @Test
    fun `숫자 키워드`() {
        val trie =
            Trie
                .builder()
                .addKeywords("123", "456", "789")
                .build()

        val emits = trie.parseText("Call 123-456-7890")

        emits.size shouldBeEqualTo 3
    }

    @Test
    fun `긴 키워드`() {
        val longKeyword = "a".repeat(1000)
        val trie =
            Trie
                .builder()
                .addKeyword(longKeyword)
                .build()

        val emits = trie.parseText("prefix${longKeyword}suffix")

        emits.size shouldBeEqualTo 1
        emits[0].keyword shouldBeEqualTo longKeyword
    }

    @Test
    fun `매우 긴 텍스트`() {
        val longText = "abc".repeat(10000)
        val trie =
            Trie
                .builder()
                .addKeyword("abc")
                .build()

        val emits = trie.parseText(longText)

        emits.size shouldBeEqualTo 10000
    }

    @Test
    fun `ignoreCase와 onlyWholeWords 함께 사용`() {
        val trie =
            Trie
                .builder()
                .ignoreCase()
                .onlyWholeWords()
                .addKeyword("test")
                .build()

        val emits = trie.parseText("TEST testing TESTER test")

        // "TEST"와 "test"는 전체 단어로 매칭됨 (ignoreCase 적용)
        // "testing"과 "TESTER"는 "test"가 접두사이므로 제외됨 (onlyWholeWords 적용)
        emits.size shouldBeEqualTo 2
    }

    @Test
    fun `동일한 키워드 중복 추가`() {
        val trie =
            Trie
                .builder()
                .addKeyword("test")
                .addKeyword("test")
                .addKeyword("test")
                .build()

        val emits = trie.parseText("test")

        // 중복 추가되어도 한 번만 매칭되어야 함
        emits.size shouldBeEqualTo 1
    }

    @Test
    fun `공백만 있는 키워드`() {
        val trie =
            Trie
                .builder()
                .addKeyword("   ")
                .build()

        val emits = trie.parseText("Hello   world")

        // 공백 키워드도 정상적으로 매칭되어야 함
        emits.size shouldBeEqualTo 1
    }

    @Test
    fun `replace - 빈 맵`() {
        val trie =
            Trie
                .builder()
                .addKeywords("abc", "def")
                .build()

        val result = trie.replace("abc def", emptyMap())

        result shouldBeEqualTo "abc def"
    }

    @Test
    fun `replace - 매칭되지 않는 키워드`() {
        val trie =
            Trie
                .builder()
                .addKeywords("abc", "def")
                .build()

        val map = mapOf("xyz" to "replacement")
        val result = trie.replace("abc def", map)

        result shouldBeEqualTo "abc def"
    }

    @Test
    fun `replace - 모든 키워드 매칭`() {
        val trie =
            Trie
                .builder()
                .addKeywords("abc", "def")
                .build()

        val map = mapOf("abc" to "XYZ", "def" to "123")
        val result = trie.replace("abc def", map)

        result shouldBeEqualTo "XYZ 123"
    }

    @Test
    fun `연속된 매치`() {
        val trie =
            Trie
                .builder()
                .addKeywords("aa", "aaa")
                .build()

        val emits = trie.parseText("aaaa")

        // "aaaa"에서 "aa"는 3번, "aaa"는 2번 매칭될 수 있음
        emits.isNotEmpty().shouldBeTrue()
    }

    @Test
    fun `줄바꿈 문자 포함`() {
        val trie =
            Trie
                .builder()
                .addKeywords("hello", "world")
                .build()

        val emits = trie.parseText("hello\nworld\nhello")

        emits.size shouldBeEqualTo 3
    }

    @Test
    fun `탭 문자 포함`() {
        val trie =
            Trie
                .builder()
                .addKeywords("hello", "world")
                .build()

        val emits = trie.parseText("hello\tworld\thello")

        emits.size shouldBeEqualTo 3
    }
}
