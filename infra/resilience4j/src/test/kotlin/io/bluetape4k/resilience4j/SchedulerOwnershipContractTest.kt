package io.bluetape4k.resilience4j

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.resilience4j.retry.completableFuture
import io.bluetape4k.resilience4j.retry.completableFutureFunction
import io.bluetape4k.resilience4j.retry.completionStage
import io.bluetape4k.resilience4j.retry.withRetry
import io.bluetape4k.resilience4j.timelimiter.completableFuture
import io.bluetape4k.resilience4j.timelimiter.completionStage
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import io.github.resilience4j.timelimiter.TimeLimiter
import io.github.resilience4j.timelimiter.TimeLimiterConfig
import org.junit.jupiter.api.Test
import java.io.IOException
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

class SchedulerOwnershipContractTest {

    @Test
    fun `TimeLimiter caller scheduler remains open after successful repeated calls`() {
        val scheduler = newScheduler()
        try {
            val function = timeLimiter().completableFuture(scheduler) { input: Int ->
                CompletableFuture.completedFuture(input * 2)
            }

            function(21).get() shouldBeEqualTo 42
            scheduler.isShutdown.shouldBeFalse()
            function(22).get() shouldBeEqualTo 44
            scheduler.isShutdown.shouldBeFalse()
        } finally {
            scheduler.shutdownNow()
        }
    }

    @Test
    fun `TimeLimiter caller scheduler remains open after timeout`() {
        val scheduler = newScheduler()
        try {
            val function = timeLimiter().completableFuture(scheduler) { _: Int ->
                CompletableFuture<Int>()
            }

            assertFailsWith<ExecutionException> {
                function(1).get(1, TimeUnit.SECONDS)
            }
            scheduler.isShutdown.shouldBeFalse()
        } finally {
            scheduler.shutdownNow()
        }
    }

    @Test
    fun `Retry caller scheduler remains open after failed completion`() {
        val scheduler = newScheduler()
        try {
            val function = retry().completableFuture<Int, Int>(scheduler) {
                CompletableFuture.failedFuture(IOException("failure"))
            }

            assertFailsWith<ExecutionException> {
                function(1).get(1, TimeUnit.SECONDS)
            }
            scheduler.isShutdown.shouldBeFalse()
        } finally {
            scheduler.shutdownNow()
        }
    }

    @Test
    fun `Retry completionStage caller scheduler remains open after success`() {
        val scheduler = newScheduler()
        try {
            val supplier = retry().completionStage(scheduler) {
                CompletableFuture.completedFuture(42)
            }

            supplier().toCompletableFuture().get() shouldBeEqualTo 42
            scheduler.isShutdown.shouldBeFalse()
        } finally {
            scheduler.shutdownNow()
        }
    }

    @Test
    fun `TimeLimiter completionStage caller scheduler remains open after success`() {
        val scheduler = newScheduler()
        try {
            val supplier = timeLimiter().completionStage(scheduler) {
                CompletableFuture.completedFuture(42)
            }

            supplier() shouldBeEqualTo 42
            scheduler.isShutdown.shouldBeFalse()
        } finally {
            scheduler.shutdownNow()
        }
    }

    @Test
    fun `Retry completableFutureFunction caller scheduler remains open after success`() {
        val scheduler = newScheduler()
        try {
            val function = retry().completableFutureFunction<Int, Int>(scheduler) { input ->
                CompletableFuture.completedFuture(input * 2)
            }

            function(21).get() shouldBeEqualTo 42
            scheduler.isShutdown.shouldBeFalse()
        } finally {
            scheduler.shutdownNow()
        }
    }

    @Test
    fun `withRetry caller scheduler remains open after success`() {
        val scheduler = newScheduler()
        try {
            val function = withRetry<Int, Int>(retry(), scheduler) { input ->
                CompletableFuture.completedFuture(input * 2)
            }

            function(21).get() shouldBeEqualTo 42
            scheduler.isShutdown.shouldBeFalse()
        } finally {
            scheduler.shutdownNow()
        }
    }

