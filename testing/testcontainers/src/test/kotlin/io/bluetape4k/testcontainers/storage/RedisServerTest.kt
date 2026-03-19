package io.bluetape4k.testcontainers.storage

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.AbstractContainerTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.test.assertFailsWith

class RedisServerTest: AbstractContainerTest() {

    companion object: KLogging()

    @Nested
    inner class UseDefaultPort {
        @Test
        fun `create redis server with default port`() {
            RedisServer(useDefaultPort = true).use { redis ->
                redis.start()
                redis.isRunning.shouldBeTrue()
                redis.port shouldBeEqualTo RedisServer.PORT

                await atMost Duration.ofSeconds(5) until {
                    runCatching {
                        verifyWithRedisson(redis)
                        true
                    }.getOrDefault(false)
                }
            }
        }
    }

    @Nested
    inner class UseDockerPort {
        @Test
        fun `create redis server`() {
            RedisServer().use { redis ->
                redis.start()
                redis.isRunning.shouldBeTrue()

                verifyWithRedisson(redis)
                verifyWithLettuce(redis)
            }
        }

        @Test
        fun `create redis server by launcher`() {
            val redis = RedisServer.Launcher.redis
            redis.isRunning.shouldBeTrue()

            verifyWithRedisson(redis)
            verifyWithLettuce(redis)
        }
    }

    private fun verifyWithRedisson(redisServer: RedisServer) {
        val redisson = RedisServer.Launcher.RedissonLib.getRedisson(redisServer.url)

        try {
            val map = redisson.getMap<String, String>("map")
            map.fastPut("key1", "value1")

            val map2 = redisson.getMap<String, String>("map")
            map2["key1"] shouldBeEqualTo "value1"
        } finally {
            runCatching { redisson.shutdown() }
        }
    }

    private fun verifyWithLettuce(redisServer: RedisServer) {
        val redisClient = RedisServer.Launcher.LettuceLib.getRedisClient(redisServer.url)

        redisClient.connect().use { conn ->
            val command = conn.sync()
            val result = command.ping()
            result.shouldNotBeNull() shouldBeEqualTo "PONG"
        }
    }

    @Test
    fun `blank image tag 는 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> { RedisServer(image = " ") }
        assertFailsWith<IllegalArgumentException> { RedisServer(tag = " ") }
    }
}
