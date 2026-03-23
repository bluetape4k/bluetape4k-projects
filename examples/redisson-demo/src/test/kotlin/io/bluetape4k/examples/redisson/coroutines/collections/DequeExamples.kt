package io.bluetape4k.examples.redisson.coroutines.collections

import io.bluetape4k.collections.toList
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.future.await
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContainSame
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

class DequeExamples: io.bluetape4k.examples.redisson.coroutines.AbstractRedissonCoroutineTest() {

    companion object: KLoggingChannel()

    @Test
    fun `deque 사용`() = runTest {
        val deque = redisson.getDeque<String>(randomName())
        deque.clear()

        // push 는 addFirst 와 같다
        // add 는 addLast 와 같다
        deque.addLastAsync("1").await()
        deque.addLastAsync("2").await()
        deque.addLastAsync("3").await()
        deque.addLastAsync("4").await()

        deque.containsAsync("1").await().shouldBeTrue()

        // 첫번째 요소를 조회한다. (제거하지 않는다)
        deque.peekAsync().await() shouldBeEqualTo "1"

        // 첫번째 요소를 가져오고, queue에서는 제거한다 (첫번째 요소가 없으면 들어올 때까지 대기힌다.)
        deque.popAsync().await() shouldBeEqualTo "1"

        // 첫 번째 요소를 조회한다 (제거하지 않는다) 단 queue에 요소가 없으면 예외를 일으킨다
        deque.element() shouldBeEqualTo "2"

        deque.removeAllAsync(listOf("2", "3")).await().shouldBeTrue()

        deque.addAllAsync(listOf("10", "11", "12")).await().shouldBeTrue()

        deque.deleteAsync().await()
    }

    @Test
    fun `deque in multi-job`() = runTest {
        val counter = AtomicInteger(0)
        val deque = redisson.getDeque<Int>(randomName())
        deque.clear()

        SuspendedJobTester()
            .workers(16)
            .rounds(16 * 4)
            .add {
                deque.addLastAsync(counter.incrementAndGet()).await()
            }
            .run()

        counter.get() shouldBeEqualTo 16 * 4
        deque.sizeAsync().await() shouldBeEqualTo counter.get()

        // 순서는 틀립니다.
        deque.iterator().toList() shouldContainSame List(16 * 4) { it + 1 }
        deque.deleteAsync().await().shouldBeTrue()
    }
}
