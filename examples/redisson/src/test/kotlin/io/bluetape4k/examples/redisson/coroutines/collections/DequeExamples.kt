package io.bluetape4k.examples.redisson.coroutines.collections

import io.bluetape4k.collections.toList
import io.bluetape4k.coroutines.support.suspendAwait
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContainSame
import org.junit.jupiter.api.Test

class DequeExamples: io.bluetape4k.examples.redisson.coroutines.AbstractRedissonCoroutineTest() {

    companion object: KLoggingChannel()

    @Test
    fun `deque 사용`() = runTest {
        val deque = redisson.getDeque<String>(randomName())
        deque.clear()

        // push 는 addFirst 와 같다
        // add 는 addLast 와 같다
        deque.addLastAsync("1").suspendAwait()
        deque.addLastAsync("2").suspendAwait()
        deque.addLastAsync("3").suspendAwait()
        deque.addLastAsync("4").suspendAwait()

        deque.containsAsync("1").suspendAwait().shouldBeTrue()

        // 첫번째 요소를 조회한다. (제거하지 않는다)
        deque.peekAsync().suspendAwait() shouldBeEqualTo "1"

        // 첫번째 요소를 가져오고, queue에서는 제거한다 (첫번째 요소가 없으면 들어올 때까지 대기힌다.)
        deque.popAsync().suspendAwait() shouldBeEqualTo "1"

        // 첫 번째 요소를 조회한다 (제거하지 않는다) 단 queue에 요소가 없으면 예외를 일으킨다
        deque.element() shouldBeEqualTo "2"

        deque.removeAllAsync(listOf("2", "3")).suspendAwait().shouldBeTrue()

        deque.addAllAsync(listOf("10", "11", "12")).suspendAwait().shouldBeTrue()

        deque.deleteAsync().suspendAwait()
    }

    @Test
    fun `deque in multi-job`() = runTest {
        val counter = atomic(0)
        val deque = redisson.getDeque<Int>(randomName())
        deque.clear()

        SuspendedJobTester()
            .numThreads(16)
            .roundsPerJob(16 * 4)
            .add {
                deque.addLastAsync(counter.incrementAndGet()).suspendAwait()
            }
            .run()

        counter.value shouldBeEqualTo 16 * 4
        deque.sizeAsync().suspendAwait() shouldBeEqualTo counter.value

        // 순서는 틀립니다.
        deque.iterator().toList() shouldContainSame List(16 * 4) { it + 1 }
        deque.deleteAsync().suspendAwait().shouldBeTrue()
    }
}
