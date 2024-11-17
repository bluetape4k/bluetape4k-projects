package io.bluetape4k.concurrent

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.internal.assertFailsWith
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException

class FutureUtilsTest {

    companion object: KLogging()

    private val success: CompletableFuture<Int> = completableFutureOf(1)
    private val failed: CompletableFuture<Int> = failedCompletableFutureOf(IllegalArgumentException())
    private val emptyFutures: List<CompletableFuture<Int>> = emptyList()

    // TODO : firstCompleted, allAsList, successfulAsList 에 대한 Test case 추가

    @Test
    fun `get firstCompleted`() {
        val futures = List(10) {
            futureOf {
                Thread.sleep(10L * it + 200)
                it.apply {
                    log.debug { "result=$it" }
                }
            }
        }
        val result = FutureUtils.firstCompleted(futures)
        result.get() shouldBeEqualTo 0
    }

    @Test
    fun `fold all success futures`() {
        val futures = (1..10).map { completableFutureOf(it) }
        FutureUtils.fold(futures, 0) { acc, i -> acc + i }.get() shouldBeEqualTo (1..10).sum()
    }

    @Test
    fun `fold futures contains failed future`() {
        val futures = (1..10).map { completableFutureOf(it) } + failed

        assertFailsWith<ExecutionException> {
            FutureUtils.fold(futures, 0) { acc, i -> acc + i }.get()
        }.cause shouldBeInstanceOf IllegalArgumentException::class
    }

    @Test
    fun `reduce all success futures`() {
        val futures = (1..10).map { completableFutureOf(it) }
        FutureUtils.reduce(futures) { acc, i -> acc + i }.get() shouldBeEqualTo (1..10).sum()
    }

    @Test
    fun `reduce futures contains failed future`() {
        val futures = (1..10).map { completableFutureOf(it) } + failed
        assertFailsWith<ExecutionException> {
            FutureUtils.reduce(futures) { acc, i -> acc + i }.get()
        }.cause shouldBeInstanceOf IllegalArgumentException::class
    }

    @Test
    fun `reduce empty future list`() {
        assertFailsWith<UnsupportedOperationException> {
            FutureUtils.reduce(emptyFutures) { acc, i -> acc + i }.get()
        }
    }

    @Test
    fun `transform success futures`() {
        val futures = (1..10).map { completableFutureOf(it) }
        FutureUtils.transform(futures) { it + 1 }.get() shouldBeEqualTo (2..11).toList()
    }

    @Test
    fun `transform failed futures`() {
        val futures = (1..10).map { completableFutureOf(it) } + failed
        assertFailsWith<ExecutionException> {
            FutureUtils.transform(futures) { it + 1 }.get()
        }.cause shouldBeInstanceOf IllegalArgumentException::class
    }
}
