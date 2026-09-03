package io.bluetape4k.concurrent

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class ExecutorSupportTest {

    @Test
    fun `DirectExecutor 는 동기 실행한다`() {
        val flag = AtomicInteger(0)
        DirectExecutor.execute { flag.incrementAndGet() }
        flag.get() shouldBeEqualTo 1
    }

    @Test
    fun `withWorkStealingPool 단일 태스크`() {
        val result = withWorkStealingPool(parallelism = 2) { 42 }
            .get()
        result shouldBeEqualTo 42
    }

    @Test
    fun `withWorkStealingPool 다중 태스크`() {
        val tasks = listOf({ 1 }, { 2 }, { 3 })

        val results = withWorkStealingPool(parallelism = 2, tasks = tasks).get()
        results shouldBeEqualTo listOf(1, 2, 3)
    }

    @Test
    fun `withWorkStealingPool 다중 태스크는 호출자를 블로킹하지 않는다`() {
        val taskStarted = CountDownLatch(1)
        val releaseTask = CountDownLatch(1)
        val invocation = CompletableFuture.supplyAsync {
            withWorkStealingPool(
                parallelism = 1,
                tasks = listOf {
                    taskStarted.countDown()
                    releaseTask.await()
                    42
                },
            )
        }
        var result: CompletableFuture<List<Int>>? = null

        try {
            taskStarted.await(1, TimeUnit.SECONDS).shouldBeTrue()
            invocation.isDone.shouldBeTrue()

            result = invocation.get(1, TimeUnit.SECONDS)
            result?.isDone.shouldBeFalse()
        } finally {
            releaseTask.countDown()
            if (result == null) {
                result = invocation.get(1, TimeUnit.SECONDS)
            }
            result?.get(1, TimeUnit.SECONDS)
        }
    }

    @Test
    fun `withWorkStealingPool 반환 future 취소가 실행 중 태스크에 전파된다`() {
        val taskStarted = CountDownLatch(1)
        val taskInterrupted = CountDownLatch(1)
        val releaseTask = CountDownLatch(1)
        val invocation = CompletableFuture.supplyAsync {
            withWorkStealingPool(
                parallelism = 1,
                tasks = listOf {
                    taskStarted.countDown()
                    try {
                        releaseTask.await()
                    } catch (e: InterruptedException) {
                        taskInterrupted.countDown()
                        throw e
                    }
                    42
                },
            )
        }
        var result: CompletableFuture<List<Int>>? = null

        try {
            taskStarted.await(1, TimeUnit.SECONDS).shouldBeTrue()
            invocation.isDone.shouldBeTrue()

            result = invocation.get(1, TimeUnit.SECONDS)
            result?.cancel(true).shouldBeTrue()
            taskInterrupted.await(1, TimeUnit.SECONDS).shouldBeTrue()
            result?.isCancelled.shouldBeTrue()
        } finally {
            releaseTask.countDown()
            if (!invocation.isDone) {
                invocation.get(1, TimeUnit.SECONDS)
            }
        }
    }

    @Test
    fun `withWorkStealingPool 다중 태스크 예외를 전파한다`() {
        val expected = IllegalStateException("task failed")
        val future = withWorkStealingPool(
            parallelism = 2,
            tasks = listOf<() -> Int>(
                { 1 },
                { throw expected },
                { 3 },
            ),
        )

        assertFailsWith<ExecutionException> { future.get() }
    }

    @Test
    fun `withWorkStealingPool 은 parallelism 0이면 예외`() {
        assertFailsWith<IllegalArgumentException> {
            withWorkStealingPool(parallelism = 0) { 1 }.get()
        }
    }
}
