package io.bluetape4k.http.hc5.examples

import io.bluetape4k.http.hc5.AbstractHc5Test
import io.bluetape4k.http.hc5.classic.httpClientConnectionManager
import io.bluetape4k.http.hc5.classic.httpClientOf
import io.bluetape4k.http.hc5.http.futureRequestExecutionServiceOf
import io.bluetape4k.http.hc5.protocol.httpClientContextOf
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.error
import org.amshove.kluent.fail
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.core5.concurrent.FutureCallback
import org.apache.hc.core5.http.HttpStatus
import org.apache.hc.core5.http.io.HttpClientResponseHandler
import org.junit.jupiter.api.Test
import java.util.concurrent.CancellationException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ClientWithRequestFuture: AbstractHc5Test() {

    companion object: KLogging()

    @Test
    fun `client with request future`() {
        // the simplest way to create a HttpAsyncClientWithFuture
        val cm = httpClientConnectionManager {
            setMaxConnPerRoute(5)
            setMaxConnTotal(5)
        }
        val httpclient = httpClientOf(cm)

        val executor = Executors.newFixedThreadPool(5)

        futureRequestExecutionServiceOf(httpclient, executor).use { requestExecService ->
            // Because things are asynchronous, you must provide a HttpClientResponseHandler
            val handler = HttpClientResponseHandler { response ->
                // simply return true if the status was OK
                response.code == HttpStatus.SC_OK
            }

            // Simple request ..
            val request1 = HttpGet("$httpbinBaseUrl/get")
            val futureTask1 = requestExecService.execute(request1, httpClientContextOf(), handler)
            val wasItOk1 = futureTask1.get()
            log.debug { "It was ok? $wasItOk1" }

            // Cancel a request
            try {
                val request2 = HttpGet("$httpbinBaseUrl/get")
                val futureTask2 = requestExecService.execute(request2, httpClientContextOf(), handler)
                futureTask2.cancel(true)
                Thread.sleep(10)
                val wasItOk2 = futureTask2.get()
                fail("여기까지 실행되면 안됩니다. 작업이 취소되어야 합니다.")
            } catch (e: CancellationException) {
                log.debug { "We cancelled it, so this is expected" }
            }

            // Request with a timeout
            val request3 = HttpGet("$httpbinBaseUrl/get")
            val futureTask3 = requestExecService.execute(request3, httpClientContextOf(), handler)
            val wasItOk3 = futureTask3.get(10, TimeUnit.SECONDS)
            log.debug { "It was ok? $wasItOk3" }

            val callback = object: FutureCallback<Boolean> {
                override fun completed(result: Boolean?) {
                    log.debug { "completed with $result" }
                }

                override fun failed(ex: Exception?) {
                    log.error(ex) { "failed." }
                }

                override fun cancelled() {
                    log.debug { "cancelled" }
                }
            }

            // Simple request with callback
            val request4 = HttpGet("$httpbinBaseUrl/get")

            // using a null HttpContext here since it is optional
            // the callback will be called when the task completes, fails, or is cancelled
            val futureTask4 = requestExecService.execute(request4, httpClientContextOf(), handler, callback)
            val wasItOk4 = futureTask4.get(10, TimeUnit.SECONDS)
            log.debug { "It was ok? $wasItOk4" }
        }
    }
}
