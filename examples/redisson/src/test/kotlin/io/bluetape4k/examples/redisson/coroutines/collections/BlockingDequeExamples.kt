package io.bluetape4k.examples.redisson.coroutines.collections

import io.bluetape4k.coroutines.support.suspendAwait
import io.bluetape4k.examples.redisson.coroutines.AbstractRedissonCoroutineTest
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import org.redisson.api.RBlockingDeque
import java.util.concurrent.TimeUnit

/**
 * Redisson [RBlockingDeque] 예제
 *
 * 참고: [RBlockingDeque](https://github.com/redisson/redisson/wiki/7.-distributed-collections/#712-blocking-deque)
 */
class BlockingDequeExamples: AbstractRedissonCoroutineTest() {

    companion object: KLoggingChannel()

    @Test
    fun `blocking deque 를 사용`() = runSuspendIO {
        val deque: RBlockingDeque<String> = redisson.getBlockingDeque(randomName())
        deque.clear()

        deque.putLastAsync("1").suspendAwait()
        deque.putLastAsync("2").suspendAwait()
        deque.putLastAsync("3").suspendAwait()
        deque.putLastAsync("4").suspendAwait()

        deque.containsAsync("1").suspendAwait().shouldBeTrue()

        // 첫번째 요소를 조회한다. (제거하지 않는다)
        deque.peekFirstAsync().suspendAwait() shouldBeEqualTo "1"
        // 첫번째 요소를 가져오고, queue에서는 제거한다 (첫번째 요소가 없으면 들어올 때까지 대기힌다.)
        deque.pollFirstAsync().suspendAwait() shouldBeEqualTo "1"

        // 첫 번째 요소를 조회한다 (제거하지 않는다) 단 queue에 요소가 없으면 예외를 일으킨다
        deque.element() shouldBeEqualTo "2"

        deque.removeAllAsync(listOf("2", "3")).suspendAwait().shouldBeTrue()
        deque.addAllAsync(listOf("10", "11", "12")).suspendAwait().shouldBeTrue()
    }

    @Test
    fun `deque operation in coroutines`() = runSuspendIO {
        val deque = redisson.getBlockingDeque<Int>(randomName())
        val deque2 = redisson.getBlockingDeque<Int>(deque.name + ":deadqueue")
        deque.clear()
        deque2.clear()

        val retrieveJob = scope.launch {
            deque.pollAsync(10, TimeUnit.SECONDS).suspendAwait() shouldBeEqualTo 1
            yield()

            deque.takeAsync().suspendAwait() shouldBeEqualTo 2
            yield()

            // deque의 마지막 요소를 deque2의 첫번째 요소로 이동
            deque.pollLastAndOfferFirstToAsync(deque2.name, 5, TimeUnit.SECONDS).suspendAwait() shouldBeEqualTo 3
            yield()

            // deque의 마지막 요소를 deque2의 첫번째 요소로 이동
            deque.takeLastAndOfferFirstToAsync(deque2.name).suspendAwait() shouldBeEqualTo 4
            yield()
        }

        deque.putLastAsync(1).suspendAwait()
        yield()

        deque.putLastAsync(2).suspendAwait()
        yield()

        deque.putLastAsync(3).suspendAwait()
        delay(10)

        deque2.takeFirstAsync().suspendAwait() shouldBeEqualTo 3

        deque.putLastAsync(4).suspendAwait()
        delay(10)

        deque2.takeFirstAsync().suspendAwait() shouldBeEqualTo 4

        retrieveJob.join()
    }
}
