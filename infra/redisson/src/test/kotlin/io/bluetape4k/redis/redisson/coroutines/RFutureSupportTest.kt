package io.bluetape4k.redis.redisson.coroutines

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.coroutines.support.awaitSuspending
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.redisson.RedissonTestUtils.randomName
import io.bluetape4k.redis.redisson.RedissonTestUtils.redisson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.future.await
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.redisson.api.RFuture
import java.util.concurrent.CompletableFuture

class RFutureSupportTest: AbstractRedissonCoroutineTest() {

    companion object: KLoggingChannel() {
        private const val REPEAT_SIZE = 3
        private const val ITEM_COUNT = 100
    }

    @Test
    fun `awaitAll returns empty list for empty futures`() = runSuspendIO {
        emptyList<RFuture<Int>>().awaitAll() shouldBeEqualTo emptyList()
    }

    @Test
    fun `awaitAll propagates failed RFuture`() = runSuspendIO {
        val failure = IllegalStateException("redisson command failed")
        val failed = failedRFuture<String>(failure)

        val error = assertFailsWith<IllegalStateException> {
            listOf(failed).awaitAll()
        }
        error.message shouldBeEqualTo failure.message
    }

    @Test
    fun `awaitAll is stable under SuspendedJobTester`() = runSuspendIO {
        SuspendedJobTester()
            .workers(4)
            .rounds(64)
            .add {
                val results = listOf(
                    completedRFuture("first"),
                    completedRFuture("second"),
                    completedRFuture("third"),
                ).awaitAll()

                results shouldBeEqualTo listOf("first", "second", "third")
            }
            .run()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `put async and get by sequence`() = runSuspendIO {
        val map = redisson.getMap<Int, Int>(randomName())

        // 당연하게도 아무리 비동기라도 round-trip이 많은 것보다 RBatch 가 낫다. 또는 `putAllAsync` 를 이용하는 게 낫다
        val futures: List<RFuture<Int>> = List(ITEM_COUNT) {
            map.putAsync(it, it)
        }
        val lists = futures.sequence().await()

        lists.size shouldBeEqualTo ITEM_COUNT
        map.delete()
    }

    @Test
    fun `sequence preserves input order independent of completion order`() {
        val first = TestRFuture<Int>()
        val second = TestRFuture<Int>()
        val third = TestRFuture<Int>()

        val sequenced = listOf(first, second, third).sequence()

        third.complete(3)
        first.complete(1)
        second.complete(2)

        sequenced.get() shouldBeEqualTo listOf(1, 2, 3)
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `run async operations with awaitSuspend`() = runSuspendIO {
        val map = redisson.getMap<Int, Int>(randomName())

        // 당연하게도 아무리 비동기라도 round-trip이 많은 것보다 RBatch 가 낫다. 또는 `putAllAsync` 를 이용하는 게 낫다
        val defers = List(ITEM_COUNT) {
            async(Dispatchers.IO) {
                map.putAsync(it, it).awaitSuspending()
            }
        }
        val lists: List<Int> = defers.awaitAll()
        lists.size shouldBeEqualTo ITEM_COUNT

        map.deleteAsync().awaitSuspending()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `put suspended and awaitAll`() = runSuspendIO {
        val map = redisson.getMap<Int, Int>(randomName())

        // 당연하게도 아무리 비동기라도 round-trip이 많은 것보다 RBatch 가 낫다. 또는 `putAllAsync` 를 이용하는 게 낫다
        val futures: List<RFuture<Int>> = List(ITEM_COUNT) {
            map.putAsync(it, it)
        }
        // RFuture 의 Collection인 경우 awaitAll 로 모두 호출할 수 있습니다.
        val lists: List<Int> = futures.awaitAll()

        lists.size shouldBeEqualTo ITEM_COUNT
        map.deleteAsync().awaitSuspending()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `putAll async and get by sequence`() = runSuspendIO {
        val map = redisson.getMap<Int, Int>(randomName())

        val items = (0 until ITEM_COUNT).associateWith { it }

        // 당연하게도 아무리 비동기라도 round-trip이 많은 것보다 RBatch 가 낫다. 또는 `putAllAsync` 를 이용하는 게 낫다
        map.putAllAsync(items).awaitSuspending()

        val lists = map.getAllAsync(items.keys).awaitSuspending()
        lists shouldBeEqualTo items
        lists.size shouldBeEqualTo ITEM_COUNT
        map.deleteAsync().awaitSuspending()
    }

    private fun <T> completedRFuture(value: T): RFuture<T> =
        TestRFuture<T>().apply { complete(value) }

    private fun <T> failedRFuture(error: Throwable): RFuture<T> =
        TestRFuture<T>().apply { completeExceptionally(error) }

    private class TestRFuture<T>: CompletableFuture<T>(), RFuture<T>
}
