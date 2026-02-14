package io.bluetape4k.testcontainers

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class GenericServerSupportTest {

    @AfterEach
    fun clearProperties() {
        System.clearProperty("testcontainers.redis.host")
        System.clearProperty("testcontainers.redis.port")
        System.clearProperty("testcontainers.redis.url")
        System.clearProperty("testcontainers.redis.ssl")
        System.clearProperty("testcontainers.redis.nullable")
    }

    @Test
    fun `writeToSystemProperties 는 기본 속성과 extra 속성을 기록한다`() {
        val server = mockk<GenericServer>(relaxed = true) {
            every { host } returns "127.0.0.1"
            every { port } returns 6379
            every { url } returns "127.0.0.1:6379"
        }

        server.writeToSystemProperties("redis", mapOf("ssl" to true, "nullable" to null))

        assertEquals("127.0.0.1", System.getProperty("testcontainers.redis.host"))
        assertEquals("6379", System.getProperty("testcontainers.redis.port"))
        assertEquals("127.0.0.1:6379", System.getProperty("testcontainers.redis.url"))
        assertEquals("true", System.getProperty("testcontainers.redis.ssl"))
        assertNull(System.getProperty("testcontainers.redis.nullable"))
    }

    @Test
    fun `writeToSystemProperties 는 빈 이름을 허용하지 않는다`() {
        val server = mockk<GenericServer>(relaxed = true)

        assertFailsWith<IllegalArgumentException> {
            server.writeToSystemProperties(" ")
        }
    }
}
