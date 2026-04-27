package io.bluetape4k.http.hc5.async

import io.bluetape4k.http.hc5.AbstractHc5Test
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.apache.hc.client5.http.async.methods.SimpleRequestBuilder
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

class AsyncHttpClientCoroutinesTest: AbstractHc5Test() {

    companion object: KLogging()

    @Test
    fun `httpAsyncClientOf 로 executeSuspending GET 요청`() = runTest(timeout = 30.seconds) {
        httpAsyncClientOf().use { client ->
            val request = SimpleRequestBuilder.get("$httpbinBaseUrl/get").build()
            val response = client.executeSuspending(request)
            log.debug { "GET $httpbinBaseUrl/get status=${response.code}" }
            response.code shouldBeEqualTo 200
        }
    }

    @Test
    fun `여러 URL 병렬 coroutine GET 요청 모두 200`() = runTest(timeout = 30.seconds) {
        httpAsyncClientOf().use { client ->
            val responses = coroutineScope {
                urisToGet.map { uri ->
                    async {
                        val request = SimpleRequestBuilder.get(uri).build()
                        val response = client.executeSuspending(request)
                        log.debug { "GET $uri status=${response.code}" }
                        response
                    }
                }.awaitAll()
            }
            responses.forEach { response ->
                response.code shouldBeEqualTo 200
            }
        }
    }
}
