package io.bluetape4k.bucket4j.distributed.redis

import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.redisson.api.RedissonClient

class RedissonBasedProxyManagerSupportTest {

    @Test
    fun `Redisson 구현체가 아니면 명확한 예외를 던진다`() {
        val client = mockk<RedissonClient>()

        assertThrows<IllegalArgumentException> {
            redissonBasedProxyManagerOf(client) {}
        }
    }
}