    @Test
    fun `Retry and TimeLimiter can share a caller scheduler`() {
        val scheduler = newScheduler()
        try {
            val retried = retry().completableFuture<Int, Int>(scheduler) {
                CompletableFuture.completedFuture(21)
            }
            val timed = timeLimiter().completableFuture<Int, CompletableFuture<Int>>(scheduler) {
                CompletableFuture.completedFuture(42)
            }

            retried(1).get() shouldBeEqualTo 21
            timed(1).get() shouldBeEqualTo 42
            scheduler.isShutdown.shouldBeFalse()
        } finally {
            scheduler.shutdownNow()
        }
    }

    @Test
    fun `TimeLimiter default scheduler supports repeated decorated calls`() {
        val function = timeLimiter().completableFuture { input: Int ->
            CompletableFuture.completedFuture(input * 2)
        }

        function(21).get() shouldBeEqualTo 42
        function(22).get() shouldBeEqualTo 44
    }

    @Test
    fun `TimeLimiter default schedulers terminate after success and timeout`() {
        withTrackedDefaultSchedulers { schedulers ->
            val successful = timeLimiter().completableFuture { input: Int ->
                CompletableFuture.completedFuture(input * 2)
            }
            successful(21).get() shouldBeEqualTo 42

            val timedOut = timeLimiter().completableFuture { _: Int ->
                CompletableFuture<Int>()
            }
            assertFailsWith<ExecutionException> { timedOut(1).get(1, TimeUnit.SECONDS) }

            schedulers.size shouldBeEqualTo 2
        }
    }

    @Test
    fun `Retry default schedulers terminate after success and failure`() {
        withTrackedDefaultSchedulers { schedulers ->
            val successful = retry().completableFuture { input: Int ->
                CompletableFuture.completedFuture(input * 2)
            }
            successful(21).get() shouldBeEqualTo 42

            val failed = retry().completableFuture<Int, Int> {
                CompletableFuture.failedFuture(IOException("failure"))
            }
            assertFailsWith<ExecutionException> { failed(1).get(1, TimeUnit.SECONDS) }

            schedulers.size shouldBeEqualTo 2
        }
    }

    @Test
    fun `decorator builder preserves caller scheduler ownership`() {
        val scheduler = newScheduler()
        try {
            val decorated = decorateCompletableFutureFunction<Int, Int> {
                CompletableFuture.completedFuture(it * 2)
            }
                .withRetry(retry(), scheduler)
                .decorate()

            decorated(21).get() shouldBeEqualTo 42
            scheduler.isShutdown.shouldBeFalse()
        } finally {
            scheduler.shutdownNow()
        }
    }

    private fun newScheduler(): ScheduledExecutorService =
        Executors.newScheduledThreadPool(2)

    private fun withTrackedDefaultSchedulers(
        block: (List<ScheduledThreadPoolExecutor>) -> Unit,
    ) {
        val schedulers = mutableListOf<ScheduledThreadPoolExecutor>()
        SchedulerHandle.withDefaultSchedulerFactory(
            factory = {
                ScheduledThreadPoolExecutor(1).also(schedulers::add)
            },
            block = {
                try {
                    block(schedulers)
                } finally {
                    schedulers.forEach { scheduler ->
                        scheduler.isShutdown.shouldBeTrue()
                        scheduler.awaitTermination(1, TimeUnit.SECONDS).shouldBeTrue()
                    }
                }
            },
        )
    }

    private fun retry(): Retry = Retry.of(
        "scheduler-${System.nanoTime()}",
        RetryConfig.custom<Any?>()
            .maxAttempts(2)
            .waitDuration(Duration.ofMillis(10))
            .build()
    )

    private fun timeLimiter(): TimeLimiter = TimeLimiter.of(
        "scheduler-${System.nanoTime()}",
        TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofMillis(50))
            .build()
    )
}
