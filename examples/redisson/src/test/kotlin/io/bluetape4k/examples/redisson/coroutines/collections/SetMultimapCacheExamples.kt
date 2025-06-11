package io.bluetape4k.examples.redisson.coroutines.collections

import io.bluetape4k.coroutines.support.suspendAwait
import io.bluetape4k.examples.redisson.coroutines.AbstractRedissonCoroutineTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import org.redisson.api.BatchOptions
import java.util.concurrent.TimeUnit

/**
 * Set based Multimap
 *
 * 참고: [Multimap](https://github.com/redisson/redisson/wiki/7.-distributed-collections#72-multimap)
 */
class SetMultimapCacheExamples: AbstractRedissonCoroutineTest() {

    companion object: KLoggingChannel()

    private suspend fun addSampleData(mapName: String) {
        val batch = redisson.createBatch(BatchOptions.defaults())

        with(batch.getSetMultimapCache<String, Int>(mapName)) {
            putAllAsync("1", listOf(1, 2, 3, 1))
            putAllAsync("2", listOf(5, 6, 5))
            putAsync("4", 7)
            putAsync("2", 5)
        }
        batch.executeAsync().suspendAwait()
    }

    @Test
    fun `use RSetMultimapCache`() = runTest {
        val mmapName = randomName()
        val mmap = redisson.getSetMultimapCache<String, Int>(mmapName)
        addSampleData(mmapName)

        mmap.getAllAsync("1").suspendAwait() shouldBeEqualTo setOf(1, 2, 3)

        // expire 설정
        mmap.expireKeyAsync("1", 60, TimeUnit.SECONDS).suspendAwait()

        mmap.containsEntryAsync("1", 3).suspendAwait().shouldBeTrue()
        mmap.containsKeyAsync("1").suspendAwait().shouldBeTrue()
        mmap.containsValueAsync(3).suspendAwait().shouldBeTrue()


        mmap.entries().forEach { entry ->
            log.debug { "key=${entry.key}, value=${entry.value}" }
        }

        mmap.removeAsync("1", 3).suspendAwait().shouldBeTrue()
        mmap.getAllAsync("1").suspendAwait() shouldBeEqualTo setOf(1, 2)

        // put all
        mmap.putAllAsync("5", listOf(5, 6, 7, 8, 9)).suspendAwait().shouldBeTrue()

        // 기존 List를 반환하고 새로운 값을 설정
        mmap.replaceValuesAsync("2", listOf(5, 6, 7, 8, 9)).suspendAwait() shouldBeEqualTo setOf(5, 6)

        // RList 를 반환한다
        mmap.get("2").addAsync(100).suspendAwait()

        // List Value를 반환한다
        mmap.getAllAsync("2").suspendAwait() shouldBeEqualTo setOf(5, 6, 7, 8, 9, 100)

        // fast remove
        mmap.fastRemoveAsync("2").suspendAwait() shouldBeEqualTo 1
        // fast remove with not exists key
        mmap.fastRemoveAsync("9999").suspendAwait() shouldBeEqualTo 0

        mmap.deleteAsync().suspendAwait()
    }
}
