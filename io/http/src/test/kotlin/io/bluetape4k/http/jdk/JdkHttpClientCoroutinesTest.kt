package io.bluetape4k.http.jdk

import io.bluetape4k.http.AbstractHttpTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.Test
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.time.Duration.Companion.seconds

class JdkHttpClientCoroutinesTest: AbstractHttpTest() {

    companion object: KLogging()

    private val urisToGet: List<String>
        get() = listOf(
            "$httpbinBaseUrl/get",
            "$httpbinBaseUrl/ip",
            "$httpbinBaseUrl/headers"
        )

    @Test
    fun `getAwait 로 GET 요청 상태코드 200`() = runTest(timeout = 30.seconds) {
        val client = jdkHttpClientOf()
        val response = client.getAwait("$httpbinBaseUrl/get")
        log.debug { "GET $httpbinBaseUrl/get status=${response.statusCode()}" }
        response.statusCode() shouldBeEqualTo 200
    }

    @Test
    fun `getStringAwait 로 GET 요청 상태코드 200 및 body 비어있지 않음`() = runTest(timeout = 30.seconds) {
        val client = jdkHttpClientOf()
        val response = client.getStringAwait("$httpbinBaseUrl/get")
        log.debug { "GET $httpbinBaseUrl/get status=${response.statusCode()}" }
        response.statusCode() shouldBeEqualTo 200
        response.body().shouldNotBeEmpty()
    }

    @Test
    fun `sendAwait 로 커스텀 요청 상태코드 200`() = runTest(timeout = 30.seconds) {
        val client = jdkHttpClientOf()
        val request = HttpRequest.newBuilder(URI.create("$httpbinBaseUrl/get")).GET().build()
        val response = client.sendAwait(request, HttpResponse.BodyHandlers.ofByteArray())
        log.debug { "GET $httpbinBaseUrl/get status=${response.statusCode()}" }
        response.statusCode() shouldBeEqualTo 200
    }

    @Test
    fun `여러 URL 병렬 coroutine GET 요청 모두 200`() = runTest(timeout = 30.seconds) {
        val client = jdkHttpClientOf()
        val responses = coroutineScope {
            urisToGet.map { uri ->
                async {
                    val response = client.getAwait(uri)
                    log.debug { "GET $uri status=${response.statusCode()}" }
                    response
                }
            }.awaitAll()
        }
        responses.forEach { response ->
            response.statusCode() shouldBeEqualTo 200
        }
    }
}
