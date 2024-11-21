package io.bluetape4k.examples.redisson.coroutines.collections

import io.bluetape4k.examples.redisson.coroutines.AbstractRedissonCoroutineTest
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.redisson.coroutines.coAwait
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import org.redisson.api.RSortedSet

/**
 * Sorted set examples
 *
 * 참고: [SortedSet](https://github.com/redisson/redisson/wiki/7.-distributed-collections/#74-sortedset)
 */
class SortedSetExamples: AbstractRedissonCoroutineTest() {

    companion object: KLogging()

    private fun getSortedSet(): RSortedSet<Int> =
        redisson.getSortedSet<Int>(randomName()).apply {
            add(1)
            add(2)
            add(3)
        }

    @Test
    fun `RSortedSet example`() = runSuspendIO {
        val sset = getSortedSet()

        sset.first() shouldBeEqualTo 1
        sset.last() shouldBeEqualTo 3

        sset.remove(1).shouldBeTrue()

        sset.deleteAsync().coAwait()
    }
}
