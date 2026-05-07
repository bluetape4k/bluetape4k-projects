package io.bluetape4k.redis.lettuce

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.lettuce.LettuceTestUtils.asyncCommands
import kotlinx.coroutines.future.await
import org.junit.jupiter.api.Test

class RedisFutureSupportTest: AbstractLettuceTest() {

    companion object: KLoggingChannel() {
        private const val ITEM_SIZE = 20
    }

    @Test
    fun `awaitSuspending - RedisFuture 결과를 suspend로 대기`() = runSuspendIO {
        val keyName = randomName()
        asyncCommands.set(keyName, "value").awaitSuspending() shouldBeEqualTo "OK"
        asyncCommands.get(keyName).awaitSuspending() shouldBeEqualTo "value"
        asyncCommands.del(keyName).await()
    }

    @Suppress("DEPRECATION")
    @Test
    fun `suspendAwait - deprecated 버전도 정상 동작`() = runSuspendIO {
        val keyName = randomName()
        asyncCommands.set(keyName, "hello").suspendAwait() shouldBeEqualTo "OK"
        asyncCommands.del(keyName).await()
    }

    @Suppress("DEPRECATION")
    @Test
    fun `coAwait - deprecated 버전도 정상 동작`() = runSuspendIO {
        val keyName = randomName()
        asyncCommands.set(keyName, "world").coAwait() shouldBeEqualTo "OK"
        asyncCommands.del(keyName).await()
    }

    @Test
    fun `awaitAll - 빈 컬렉션은 빈 리스트 반환`() = runSuspendIO {
        val result = emptyList<io.lettuce.core.RedisFuture<String>>().awaitAll()
        result shouldHaveSize 0
    }

    @Test
    fun `awaitAll - 복수 RedisFuture 일괄 대기`() = runSuspendIO {
        val keyName = randomName()
        val futures = List(ITEM_SIZE) { i ->
            asyncCommands.hset(keyName, i.toString(), i)
        }
        val results = futures.awaitAll()
        results shouldHaveSize ITEM_SIZE
        asyncCommands.del(keyName).await()
    }

    @Test
    fun `sequence - RedisFuture 컬렉션을 CompletableFuture로 변환`() {
        val keyName = randomName()
        val futures = List(ITEM_SIZE) { i ->
            asyncCommands.hset(keyName, i.toString(), i)
        }.sequence()
        val results = futures.get()
        results shouldHaveSize ITEM_SIZE
        asyncCommands.del(keyName).get()
    }
}
