package io.bluetape4k.http.okhttp3

import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeBlank
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.assertions.fail
import io.bluetape4k.concurrent.allAsList
import io.bluetape4k.concurrent.onFailure
import io.bluetape4k.concurrent.onSuccess
import io.bluetape4k.http.AbstractHttpTest
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.error
import io.bluetape4k.logging.trace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import org.apache.commons.lang3.time.StopWatch
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

class OkHttp3SupportTest: AbstractHttpTest() {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 3
        private const val TEST_SIZE = 10
    }

    private val client: OkHttpClient = okhttp3Client {
        connectTimeout(Duration.ofSeconds(10))
        dispatcher(Dispatcher(Executors.newVirtualThreadPerTaskExecutor()))
    }

    @Nested
    inner class Async {

        @Test
        fun `OkHttpClient 비동기 GET`() = runSuspendIO {
            val request = okhttp3Request {
                url("$HTTPBIN_URL/get")
                get()
            }
            client.executeAsync(request).verifyResponse()
        }

        @RepeatedTest(REPEAT_SIZE)
        fun `OkHttpClient 비동기 GET 통신 성능 테스트`() = runSuspendIO {
            val request = okhttp3Request {
                url("$HTTPBIN_URL/get")
                get()
            }

            val futures = List(TEST_SIZE) { index ->
                val sw = StopWatch.createStarted()

                client.executeAsync(request)
                    .onSuccess { response ->
                        response.use {
                            sw.stop()
                            log.trace { "Run $index elapsed time=${sw.formatTime()}" }
                            it.isSuccessful.shouldBeTrue()
                            it.bodyAsString().shouldNotBeBlank()
                        }
                    }
                    .onFailure { error -> fail(cause = error) }
            }

            futures.allAsList().get()
        }

        private fun CompletableFuture<okhttp3.Response>.verifyResponse() {
            this
                .onSuccess { response ->
                    response.use {
                        val bodyStr = it.bodyAsString().shouldNotBeNull().shouldNotBeBlank()
                        log.trace { "Response body=$bodyStr" }
                    }
                }
                .onFailure { error ->
                    log.error(error) { "Failed to execute request" }
                    fail("Failed to execute request", error)
                }
                .get()  // 이게 Blocking 이라는 겁니다 ㅠㅠ
        }
    }

    @Nested
    inner class Coroutines {
        @Test
        fun `OkHttpClient Suspend GET`() = runSuspendIO {
            val request = okhttp3Request {
                url("$HTTPBIN_URL/get")
                get()
            }
            client.executeSuspending(request).verifyResponse()
        }

        @RepeatedTest(REPEAT_SIZE)
        fun `OkHttpClient Suspend GET 통신 성능 테스트`() = runSuspendIO {
            val request = okhttp3Request {
                url("$HTTPBIN_URL/get")
                get()
            }

            val tasks = List(TEST_SIZE) { index ->
                async(Dispatchers.IO) {
                    val sw = StopWatch.createStarted()

                    client.executeSuspending(request).use {
                        sw.stop()
                        log.trace { "Run $index elapsed time=${sw.formatTime()}" }
                        it.isSuccessful.shouldBeTrue()
                        it.bodyAsString().shouldNotBeBlank()
                    }
                }
            }

            tasks.awaitAll()
        }

        private fun okhttp3.Response.verifyResponse() {
            use {
                val bodyStr = it.bodyAsString().shouldNotBeNull().shouldNotBeBlank()
                log.trace { "Response body=$bodyStr" }
            }
        }
    }
}
