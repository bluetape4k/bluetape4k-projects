package io.bluetape4k.assertions

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import org.junit.jupiter.api.Test

class SoftlyVirtualThreadTest {

    @Test
    fun `assertSoftly is safe under 256 concurrent virtual threads`() {
        val threadCount = 256
        val executor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory())
        val errors = ConcurrentHashMap.newKeySet<Throwable>()

        val futures: List<Future<*>> = (1..threadCount).map { i ->
            executor.submit {
                try {
                    assertSoftly {
                        add { i shouldBeEqualTo i }
                        add { (i * 2) shouldBeEqualTo (i * 2) }
                    }
                } catch (e: Throwable) {
                    errors.add(e)
                }
            }
        }

        futures.forEach { it.get(10, TimeUnit.SECONDS) }
        executor.shutdown()
        executor.awaitTermination(30, TimeUnit.SECONDS)

        assert(errors.isEmpty()) {
            "Virtual thread safety violations: ${errors.joinToString { it.message ?: it::class.simpleName ?: "unknown" }}"
        }
    }

    @Test
    fun `each virtual thread has independent SoftAssertionScope`() {
        val threadCount = 64
        val executor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory())
        val failureErrors = ConcurrentHashMap.newKeySet<Throwable>()

        val futures: List<Future<*>> = (1..threadCount).map { i ->
            executor.submit {
                try {
                    assertSoftly {
                        add { i shouldBeEqualTo i }
                    }
                } catch (e: Throwable) {
                    failureErrors.add(e)
                }
            }
        }

        futures.forEach { it.get(10, TimeUnit.SECONDS) }
        executor.shutdown()
        executor.awaitTermination(30, TimeUnit.SECONDS)

        assert(failureErrors.isEmpty()) {
            "Expected all threads to succeed independently, but got: $failureErrors"
        }
    }
}
