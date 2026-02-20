package io.bluetape4k.concurrent

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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
    fun `withWorkStealingPool 은 parallelism 0이면 예외`() {
        assertThrows<AssertionError> {
            withWorkStealingPool(parallelism = 0) { 1 }.get()
        }
    }
}
