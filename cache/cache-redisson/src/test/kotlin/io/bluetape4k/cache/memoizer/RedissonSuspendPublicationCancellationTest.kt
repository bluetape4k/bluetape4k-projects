package io.bluetape4k.cache.memoizer

import io.bluetape4k.assertions.shouldBeFalse
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.time.Duration.Companion.seconds
import org.junit.jupiter.api.Test
import org.redisson.api.RMap
import org.redisson.misc.CompletableFutureWrapper
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(ExperimentalCoroutinesApi::class)
class RedissonSuspendPublicationCancellationTest {

    @Test
    fun `저장 중 취소는 서버 완료를 기다리되 호출자에게 값을 반환하지 않는다`() = runTest(timeout = 30.seconds) {
        val map = mockk<RMap<Int, Int>>()
        val put = CompletableFuture<Int?>()
        every { map.name } returns "cancellation-test"
        every { map.getAsync(1) } returns CompletableFutureWrapper(CompletableFuture.completedFuture(null))
        every { map.putIfAbsentAsync(1, 10) } returns CompletableFutureWrapper(put)
        every { map.clearAsync() } returns CompletableFutureWrapper(CompletableFuture.completedFuture(null))
        val returned = AtomicBoolean()
        val memo = RedissonSuspendMemoizer(map) { 10 }
        val caller = launch {
            memo(1)
            returned.set(true)
        }
        try {
            runCurrent()
            verify(exactly = 1) { map.putIfAbsentAsync(1, 10) }
            caller.cancel()
            val clear = async { memo.clear() }
            runCurrent()
            verify(exactly = 0) { map.clearAsync() }
            put.complete(null)
            caller.join()
            clear.await()
            returned.get().shouldBeFalse()
            verify(exactly = 1) { map.clearAsync() }
        } finally {
            put.complete(null)
        }
    }

    @Test
    fun `삭제 중 취소도 서버 완료 후 호출자에게 전파한다`() = runTest(timeout = 30.seconds) {
        val map = mockk<RMap<Int, Int>>()
        val deletion = CompletableFuture<Boolean>()
        every { map.name } returns "clear-cancellation-test"
        every { map.clearAsync() } returns CompletableFutureWrapper(deletion)
        val returned = AtomicBoolean()
        val memo = RedissonSuspendMemoizer(map) { 10 }
        val caller = launch {
            memo.clear()
            returned.set(true)
        }
        try {
            runCurrent()
            verify(exactly = 1) { map.clearAsync() }
            caller.cancel()
            deletion.complete(true)
            caller.join()
            returned.get().shouldBeFalse()
        } finally {
            deletion.complete(true)
        }
    }

}
