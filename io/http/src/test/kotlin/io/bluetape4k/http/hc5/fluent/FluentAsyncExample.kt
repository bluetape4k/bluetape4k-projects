package io.bluetape4k.http.hc5.fluent

import io.bluetape4k.concurrent.virtualthread.VirtualThreadExecutor
import io.bluetape4k.coroutines.support.coAwait
import io.bluetape4k.http.hc5.AbstractHc5Test
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.VirtualthreadTester
import io.bluetape4k.junit5.coroutines.MultijobTester
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.error
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import org.apache.hc.client5.http.fluent.Async
import org.apache.hc.client5.http.fluent.Content
import org.apache.hc.core5.concurrent.FutureCallback
import org.junit.jupiter.api.Test
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * This example demonstrates how the he HttpClient fluent API can be used to execute multiple
 * requests asynchronously using background threads.
 */
class FluentAsyncExample: AbstractHc5Test() {

    companion object: KLogging()

    val requests = listOf(
        requestGet("http://www.google.com/"),
        requestGet("http://www.yahoo.com/"),
        requestGet("http://www.apache.org/"),
        requestGet("http://www.apple.com/"),
    )

    @Test
    fun `execute multiple request asynchronously`() {
        val executor = Executors.newFixedThreadPool(2)
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
        executor.shutdown()
    }

    // FIXME: status code: 400 Bad Request 가 발생한다.
    @Test
    fun `execute multiple request in multi threading`() {
        val async = Async.newInstance()
        val counter = atomic(0)

        MultithreadingTester()
            .numThreads(2)
            .roundsPerThread(2)
            .add {
                val index = counter.getAndIncrement() % requests.size
                val request = requests[index]
                log.debug { "Reqeust $request" }

                val content = async.execute(request).get()
                log.debug { "Content type=${content.type} from $request" }
            }
            .run()
    }

    // FIXME: status code: 400 Bad Request 가 발생한다.
    @Test
    fun `execute multiple request in virtual threads`() {
        val async = Async.newInstance().use(VirtualThreadExecutor)
        val counter = atomic(0)

        VirtualthreadTester()
            .numThreads(2)
            .roundsPerThread(2)
            .add {
                val index = counter.getAndIncrement() % requests.size
                val request = requests[index]
                log.debug { "Reqeust $request" }

                val content = async.execute(request).get()
                log.debug { "Content type=${content.type} from $request" }
            }
            .run()
    }

    // FIXME: status code: 400 Bad Request 가 발생한다.
    @Test
    fun `execute multiple request in multi job`() = runSuspendIO {
        val async = Async.newInstance().use(Dispatchers.IO.asExecutor())
        val counter = atomic(0)

        MultijobTester()
            .numThreads(2)
            .roundsPerJob(2)
            .add {
                val index = counter.getAndIncrement() % requests.size
                val request = requests[index]
                log.debug { "Reqeust $request" }

                val content = async.execute(request).coAwait()
                log.debug { "Content type=${content.type} from $request" }
            }
            .run()
    }
}
