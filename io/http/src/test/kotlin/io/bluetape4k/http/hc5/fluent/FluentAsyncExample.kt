package io.bluetape4k.http.hc5.fluent

import io.bluetape4k.concurrent.virtualthread.VirtualThreadExecutor
import io.bluetape4k.coroutines.support.suspendAwait
import io.bluetape4k.http.hc5.AbstractHc5Test
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.error
import io.bluetape4k.logging.trace
import kotlinx.atomicfu.atomic
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

/**
 * This example demonstrates how the he HttpClient fluent API can be used to execute multiple
 * requests asynchronously using background threads.
 */
class FluentAsyncExample: AbstractHc5Test() {

    companion object: KLoggingChannel()

    val requests = listOf(
        requestGet("http://www.naver.com/"),
        requestGet("http://www.daum.net/"),
        requestGet("http://www.chosun.com/"),
        requestGet("https://news.kbs.co.kr/news/pc/main/main.html"),
        requestGet("http://www.ytn.co.kr/"),
        requestGet("http://www.mbc.co.kr/"),
        requestGet("http://www.sbs.co.kr/"),
    )

    @Test
    fun `execute multiple request asynchronously`() {
        val executor = Executors.newFixedThreadPool(2)
        try {
            val async = Async.newInstance().use(executor)
            val queue = LinkedList<Future<Content>>()

            requests.forEach { request ->
                val future = async.execute(request, object: FutureCallback<Content> {
                    override fun completed(result: Content?) {
                        log.debug { "Request completed: $result" }
                    }

                    override fun failed(ex: Exception?) {
                        log.error(ex) { "failed. request=$request" }
                    }

                    override fun cancelled() {
                        log.debug { "Cancelled." }
                    }
                })
                queue.add(future)
            }

            while (queue.isNotEmpty()) {
                val future = queue.remove()
                try {
                    future.get()
                } catch (ex: ExecutionException) {
                    // Nothing to do
                }
            }
            log.debug { "Done" }
        } finally {
            executor.shutdown()
        }
    }

    @Test
    fun `execute multiple request in multi threading`() {
        val executor = Executors.newFixedThreadPool(2)
        val async = Async.newInstance().use(executor)
        val counter = atomic(0)

        try {
            MultithreadingTester()
                .numThreads(requests.size / 2)
                .roundsPerThread(2)
                .add {
                    val index = counter.getAndIncrement() % requests.size
                    val request = requests[index]
                    log.trace { "Reqeust $request" }

                    val content = async.execute(request).get()
                    log.trace { "Content type=${content.type} from $request" }
                }
                .run()
        } finally {
            executor.shutdown()
        }
    }

    @EnabledOnJre(JRE.JAVA_21)
    @Test
    fun `execute multiple request in virtual threads`() {
        val async = Async.newInstance().use(VirtualThreadExecutor)
        val counter = atomic(0)

        StructuredTaskScopeTester()
            .roundsPerTask(requests.size)
            .add {
                val index = counter.getAndIncrement() % requests.size
                val request = requests[index]
                log.trace { "Reqeust $request" }

                val content = async.execute(request).get()
                log.trace { "Content type=${content.type} from $request" }
            }
            .run()
    }

    @Test
    fun `execute multiple request in multi job`() = runSuspendIO {
        val async = Async.newInstance().use(Dispatchers.IO.asExecutor())
        val counter = atomic(0)

        SuspendedJobTester()
            .numThreads(requests.size / 2)
            .roundsPerJob(requests.size)
            .add {
                val index = counter.getAndIncrement() % requests.size
                val request = requests[index]
                log.trace { "Reqeust $request" }

                val content = async.execute(request).suspendAwait()
                log.trace { "Content type=${content.type} from $request" }
            }
            .run()
    }
}
