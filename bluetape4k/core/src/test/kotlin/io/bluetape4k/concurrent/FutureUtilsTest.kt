package io.bluetape4k.concurrent

import io.bluetape4k.collections.eclipse.fastList
import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.internal.assertFailsWith
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutionException

class FutureUtilsTest {

    companion object: KLogging()

    private val success: CompletableFuture<Int> = completableFutureOf(1)
    private val failed: CompletableFuture<Int> = failedCompletableFutureOf(IllegalArgumentException())
    private val emptyFutures: List<CompletableFuture<Int>> = emptyList()

    @Test
    fun `allAsList - 모든 성공 future를 리스트로 반환한다`() {
        val futures = (1..5).map { completableFutureOf(it) }
        val result = FutureUtils.allAsList(futures).get()
        result shouldBeEqualTo listOf(1, 2, 3, 4, 5)
    }

    @Test
    fun `allAsList - 하나라도 실패하면 예외가 발생한다`() {
        val futures = listOf(
            completableFutureOf(1),
            completableFutureOf(2),
            failedCompletableFutureOf<Int>(IllegalStateException("Boom!"))
        )
        assertFailsWith<ExecutionException> {
            FutureUtils.allAsList(futures).get()
        }
    }

    @Test
    fun `allAsList - 빈 리스트는 빈 결과를 반환한다`() {
        val result = FutureUtils.allAsList(emptyFutures).get()
        result shouldBeEqualTo emptyList()
    }

    @Test
    fun `successfulAsList - 성공한 future만 반환한다`() {
        val futures = listOf(
            completableFutureOf(1),
            failedCompletableFutureOf(IllegalStateException("Boom!")),
            completableFutureOf(3),
        )
        val result = FutureUtils.successfulAsList(futures).get()
        result shouldBeEqualTo listOf(1, 3)
    }

    @Test
    fun `successfulAsList - 모두 성공하면 전부 반환한다`() {
        val futures = (1..5).map { completableFutureOf(it) }
        val result = FutureUtils.successfulAsList(futures).get()
        result shouldBeEqualTo listOf(1, 2, 3, 4, 5)
    }

    @Test
    fun `successfulAsList - 모두 실패하면 빈 리스트를 반환한다`() {
        val futures = (1..3).map { failedCompletableFutureOf<Int>(RuntimeException("error $it")) }
        val result = FutureUtils.successfulAsList(futures).get()
        result shouldBeEqualTo emptyList()
    }

    @Test
    fun `CompletableFuture Task들 중 첫번째 완료된 작업을 반환하고, 나머지는 캔슬한다`() {
        val completedTasks = CopyOnWriteArrayList<Int>()

        val futures = fastList(10) {
            futureOf {
                log.debug { "Task[$it] is starting..." }
                Thread.sleep(100L * (it + 1))
                it.apply {
                    log.debug { "result=$it" }
                }
            }.whenComplete { result, error ->
                log.debug { "Task[$it] is completed. result=$result, error=$error" }
                if (error == null && completedTasks.isEmpty()) {
                    completedTasks.add(result)
                }
            }
        }
        val result = FutureUtils.firstCompleted(futures)
        result.get() shouldBeEqualTo 0
        completedTasks.size shouldBeEqualTo 1   // 1개만 완료되어야 하지만, 거의 동시에 완료되는 경우가 있기 때문에
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
        FutureUtils.transform(futures) { it + 1 }.get() shouldBeEqualTo (2..11).toFastList()
    }

    @Test
    fun `transform failed futures`() {
        val futures = (1..10).map { completableFutureOf(it) } + failed
        assertFailsWith<ExecutionException> {
            FutureUtils.transform(futures) { it + 1 }.get()
        }.cause shouldBeInstanceOf IllegalArgumentException::class
    }
}
