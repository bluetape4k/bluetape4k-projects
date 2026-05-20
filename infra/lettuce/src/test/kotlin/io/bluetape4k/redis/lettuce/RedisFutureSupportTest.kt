package io.bluetape4k.redis.lettuce

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.lettuce.LettuceTestUtils.asyncCommands
import io.lettuce.core.RedisFuture
import kotlinx.coroutines.future.await
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

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

    @Test
    fun `awaitSuspending should handle set command`() = runSuspendIO {
        val keyName = randomName()
        asyncCommands.set(keyName, "hello").awaitSuspending() shouldBeEqualTo "OK"
        asyncCommands.del(keyName).await()
    }

    @Test
    fun `awaitSuspending should handle get command`() = runSuspendIO {
        val keyName = randomName()
        asyncCommands.set(keyName, "world").awaitSuspending() shouldBeEqualTo "OK"
        asyncCommands.get(keyName).awaitSuspending() shouldBeEqualTo "world"
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
    fun `awaitAll - 실패한 RedisFuture 예외 전파`() = runSuspendIO {
        val failure = IllegalStateException("redis command failed")
        val failed = failedRedisFuture<String>(failure)

        val error = assertFailsWith<IllegalStateException> {
            listOf(failed).awaitAll()
        }
        error.message shouldBeEqualTo failure.message
    }

    @Test
    fun `awaitAll - SuspendedJobTester 로 코루틴 일괄 대기 안정성 검증`() = runSuspendIO {
        SuspendedJobTester()
            .workers(4)
            .rounds(64)
            .add {
                val results = listOf(
                    completedRedisFuture("first"),
                    completedRedisFuture("second"),
                    completedRedisFuture("third"),
                ).awaitAll()

                results shouldBeEqualTo listOf("first", "second", "third")
            }
            .run()
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

    @Test
    fun `sequence - 완료 순서와 무관하게 입력 순서 유지`() {
        val first = TestRedisFuture<Int>()
        val second = TestRedisFuture<Int>()
        val third = TestRedisFuture<Int>()

        val sequenced = listOf(first, second, third).sequence()

        third.complete(3)
        first.complete(1)
        second.complete(2)

        sequenced.get() shouldBeEqualTo listOf(1, 2, 3)
    }

    private fun <T> completedRedisFuture(value: T): RedisFuture<T> =
        TestRedisFuture<T>().apply { complete(value) }

    private fun <T> failedRedisFuture(error: Throwable): RedisFuture<T> =
        TestRedisFuture<T>().apply { completeExceptionally(error) }

    private class TestRedisFuture<T>: CompletableFuture<T>(), RedisFuture<T> {

        override fun getError(): String? =
            if (isCompletedExceptionally) "completed exceptionally" else null

        override fun await(timeout: Long, unit: TimeUnit): Boolean =
            try {
                get(timeout, unit)
                true
            } catch (_: TimeoutException) {
                false
            }
    }
}
