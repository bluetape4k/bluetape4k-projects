package io.bluetape4k.redis.redisson.coroutines

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.future.await
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.RepeatedTest
import org.redisson.api.RFuture

class RFutureSupportTest: AbstractRedissonCoroutineTest() {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 3
        private const val ITEM_COUNT = 100
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `put async and get by sequence`() = runSuspendIO {
        val map = redisson.getMap<Int, Int>(randomName())

        // 이것보다 RBatch 를 이용하는 게 낫다 ㅋ
        val futures: List<RFuture<Int>> = List(ITEM_COUNT) {
            map.putAsync(it, it)
        }
        val lists = futures.sequence().await()

        lists.size shouldBeEqualTo ITEM_COUNT
        map.delete()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `run async operations with awaitSuspend`() = runSuspendIO {
        val map = redisson.getMap<Int, Int>(randomName())

        // 당연하게도 아무리 비동기라도 round-trip이 많은 것보다 RBatch 가 낫다 ㅋ
        val defers = List(ITEM_COUNT) {
            async(Dispatchers.IO) {
                map.putAsync(it, it).coAwait()
            }
        }
        val lists: List<Int> = defers.awaitAll()
        lists.size shouldBeEqualTo ITEM_COUNT

        map.deleteAsync().coAwait()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `put suspended and awaitAll`() = runSuspendIO {
        val map = redisson.getMap<Int, Int>(randomName())

        // 당연하게도 아무리 비동기라도 round-trip이 많은 것보다 RBatch 가 낫다 ㅋ
        val futures: List<RFuture<Int>> = List(ITEM_COUNT) {
            map.putAsync(it, it)
        }
        // RFuture 의 Collection인 경우 awaitAll 로 모두 호출할 수 있습니다.
        val lists: List<Int> = futures.awaitAll()

        lists.size shouldBeEqualTo ITEM_COUNT
        map.deleteAsync().coAwait()
    }
}
