package io.bluetape4k.examples.redisson.coroutines.collections

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.coroutines.support.awaitUntil
import io.bluetape4k.examples.redisson.coroutines.AbstractRedissonCoroutineTest
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds

class QueueExamples: AbstractRedissonCoroutineTest() {

    companion object: KLoggingChannel()

    @Test
    fun `queue usage`() = runSuspendIO {
        val queue = redisson.getQueue<Int>(randomName())
        val queue2 = redisson.getQueue<Int>(randomName())

        queue.addAllAsync(listOf(1, 2, 3, 4)).awaitUntil().shouldBeTrue()
        queue.containsAsync(3).awaitUntil().shouldBeTrue()

        // 첫 번째 요소를 조회한다
        queue.peekAsync().awaitUntil() shouldBeEqualTo 1

        val job = scope.launch {
            log.debug { "최대 요소 5개를 가져온다" }
            while (queue.sizeAsync().awaitUntil() < 5) {
                delay(10.milliseconds)
            }
            val items = queue.pollAsync(5).awaitUntil()
            log.debug { "최대 요소 5개 = $items" }
            items shouldBeEqualTo listOf(1, 2, 3, 4, 5)

            log.debug { "[6,7] 이 새로 들어오는데, 7 을 queue2 로 이동시킨다." }
            queue.pollLastAndOfferFirstToAsync(queue2.name).awaitUntil() shouldBeEqualTo 7
            while (!queue2.containsAsync(7).awaitUntil()) {
                delay(10.milliseconds)
            }
        }
        // 새롭게 요소 [5,6,7]을 추가한다
        queue.addAllAsync(listOf(5, 6, 7)).awaitUntil().shouldBeTrue()
        delay(10.milliseconds)

        job.join()
        delay(10.milliseconds)

        // queue2에 [7]이 새로 들어왔다
        queue2.peekAsync().awaitUntil() shouldBeEqualTo 7

        // [6,7] 에서 7이 이동해서 6만 남았다
        queue.sizeAsync().awaitUntil() shouldBeEqualTo 1

        queue.deleteAsync().awaitUntil()
        queue2.deleteAsync().awaitUntil()
    }
}
