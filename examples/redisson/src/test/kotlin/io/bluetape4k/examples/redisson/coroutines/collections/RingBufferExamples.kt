package io.bluetape4k.examples.redisson.coroutines.collections

import io.bluetape4k.examples.redisson.coroutines.AbstractRedissonCoroutineTest
import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.redisson.coroutines.coAwait
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

/**
 * Ring buffer examples
 *
 * 참고: [Ring Buffer](https://github.com/redisson/redisson/wiki/7.-distributed-collections#721-ring-buffer)
 */
class RingBufferExamples: AbstractRedissonCoroutineTest() {

    companion object: KLoggingChannel()

    @Test
    fun `use Ring Buffer`() = runSuspendTest {
        val buffer = redisson.getRingBuffer<Int>(randomName())
        // 버퍼 용량을 미리 설정해주어야 합니다.
        buffer.trySetCapacity(4)
        buffer.capacityAsync().coAwait() shouldBeEqualTo 4

        buffer.addAllAsync(listOf(1, 2, 3, 4)).coAwait().shouldBeTrue()

        buffer.remainingCapacityAsync().coAwait() shouldBeEqualTo 0

        // buffer contains 1,2,3,4
        buffer.addAllAsync(listOf(5, 6)).coAwait().shouldBeTrue()

        // buffer contains 3,4,5,6
        buffer.pollAsync(2).coAwait() shouldBeEqualTo listOf(3, 4)

        // buffer contains 5, 6
        buffer.deleteAsync().coAwait()
    }
}
