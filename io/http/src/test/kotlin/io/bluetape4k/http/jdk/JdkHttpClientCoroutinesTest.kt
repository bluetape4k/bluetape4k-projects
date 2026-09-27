package io.bluetape4k.http.jdk

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeEmpty
import io.bluetape4k.coroutines.flow.async
import io.bluetape4k.http.AbstractHttpTest
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import org.junit.jupiter.api.Test
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.time.Duration.Companion.seconds

class JdkHttpClientCoroutinesTest: AbstractHttpTest() {

    companion object: KLoggingChannel()

    private val urisToGet: List<String>
        get() = listOf(
            "$httpbinBaseUrl/get",
            "$httpbinBaseUrl/ip",
            "$httpbinBaseUrl/headers"
        )

    @Test
    fun `getAwait 로 GET 요청 상태코드 200`() = runSuspendIO(timeout = 30.seconds) {
        val client = jdkHttpClientOf()
        val response = client.getAwait("$httpbinBaseUrl/get")
        log.debug { "GET $httpbinBaseUrl/get status=${response.statusCode()}" }
        response.statusCode() shouldBeEqualTo 200
    }

    @Test
    fun `getStringAwait 로 GET 요청 상태코드 200 및 body 비어있지 않음`() = runSuspendIO(timeout = 30.seconds) {
        val client = jdkHttpClientOf()
        val response = client.getStringAwait("$httpbinBaseUrl/get")
        log.debug { "GET $httpbinBaseUrl/get status=${response.statusCode()}" }
        response.statusCode() shouldBeEqualTo 200
        response.body().shouldNotBeEmpty()
    }

    @Test
    fun `sendAwait 로 커스텀 요청 상태코드 200`() = runSuspendIO(timeout = 30.seconds) {
        val client = jdkHttpClientOf()
        val request = HttpRequest.newBuilder(URI.create("$httpbinBaseUrl/get")).GET().build()
        val response = client.sendAwait(request, HttpResponse.BodyHandlers.ofByteArray())
        log.debug { "GET $httpbinBaseUrl/get status=${response.statusCode()}" }
        response.statusCode() shouldBeEqualTo 200
    }

    @Test
    fun `여러 URL 병렬 coroutine GET 요청 모두 200`() = runSuspendIO(timeout = 30.seconds) {
        val client = jdkHttpClientOf()
        val responses = urisToGet.asFlow()
            .async { uri ->
                val response = client.getAwait(uri)
                log.debug { "GET $uri status=${response.statusCode()}" }
                response
            }
            .toList()

        responses.forEach { response ->
            response.statusCode() shouldBeEqualTo 200
        }
    }
}
