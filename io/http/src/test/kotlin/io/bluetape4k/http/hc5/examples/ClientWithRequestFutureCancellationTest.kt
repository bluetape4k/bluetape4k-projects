package io.bluetape4k.http.hc5.examples

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.http.hc5.http.futureRequestExecutionServiceOf
import io.bluetape4k.http.hc5.protocol.httpClientContextOf
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.core5.concurrent.FutureCallback
import org.apache.hc.core5.http.ClassicHttpRequest
import org.apache.hc.core5.http.io.HttpClientResponseHandler
import org.apache.hc.core5.http.protocol.HttpContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

/**
 * 실제 HC5 Future의 실행 순서를 제어해 취소 결과를 검증합니다.
 * executor는 작업 제출만 가로채며 Future와 callable의 상태 전이는 실제 구현을 사용합니다.
 */
class ClientWithRequestFutureCancellationTest {

    private val httpclient = mockk<CloseableHttpClient>(relaxed = true)
    private val executor = mockk<ExecutorService>(relaxed = true)
    private val callback = mockk<FutureCallback<Boolean>>(relaxed = true)
    private val task = slot<Runnable>()
    private val handler = HttpClientResponseHandler { true }

    @BeforeEach
    fun setUp() {
        clearMocks(httpclient, executor, callback)
        task.clear()
        // 스트레스 드라이버가 아니라 HC5에 전달하는 executor의 실행 순서를 제어합니다.
        every { executor.execute(capture(task)) } just Runs
    }

    @Test
    fun `실행 전에 취소하면 직접 취소 예외가 발생한다`() {
        futureRequestExecutionServiceOf(httpclient, executor).use { service ->
            val future = service.execute(HttpGet("http://localhost/unused"), httpClientContextOf(), handler)

            future.cancel(true).shouldBeTrue()
            task.captured.run()

            future.isCancelled.shouldBeTrue()
            assertFailsWith<CancellationException> { future.get(1, TimeUnit.SECONDS) }
            verify(exactly = 0) {
                httpclient.execute(any<ClassicHttpRequest>(), any<HttpContext>(), handler)
            }
        }
        verify(exactly = 1) {
            executor.shutdownNow()
            httpclient.close()
        }
    }

    @Test
    fun `취소 표시 후 callable이 먼저 완료되어도 취소 결과를 확인한다`() {
        // callable.cancel()의 표시 직후, FutureTask.cancel()보다 먼저 실행합니다.
        every { callback.cancelled() } answers { task.captured.run() }
        futureRequestExecutionServiceOf(httpclient, executor).use { service ->
            val future = service.execute(HttpGet("http://localhost/unused"), httpClientContextOf(), handler, callback)

            future.cancel(true).shouldBeFalse()

            future.isDone.shouldBeTrue()
            future.isCancelled.shouldBeFalse()
            // callable이 예외로 먼저 완료되면 get()이 취소 원인을 ExecutionException으로 감쌉니다.
            val error = assertFailsWith<ExecutionException> { future.get(1, TimeUnit.SECONDS) }
            error.cause.shouldBeInstanceOf<CancellationException>()
            verify(exactly = 1) { callback.cancelled() }
            verify(exactly = 0) {
                httpclient.execute(any<ClassicHttpRequest>(), any<HttpContext>(), handler)
            }
        }
        verify(exactly = 1) {
            executor.shutdownNow()
            httpclient.close()
        }
    }

    @Test
    fun `이미 성공한 요청은 취소 성공으로 오인하지 않는다`() {
        every { httpclient.execute(any<ClassicHttpRequest>(), any<HttpContext>(), handler) } returns true
        futureRequestExecutionServiceOf(httpclient, executor).use { service ->
            val future = service.execute(HttpGet("http://localhost/unused"), httpClientContextOf(), handler)
            task.captured.run()

            future.cancel(true).shouldBeFalse()

            future.isCancelled.shouldBeFalse()
            future.get(1, TimeUnit.SECONDS).shouldBeTrue()
        }
    }

    @Test
    fun `취소와 무관한 실패는 원인을 그대로 전달한다`() {
        val failure = IOException("요청 실패")
        every { httpclient.execute<Boolean>(any<ClassicHttpRequest>(), any<HttpContext>(), handler) } throws failure
        futureRequestExecutionServiceOf(httpclient, executor).use { service ->
            val future = service.execute(HttpGet("http://localhost/unused"), httpClientContextOf(), handler)
            task.captured.run()

            future.cancel(true).shouldBeFalse()

            val error = assertFailsWith<ExecutionException> { future.get(1, TimeUnit.SECONDS) }
            error.cause.shouldBeSameInstanceAs(failure)
        }
    }
}
