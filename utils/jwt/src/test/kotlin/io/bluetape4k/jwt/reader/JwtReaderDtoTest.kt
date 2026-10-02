package io.bluetape4k.jwt.reader

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldNotBeEqualTo
import org.junit.jupiter.api.Test
import java.util.HashMap
import java.util.HashSet

class JwtReaderDtoTest {

    @Test
    fun `같은 digest 내용은 별도 배열이어도 같은 hashCode를 가진다`() {
        val first = JwtReaderDto(digest = byteArrayOf(1, 2, 3))
        val second = JwtReaderDto(digest = byteArrayOf(1, 2, 3))

        first shouldBeEqualTo second
        first.hashCode() shouldBeEqualTo second.hashCode()
    }

    @Test
    fun `digest 내용이 다르면 DTO도 다르다`() {
        val first = JwtReaderDto(digest = byteArrayOf(1, 2, 3))
        val second = JwtReaderDto(digest = byteArrayOf(1, 2, 4))

        first shouldNotBeEqualTo second
    }

    @Test
    fun `null digest는 같은 hashCode를 유지하고 hash collection에서 조회된다`() {
        val first = JwtReaderDto()
        val second = JwtReaderDto(digest = null)

        first shouldBeEqualTo second
        first.hashCode() shouldBeEqualTo second.hashCode()
        HashSet<JwtReaderDto>().apply { add(first) } shouldContain second
        HashMap<JwtReaderDto, String>().apply { put(first, "cached") }[second] shouldBeEqualTo "cached"
    }

    @Test
    fun `같은 digest 내용의 DTO를 hash collection에서 조회한다`() {
        val stored = JwtReaderDto(digest = byteArrayOf(4, 5, 6))
        val equivalent = JwtReaderDto(digest = byteArrayOf(4, 5, 6))

        HashSet<JwtReaderDto>().apply { add(stored) } shouldContain equivalent
        HashMap<JwtReaderDto, String>().apply { put(stored, "cached") }[equivalent] shouldBeEqualTo "cached"
    }
}
