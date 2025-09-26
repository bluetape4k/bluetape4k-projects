package io.bluetape4k.http.ahc

import io.bluetape4k.concurrent.allAsList
import io.bluetape4k.concurrent.onFailure
import io.bluetape4k.concurrent.onSuccess
import io.bluetape4k.http.AbstractHttpTest
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import io.bluetape4k.utils.Systemx
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.asynchttpclient.AsyncHttpClient
import org.asynchttpclient.AsyncHttpClientConfig
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.fail

class AsyncHttpClientTest: AbstractHttpTest() {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 3
        private const val TEST_SIZE = 10

        private val requestFilter = AttachHeaderRequestFilter(mapOf("app.key" to "app-12345"))
        private val dynamicAttachHeaderRequestFilter = DynamicAttachHandlerRequest(listOf("a", "b")) { name ->
            when (name) {
                "a" -> "header-value-a"
                "b" -> "header-value-b"
                else -> "none"
            }
        }
    }

    private val ahc = asyncHttpClientOf(requestFilter, dynamicAttachHeaderRequestFilter)
    private val ahcTransport =
        asyncHttpClientOf(defaultAsyncHttpClientConfig)

    @AfterAll
    fun afterAll() {
        ahc.close()
        ahcTransport.close()
    }

    @Test
    fun `비동기 GET 호출`() {
        executeGetAsync(ahc)
    }

    @Test
    fun `Coroutine GET 호출`() = runSuspendIO {
        executeGetSuspend(ahc)
    }

    @EnabledOnOs(OS.LINUX, OS.MAC)
    @RepeatedTest(REPEAT_SIZE)
    fun `netty native event poll 사용해서 비동기 실행하기`() {
        executeGetAsync(ahc, TEST_SIZE)
    }

    @EnabledOnOs(OS.LINUX, OS.MAC)
    @RepeatedTest(REPEAT_SIZE)
    fun `netty native event poll 사용해서 Coroutines 환경에서 실행하기`() = runSuspendIO {
        executeGetSuspend(ahc, TEST_SIZE)
    }

    private fun executeGetAsync(ahc: AsyncHttpClient, count: Int = TEST_SIZE) {
        val futures = List(count) {
            ahc
                .prepareGet(JSON_PLACEHOLDER_TODOS_URL)
                .execute()
                .toCompletableFuture()
                .onSuccess { response ->
                    response.statusCode shouldBeEqualTo 200
                    response.hasResponseBody().shouldBeTrue()
                    log.trace { "Response body=${response.responseBody}" }
                }
                .onFailure { error ->
                    fail(error)
                }
        }

        futures.allAsList().join()
    }

    private suspend fun executeGetSuspend(ahc: AsyncHttpClient, count: Int = TEST_SIZE) = coroutineScope {
        val jobs = List(count) {
            launch(Dispatchers.IO) {
                val response = ahc
                    .prepareGet(JSON_PLACEHOLDER_TODOS_URL)
                    .executeSuspending()

                response.statusCode shouldBeEqualTo 200
                response.hasResponseBody().shouldBeTrue()
                log.trace { "Response body=${response.responseBody}" }
            }
        }
        jobs.joinAll()
    }

    private fun getAsyncHttpClientConfig(): AsyncHttpClientConfig {
        return asyncHttpClientConfig {
            setKeepAlive(true)
            setCompressionEnforced(true)
            setFollowRedirect(true)
            setTcpNoDelay(true)
            setSoReuseAddress(true)

            runCatching {
                if (Systemx.isUnix) {
                    // setEventLoopGroup(EpollEventLoopGroup())
                    setUseNativeTransport(true)
                } else if (Systemx.isMac) {
                    // setEventLoopGroup(KQueueEventLoopGroup())
                    setUseNativeTransport(true)
                } else {
                    // Nothing to do
                }
            }
        }
    }
}
