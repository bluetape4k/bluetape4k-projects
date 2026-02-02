package io.bluetape4k.concurrent.virtualthread.examples

import io.bluetape4k.collections.eclipse.fastList
import io.bluetape4k.concurrent.FutureUtils
import io.bluetape4k.concurrent.asCompletableFuture
import io.bluetape4k.concurrent.virtualthread.AbstractVirtualThreadTest
import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.trace
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * ### Rule 4
 *
 * 동시성을 제어할 때에는 FixedThreadPool 을 사용하지 말고, [Semaphore]를 사용하세요
 */
class Rule4UseSemaphoreInsteadOfFixedThreadPool: AbstractVirtualThreadTest() {

    companion object {
        private val log = KotlinLogging.logger { }
    }

    @Nested
    inner class DoNot {
        private val executor = Executors.newFixedThreadPool(8)

        fun useFixedExecutorServiceToLimitConcurrency(): String {
            val future = executor.submit<String> { sharedResource() }
            return future.get()
        }
    }

    @Nested
    inner class Do {
        private val semaphore = Semaphore(2)
        private val taskSize = 100

        fun useSemaphoreToLimitConcurrency(): String {
            if (semaphore.tryAcquire(10, TimeUnit.SECONDS)) {
                try {
                    val result = sharedResource()
                    return result
                } finally {
                    semaphore.release()
                }
            }
            throw TimeoutException("Timeout to acquire semaphore")
        }

        @Test
        fun `추천 - Semaphore를 사용하여 동시성 제어하기`() {
            val results = ConcurrentLinkedQueue<String>()
            Executors.newVirtualThreadPerTaskExecutor().use { executor ->
                val futures = fastList(taskSize) { index ->
                    executor.submit<String> {
                        log.trace { "Start run task[$index]" }
                        val result = useSemaphoreToLimitConcurrency()
                        log.trace { "Finish run task[$index]" }
                        results.add(result)
                        result
                    }.asCompletableFuture()
                }
                FutureUtils.allAsList(futures).get()
                results.size shouldBeEqualTo taskSize
            }
        }
    }

    private fun sharedResource(): String {
        sleep(100)
        return "result"
    }
}
