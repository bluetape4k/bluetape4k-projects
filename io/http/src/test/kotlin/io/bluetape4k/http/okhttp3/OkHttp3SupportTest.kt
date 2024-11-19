package io.bluetape4k.http.okhttp3

import io.bluetape4k.concurrent.allAsList
import io.bluetape4k.concurrent.onFailure
import io.bluetape4k.concurrent.onSuccess
import io.bluetape4k.http.AbstractHttpTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.error
import io.bluetape4k.logging.trace
import okhttp3.OkHttpClient
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeBlank
import org.apache.commons.lang3.time.StopWatch
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.CompletableFuture

class OkHttp3SupportTest: AbstractHttpTest() {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 3
        private const val TEST_SIZE = 10
    }

    private val client: OkHttpClient = okhttp3Client {
        connectTimeout(Duration.ofSeconds(10))
    }

    @Test
    fun `OkHttpClient 비동기 GET`() {
        val request = okhttp3Request {
            url(JSON_PLACEHOLDER_TODOS_URL)
            get()
        }
        client.executeAsync(request).verifyResponse()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `OkHttpClient 비동기 GET 통신 성능 테스트`() {
        val request = okhttp3Request {
            url(JSON_PLACEHOLDER_TODOS_URL)
            get()
        }

        val futures = List(TEST_SIZE) { index ->
            val sw = StopWatch.createStarted()

            client.executeAsync(request)
                .onSuccess { response ->
                    sw.stop()
                    log.trace { "Run $index elapsed time=${sw.formatTime()}" }
                    response.isSuccessful.shouldBeTrue()
                }
                .onFailure { error -> fail(error) }
        }

        val responses = futures.allAsList().get()
        responses.all { it.isSuccessful }.shouldBeTrue()
    }

    private fun CompletableFuture<okhttp3.Response>.verifyResponse() {
        this
            .onSuccess { response ->
                val bodyStr = response.bodyAsString()
                log.trace { "Response body=$bodyStr" }
                bodyStr!!.shouldNotBeBlank()
            }
            .onFailure { error ->
                log.error(error) { "Failed to execute request" }
                fail("Failed to execute request", error)
            }
            .get()  // 이게 Blocking 이라는 겁니다 ㅠㅠ
    }
}
