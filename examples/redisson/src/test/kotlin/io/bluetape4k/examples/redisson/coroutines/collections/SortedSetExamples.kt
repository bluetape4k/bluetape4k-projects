package io.bluetape4k.examples.redisson.coroutines.collections

import io.bluetape4k.coroutines.support.awaitSuspending
import io.bluetape4k.examples.redisson.coroutines.AbstractRedissonCoroutineTest
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
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

    companion object: KLoggingChannel()

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

        sset.removeAsync(1).awaitSuspending().shouldBeTrue()

        sset.deleteAsync().awaitSuspending()
    }
}
