package io.bluetape4k.ahocorasick.trie

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class TokenTest {
    companion object: KLogging()

    @Test
    fun `MatchToken 생성 및 검증`() {
        val emit = Emit(0, 4, "hello")
        val token = MatchToken("hello", emit)

        token.fragment shouldBeEqualTo "hello"
        token.emit shouldBeEqualTo emit
        token.isMatch().shouldBeTrue()
    }

    @Test
    fun `FragmentToken 생성 및 검증`() {
        val token = FragmentToken("world")

        token.fragment shouldBeEqualTo "world"
        token.emit.shouldBeNull()
        token.isMatch().shouldBeFalse()
    }

    @Test
    fun `Token 비교 - MatchToken vs FragmentToken`() {
        val emit = Emit(5, 9, "match")
        val matchToken = MatchToken("match", emit)
        val fragmentToken = FragmentToken("text")

        matchToken.isMatch().shouldBeTrue()
        fragmentToken.isMatch().shouldBeFalse()
    }

    @Test
    fun `Token 리스트 처리`() {
        val emit1 = Emit(0, 2, "abc")
        val emit2 = Emit(4, 6, "def")

        val tokens =
            listOf<Token>(
                FragmentToken("start "),
                MatchToken("abc", emit1),
                FragmentToken(" middle "),
                MatchToken("def", emit2),
                FragmentToken(" end"),
            )

        tokens.filter { it.isMatch() }.size shouldBeEqualTo 2
        tokens.filter { !it.isMatch() }.size shouldBeEqualTo 3
    }

    @Test
    fun `MatchToken toString 검증`() {
        val emit = Emit(0, 3, "test")
        val token = MatchToken("test", emit)

        val str = token.toString()
        str.contains("test").shouldBeTrue()
        str.contains("emit").shouldBeTrue()
    }

    @Test
    fun `FragmentToken toString 검증`() {
        val token = FragmentToken("fragment")

        val str = token.toString()
        str.contains("fragment").shouldBeTrue()
    }

    @Test
    fun `빈 문자열 Token 처리`() {
        val emit = Emit(0, 0, "")
        val matchToken = MatchToken("", emit)
        val fragmentToken = FragmentToken("")

        matchToken.fragment shouldBeEqualTo ""
        matchToken.isMatch().shouldBeTrue()

        fragmentToken.fragment shouldBeEqualTo ""
        fragmentToken.isMatch().shouldBeFalse()
    }

    @Test
    fun `유니코드 문자열 Token 처리`() {
        val emit = Emit(0, 4, "안녕하세요")
        val matchToken = MatchToken("안녕하세요", emit)
        val fragmentToken = FragmentToken("세상")

        matchToken.fragment shouldBeEqualTo "안녕하세요"
        matchToken.isMatch().shouldBeTrue()

        fragmentToken.fragment shouldBeEqualTo "세상"
        fragmentToken.isMatch().shouldBeFalse()
    }
}
