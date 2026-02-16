package io.bluetape4k.examples.redisson.coroutines.collections

import io.bluetape4k.coroutines.support.awaitSuspending
import io.bluetape4k.examples.redisson.coroutines.AbstractRedissonCoroutineTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.redisson.RedissonCodecs
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

/**
 * Priority queue examples
 *
 * 참고: [Priority Queue](https://github.com/redisson/redisson/wiki/7.-distributed-collections#716-priority-queue)
 */
class PriorityQueueExamples: AbstractRedissonCoroutineTest() {

    companion object: KLoggingChannel()

    data class Item(
        val key: String,
        val value: Int,
    ): Comparable<Item>, java.io.Serializable {
        override fun compareTo(other: Item): Int = key.compareTo(other.key)
    }

    @Test
    fun `use PriorityQueue`() = runTest {
        val queueName = randomName()
        val queue = redisson.getPriorityQueue<Item>(queueName, RedissonCodecs.LZ4Fory)

        queue.add(Item("b", 1))
        queue.add(Item("c", 2))
        queue.add(Item("a", 3))

        queue.addAll(listOf(Item("x", 11), Item("y", 22), Item("z", 33)))

        queue.count() shouldBeEqualTo 6

        // 첫번째 요소 조회
        queue.peekAsync().awaitSuspending() shouldBeEqualTo Item("a", 3)
        // 첫번째 요소 가져오기
        queue.pollAsync().awaitSuspending() shouldBeEqualTo Item("a", 3)

        queue.deleteAsync().awaitSuspending()
    }
}
