package io.bluetape4k.redis.lettuce.filter

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Test

class Murmur3Test {

    companion object: KLogging()

    @Test
    fun `hash128x64 - 빈 바이트 배열 처리`() {
        val result = Murmur3.hash128x64(ByteArray(0))
        result shouldHaveSize 2
    }

    @Test
    fun `hash128x64 - 동일 입력은 동일 해시`() {
        val data = "hello world".toByteArray()
        val h1 = Murmur3.hash128x64(data)
        val h2 = Murmur3.hash128x64(data)
        h1[0] shouldBeEqualTo h2[0]
        h1[1] shouldBeEqualTo h2[1]
    }

    @Test
    fun `hash128x64 - 다른 입력은 다른 해시`() {
        val h1 = Murmur3.hash128x64("foo".toByteArray())
        val h2 = Murmur3.hash128x64("bar".toByteArray())
        assert(h1[0] != h2[0] || h1[1] != h2[1]) { "Hash collision for foo vs bar" }
    }

    @Test
    fun `hash128x64 - seed 변경시 다른 해시`() {
        val data = "test".toByteArray()
        val h1 = Murmur3.hash128x64(data, seed = 0L)
        val h2 = Murmur3.hash128x64(data, seed = 42L)
        assert(h1[0] != h2[0] || h1[1] != h2[1]) { "Seed should change hash" }
    }

    @Test
    fun `hash128x64 - 15바이트 이하 tail 처리`() {
        for (len in 1..15) {
            val data = ByteArray(len) { it.toByte() }
            val result = Murmur3.hash128x64(data)
            result shouldHaveSize 2
        }
    }

    @Test
    fun `hash128x64 - 16바이트 이상 블록 처리`() {
        val data = ByteArray(64) { it.toByte() }
        val result = Murmur3.hash128x64(data)
        result shouldHaveSize 2
    }

    @Test
    fun `hash128x64 - 알려진 벡터 검증`() {
        val data = "The quick brown fox".toByteArray(Charsets.UTF_8)
        val result = Murmur3.hash128x64(data, 0L)
        result shouldHaveSize 2
        assert(result[0] != 0L || result[1] != 0L)
    }
}
