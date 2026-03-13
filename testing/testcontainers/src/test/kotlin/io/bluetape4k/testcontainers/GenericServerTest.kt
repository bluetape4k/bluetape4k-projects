package io.bluetape4k.testcontainers

import io.bluetape4k.logging.KLogging
import io.mockk.every
import io.mockk.mockk
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

/**
 * [writeToSystemProperties] 확장 함수에 대한 단위 테스트입니다.
 */
class GenericServerTest {
    companion object : KLogging() {
        private const val TEST_SERVER_NAME = "test-generic-server"
    }

    @AfterEach
    fun cleanup() {
        System.clearProperty("$SERVER_PREFIX.$TEST_SERVER_NAME.host")
        System.clearProperty("$SERVER_PREFIX.$TEST_SERVER_NAME.port")
        System.clearProperty("$SERVER_PREFIX.$TEST_SERVER_NAME.url")
        System.clearProperty("$SERVER_PREFIX.$TEST_SERVER_NAME.extra")
    }

    private fun fakeServer(
        host: String = "localhost",
        port: Int = 12345,
    ): GenericServer =
        mockk<GenericServer> {
            every { getHost() } returns host
            every { this@mockk.port } returns port
            every { url } returns "http://$host:$port"
        }

    @Test
    fun `writeToSystemProperties 는 기본 속성을 System Property에 등록한다`() {
        val server = fakeServer(host = "localhost", port = 12345)

        server.writeToSystemProperties(TEST_SERVER_NAME)

        System.getProperty("$SERVER_PREFIX.$TEST_SERVER_NAME.host") shouldBeEqualTo "localhost"
        System.getProperty("$SERVER_PREFIX.$TEST_SERVER_NAME.port") shouldBeEqualTo "12345"
        System.getProperty("$SERVER_PREFIX.$TEST_SERVER_NAME.url") shouldBeEqualTo "http://localhost:12345"
    }

    @Test
    fun `writeToSystemProperties 는 extraProps 를 System Property에 추가로 등록한다`() {
        val server = fakeServer(host = "localhost", port = 12345)

        server.writeToSystemProperties(TEST_SERVER_NAME, mapOf("extra" to "extraVal"))

        System.getProperty("$SERVER_PREFIX.$TEST_SERVER_NAME.extra") shouldBeEqualTo "extraVal"
    }

    @Test
    fun `writeToSystemProperties 는 extraProps 의 null 값을 무시한다`() {
        val server = fakeServer(host = "localhost", port = 12345)

        server.writeToSystemProperties(TEST_SERVER_NAME, mapOf("extra" to null))

        System.getProperty("$SERVER_PREFIX.$TEST_SERVER_NAME.extra").shouldBeNull()
    }

    @Test
    fun `writeToSystemProperties 는 blank name 에 IllegalArgumentException 을 던진다`() {
        val server = fakeServer()

        assertFailsWith<IllegalArgumentException> {
            server.writeToSystemProperties("   ")
        }
    }

    @Test
    fun `writeToSystemProperties 는 빈 name 에 IllegalArgumentException 을 던진다`() {
        val server = fakeServer()

        assertFailsWith<IllegalArgumentException> {
            server.writeToSystemProperties("")
        }
    }
}
