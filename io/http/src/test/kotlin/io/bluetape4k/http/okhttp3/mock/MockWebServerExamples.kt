package io.bluetape4k.http.okhttp3.mock

import com.fasterxml.jackson.module.kotlin.readValue
import io.bluetape4k.LibraryName
import io.bluetape4k.http.okhttp3.bodyAsString
import io.bluetape4k.http.okhttp3.execute
import io.bluetape4k.http.okhttp3.okhttp3Client
import io.bluetape4k.http.okhttp3.okhttp3Request
import io.bluetape4k.jackson.Jackson
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration

class MockWebServerExamples {

    companion object: KLogging() {
        private const val SUCCESS_RESPONSE_BODY = """{ "payload": "hello" }"""
    }

    private lateinit var mockServer: MockWebServer

    private val client: OkHttpClient = okhttp3Client {
        connectTimeout(Duration.ofSeconds(10))
    }
    private val jsonMapper = Jackson.defaultJsonMapper

    @BeforeEach
    fun beforeEach() {
        mockServer = MockWebServer().apply { start() }
    }

    @AfterEach
    fun afterEach() {
        mockServer.shutdown()
    }

    data class Message(val payload: String)

    @Test
    fun `성공 시나리오에 따른 메시지 조회`() {
        mockServer.enqueueBody(SUCCESS_RESPONSE_BODY)

        val request = okhttp3Request {
            url(mockServer.baseUrl)
            get()
        }
        assertHttpResponse(request)
    }

    @Test
    fun `응답 지연이 있는 성공 시나리오`() {
        mockServer.enqueueBody(SUCCESS_RESPONSE_BODY)
        mockServer.enqueueBodyWithDelay(SUCCESS_RESPONSE_BODY, Duration.ofMillis(500))

        val request = okhttp3Request {
            url(mockServer.baseUrl)
            get()
        }
        assertHttpResponse(request)

        val request2 = okhttp3Request {
            url(mockServer.baseUrl)
            get()
        }
        assertHttpResponse(request2)
    }

    @Test
    fun `MockServer 에서 Request 정보 조회하기`() {
        mockServer.enqueueBody(SUCCESS_RESPONSE_BODY)

        val request = okhttp3Request {
            url(mockServer.baseUrl + "repository?name=$LibraryName")
            get()
        }
        assertHttpResponse(request)

        // 서버가 받은 Request
        val recordedRequest = mockServer.takeRequest()
        log.debug { "Recorded Request: $recordedRequest" }
        recordedRequest.method shouldBeEqualTo "GET"
        recordedRequest.requestUrl.toString() shouldBeEqualTo mockServer.baseUrl + "repository?name=$LibraryName"
        recordedRequest.path shouldBeEqualTo "/repository?name=$LibraryName"
    }

    private fun assertHttpResponse(request: okhttp3.Request) {
        val response = client.execute(request)

        response.isSuccessful.shouldBeTrue()
        val body = response.bodyAsString()!!
        body shouldContain "hello"

        val message = jsonMapper.readValue<Message>(body)
        message.payload shouldBeEqualTo "hello"
    }
}
