package io.bluetape4k.examples.redisson.coroutines.collections

import io.bluetape4k.examples.redisson.coroutines.AbstractRedissonCoroutineTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class QueueExamples: AbstractRedissonCoroutineTest() {

    companion object: KLoggingChannel()

    @Test
    fun `queue usage`() = runTest {
        val queue = redisson.getQueue<Int>(randomName())
        val queue2 = redisson.getQueue<Int>(randomName())

        queue.addAllAsync(listOf(1, 2, 3, 4)).await().shouldBeTrue()
        queue.containsAsync(3).await().shouldBeTrue()

        // 첫 번째 요소를 조회한다
        queue.peekAsync().await() shouldBeEqualTo 1

        val job = scope.launch {
            log.debug { "최대 요소 5개를 가져온다" }
            while (queue.sizeAsync().await() < 5) {
                delay(10)
            }
            val items = queue.pollAsync(5).await()
            log.debug { "최대 요소 5개 = $items" }
            items shouldBeEqualTo listOf(1, 2, 3, 4, 5)

            log.debug { "[6,7] 이 새로 들어오는데, 7 을 queue2 로 이동시킨다." }
            queue.pollLastAndOfferFirstToAsync(queue2.name).await() shouldBeEqualTo 7
            while (!queue2.containsAsync(7).await()) {
                delay(10)
            }
        }
        // 새롭게 요소 [5,6,7]을 추가한다
        queue.addAllAsync(listOf(5, 6, 7)).await().shouldBeTrue()
        delay(10)

        job.join()
        delay(10)

        // queue2에 [7]이 새로 들어왔다
        queue2.peekAsync().await() shouldBeEqualTo 7

        // [6,7] 에서 7이 이동해서 6만 남았다
        queue.sizeAsync().await() shouldBeEqualTo 1

        queue.deleteAsync().await()
        queue2.deleteAsync().await()
    }
}
