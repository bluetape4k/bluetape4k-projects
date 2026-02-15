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
        // HttpAsyncClientWithFuture를 구성하는 가장 단순한 방법
        val cm = httpClientConnectionManager {
            setMaxConnPerRoute(5)
            setMaxConnTotal(5)
        }
        val httpclient = httpClientOf(cm)

        val executor = Executors.newFixedThreadPool(5)

        futureRequestExecutionServiceOf(httpclient, executor).use { requestExecService ->
            // 비동기 실행이므로 HttpClientResponseHandler를 제공해야 합니다.
            val handler = HttpClientResponseHandler { response ->
                // 상태 코드가 OK면 true 반환
                response.code == HttpStatus.SC_OK
            }

            // 기본 요청
            val request1 = HttpGet("$httpbinBaseUrl/get")
            val futureTask1 = requestExecService.execute(request1, httpClientContextOf(), handler)
            val wasItOk1 = futureTask1.get()
            log.debug { "It was ok? $wasItOk1" }

            // 요청 취소
            try {
                val request2 = HttpGet("$httpbinBaseUrl/get")
                val futureTask2 = requestExecService.execute(request2, httpClientContextOf(), handler)
                futureTask2.cancel(true)
                Thread.sleep(10)
                val wasItOk2 = futureTask2.get()
                log.debug { "It was ok? $wasItOk2" }
                fail("여기까지 실행되면 안됩니다. 작업이 취소되어야 합니다.")
            } catch (e: CancellationException) {
                log.debug { "취소 후 예외가 발생하는 것이 정상 동작입니다." }
            }

            // 타임아웃이 있는 요청
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

            // 콜백 기반 요청
            val request4 = HttpGet("$httpbinBaseUrl/get")

            // HttpContext는 선택 사항이므로 null 대신 기본 컨텍스트를 사용합니다.
            // 콜백은 완료/실패/취소 시 호출됩니다.
            val futureTask4 = requestExecService.execute(request4, httpClientContextOf(), handler, callback)
            val wasItOk4 = futureTask4.get(10, TimeUnit.SECONDS)
            log.debug { "It was ok? $wasItOk4" }
        }
    }
}
