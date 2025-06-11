package io.bluetape4k.examples.redisson.coroutines.collections

import io.bluetape4k.coroutines.support.suspendAwait
import io.bluetape4k.examples.redisson.coroutines.AbstractRedissonCoroutineTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import org.redisson.api.RBoundedBlockingQueue
import java.util.concurrent.TimeUnit


/**
 * [RBoundedBlockingQueue] 예제
 *
 * 참고: [RBoundedBlockingQueue](https://github.com/redisson/redisson/wiki/7.-distributed-collections/#711-bounded-blocking-queue)
 */
class BoundedBlockingQueueExamples: AbstractRedissonCoroutineTest() {

    companion object: KLoggingChannel() {
        private const val ITEM_SIZE = 10
    }

    @Test
    fun `크기 제한이 있는 Queue 사용`() = runTest {
        val queue = redisson.getBoundedBlockingQueue<Int>(randomName())
        queue.trySetCapacity(ITEM_SIZE).shouldBeTrue()

        repeat(ITEM_SIZE) {
            queue.offerAsync(it + 1).suspendAwait().shouldBeTrue()
        }

        // 크기 제한으로 추가 못한다
        queue.offerAsync(ITEM_SIZE + 1).suspendAwait().shouldBeFalse()

        val job = scope.launch {
            delay(1000)
            queue.takeAsync().suspendAwait() shouldBeEqualTo 1
        }
        yield()

        // 요소 [6]을 추가합니다. (10초간 시도)
        queue.offerAsync(ITEM_SIZE + 1, 10, TimeUnit.SECONDS).suspendAwait().shouldBeTrue()

        job.join()

        // 요소 [1]은 job 내부에서 가겨갔기 때문에 삭제되었습니다.
        queue.containsAsync(1).suspendAwait().shouldBeFalse()

        queue.deleteAsync().suspendAwait()
    }
}
