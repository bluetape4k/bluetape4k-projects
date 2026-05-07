package io.bluetape4k.testcontainers.http

import com.github.tomakehurst.wiremock.client.WireMock
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.testcontainers.AbstractContainerTest
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldContain
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.net.HttpURLConnection
import java.net.URI
import kotlin.test.assertFailsWith

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WireMockServerTest: AbstractContainerTest() {

    companion object: KLogging()

    private lateinit var wireMock: WireMockServer

    @BeforeAll
    fun beforeAll() {
        wireMock = WireMockServer().apply { start() }
    }

    @BeforeEach
    fun beforeEach() {
        // 각 테스트 전에 stub 상태 초기화 — 누수 방지
        wireMock.resetAll()
    }

    @AfterAll
    fun afterAll() {
        if(this::wireMock.isInitialized && wireMock.isRunning) {
            wireMock.close()
        }
    }

    @Test
    fun `WireMock 서버가 정상적으로 실행된다`() {
        wireMock.isRunning.shouldBeTrue()
        log.debug { "WireMock baseUrl: ${wireMock.baseUrl}" }
    }

    @Test
    fun `GET hello 요청에 200 응답과 바디를 반환한다`() {
        wireMock.stubFor(
            WireMock.get("/hello").willReturn(WireMock.ok("Hello, World!"))
        )

        val url = URI("${wireMock.baseUrl}/hello").toURL()
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.connect()

            conn.responseCode shouldBeEqualTo 200
            val body = conn.inputStream.reader().buffered().readText()
            log.debug { "response body: $body" }
            body shouldContain "Hello, World!"
        } finally {
            conn.disconnect()
        }
    }

    @Test
    fun `등록하지 않은 경로는 404를 반환한다`() {
        val url = URI("${wireMock.baseUrl}/not-registered").toURL()
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.connect()

            conn.responseCode shouldBeEqualTo 404
        } finally {
            conn.disconnect()
        }
    }

    @Test
    fun `JSON 응답 stub을 등록하고 검증한다`() {
        wireMock.stubFor(
            WireMock.get("/api/data").willReturn(
                WireMock.okJson("""{"status":"ok","value":42}""")
            )
        )

        val url = URI("${wireMock.baseUrl}/api/data").toURL()
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.connect()

            conn.responseCode shouldBeEqualTo 200
            val body = conn.inputStream.reader().buffered().readText()
            log.debug { "JSON response: $body" }
            body shouldContain "\"status\":\"ok\""
        } finally {
            conn.disconnect()
        }
    }

    @Test
    fun `blank image tag 는 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> { WireMockServer(image = " ") }
        assertFailsWith<IllegalArgumentException> { WireMockServer(tag = " ") }
    }

    @Test
    fun `start 전에 stubFor 호출하면 IllegalStateException 이 발생한다`() {
        val unstartedServer = WireMockServer()
        assertFailsWith<IllegalStateException> {
            unstartedServer.stubFor(WireMock.get("/test").willReturn(WireMock.ok()))
        }
    }

    @Test
    fun `start 전에 resetAll 호출하면 IllegalStateException 이 발생한다`() {
        val unstartedServer = WireMockServer()
        assertFailsWith<IllegalStateException> {
            unstartedServer.resetAll()
        }
    }

    @Test
    fun `resetAll 후 이전 stub 이 제거된다`() {
        wireMock.stubFor(
            WireMock.get("/to-be-reset").willReturn(WireMock.ok("before reset"))
        )

        wireMock.resetAll()

        val url = URI("${wireMock.baseUrl}/to-be-reset").toURL()
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.connect()
            conn.responseCode shouldBeEqualTo 404
        } finally {
            conn.disconnect()
        }
    }
}
