package io.bluetape4k.http.hc5.fluent

import io.bluetape4k.concurrent.virtualthread.VirtualThreadExecutor
import io.bluetape4k.coroutines.support.awaitSuspending
import io.bluetape4k.http.hc5.AbstractHc5Test
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.error
import io.bluetape4k.logging.trace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import org.apache.hc.client5.http.fluent.Async
import org.apache.hc.client5.http.fluent.Content
import org.apache.hc.core5.concurrent.FutureCallback
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnJre
import org.junit.jupiter.api.condition.JRE
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** HttpClient Fluent API로 여러 요청을 비동기로 실행하는 예제입니다. */
class FluentAsyncExample: AbstractHc5Test() {

    companion object: KLoggingChannel()

    val requests by lazy {
        listOf(
            requestGet("$httpbinBaseUrl/get?site=1"),
            requestGet("$httpbinBaseUrl/get?site=2"),
            requestGet("$httpbinBaseUrl/ip"),
            requestGet("$httpbinBaseUrl/headers"),
            requestGet("$httpbinBaseUrl/user-agent"),
            requestGet("$httpbinBaseUrl/uuid"),
            requestGet("$httpbinBaseUrl/get?site=7"),
        )
    }

    @Test
    fun `execute multiple request asynchronously`() {
        val executor = Executors.newFixedThreadPool(2)
        try {
            val async = Async.newInstance().use(executor)
            val queue = LinkedList<Future<Content>>()

            requests.forEach { request ->
                val future = async.execute(
                    request,
                    object: FutureCallback<Content> {
                        override fun completed(result: Content?) {
                            log.debug { "요청 전송 완료: $result" }
                        }

                        override fun failed(ex: Exception?) {
                            log.error(ex) { "실패. 요청 정보=$request" }
                        }

                        override fun cancelled() {
                            log.debug { "요청이 취소되었습니다." }
                        }
                    }
                )
                queue.add(future)
            }

            while (queue.isNotEmpty()) {
                val future = queue.remove()
                try {
                    future.get(1, TimeUnit.SECONDS)
                } catch (ex: ExecutionException) {
                    // 무시
                }
            }
            log.debug { "Done !!!" }
        } finally {
            runCatching {
                executor.shutdown()
                executor.awaitTermination(1, TimeUnit.SECONDS)
            }
        }
    }

    @Test
    fun `execute multiple request in multi threading`() {
        val executor = Executors.newFixedThreadPool(2)
        val async = Async.newInstance().use(executor)
        val counter = AtomicInteger(0)

        try {
            MultithreadingTester()
                .workers(requests.size / 2)
                .rounds(requests.size)
                .add {
                    val index = counter.getAndIncrement() % requests.size
                    val request = requests[index]
                    log.trace { "요청: $request" }

                    val content = async.execute(request).get()
                    log.trace { "Content type=${content.type} from $request" }
                }
                .run()
        } finally {
            runCatching {
                executor.shutdown()
                executor.awaitTermination(1, TimeUnit.SECONDS)
            }
        }
    }

    @EnabledOnJre(JRE.JAVA_21)
    @Test
    fun `execute multiple request in virtual threads`() {
        val async = Async.newInstance().use(VirtualThreadExecutor)
        val counter = AtomicInteger(0)

        StructuredTaskScopeTester()
            .rounds(requests.size * requests.size / 2)
            .add {
                val index = counter.getAndIncrement() % requests.size
                val request = requests[index]
                log.trace { "Request $request" }

                val content = async.execute(request).get()
                log.trace { "Content type=${content.type} from $request" }
            }
            .run()
    }

    @Test
    fun `execute multiple request in multi job`() = runSuspendIO {
        val async = Async.newInstance().use(Dispatchers.IO.asExecutor())
        val counter = AtomicInteger(0)

        SuspendedJobTester()
            .workers(requests.size / 2)
            .rounds(requests.size)
            .add {
                val index = counter.getAndIncrement() % requests.size
                val request = requests[index]
                log.trace { "Reqeust $request" }

                val content = async.execute(request).awaitSuspending()
                log.trace { "Content type=${content.type} from $request" }
            }
            .run()
    }
}
