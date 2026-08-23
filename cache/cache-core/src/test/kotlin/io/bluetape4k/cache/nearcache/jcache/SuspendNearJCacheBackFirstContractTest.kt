package io.bluetape4k.cache.nearcache.jcache

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.cache.jcache.SuspendJCache
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class SuspendNearJCacheBackFirstContractTest {

    private val failure = IllegalStateException("back cache is unavailable")

    @Test
    fun `put은 back 성공 후 front를 반영한다`() = runSuspendIO {
        val frontCache = mockk<SuspendJCache<String, String>>(relaxed = true)
        val backCache = mockk<SuspendJCache<String, String>>(relaxed = true)
        coEvery { backCache.put("key", "value") } just runs

        val nearCache = SuspendNearJCache.withoutListener(frontCache, backCache)

        nearCache.put("key", "value")

        coVerifyOrder {
            backCache.put("key", "value")
            frontCache.put("key", "value")
        }
    }

    @Test
    fun `put은 back 실패 시 front에 미커밋 값을 남기지 않는다`() = runSuspendIO {
        val frontCache = mockk<SuspendJCache<String, String>>(relaxed = true)
        val backCache = mockk<SuspendJCache<String, String>>(relaxed = true)
        coEvery { backCache.put("key", "value") } throws failure
        val nearCache = SuspendNearJCache.withoutListener(frontCache, backCache)

        assertFailsWith<IllegalStateException> { nearCache.put("key", "value") }

        coVerify(exactly = 0) { frontCache.put("key", "value") }
    }

    @Test
    fun `putAll은 back 실패 시 front에 미커밋 값을 남기지 않는다`() = runSuspendIO {
        val entries = mapOf("key" to "value")
        val frontCache = mockk<SuspendJCache<String, String>>(relaxed = true)
        val backCache = mockk<SuspendJCache<String, String>>(relaxed = true)
        coEvery { backCache.putAll(entries) } throws failure
        val nearCache = SuspendNearJCache.withoutListener(frontCache, backCache)

        assertFailsWith<IllegalStateException> { nearCache.putAll(entries) }

        coVerify(exactly = 0) { frontCache.putAll(entries) }
    }

    @Test
    fun `putIfAbsent는 back 실패 시 front에 미커밋 값을 남기지 않는다`() = runSuspendIO {
        val frontCache = mockk<SuspendJCache<String, String>>(relaxed = true)
        val backCache = mockk<SuspendJCache<String, String>>(relaxed = true)
        coEvery { backCache.putIfAbsent("key", "value") } throws failure
        val nearCache = SuspendNearJCache.withoutListener(frontCache, backCache)

        assertFailsWith<IllegalStateException> { nearCache.putIfAbsent("key", "value") }

        coVerify(exactly = 0) { frontCache.putIfAbsent("key", "value") }
    }

    @Test
    fun `remove는 back 실패 시 front를 변경하지 않는다`() = runSuspendIO {
        val frontCache = mockk<SuspendJCache<String, String>>(relaxed = true)
        val backCache = mockk<SuspendJCache<String, String>>(relaxed = true)
        coEvery { backCache.remove("key") } throws failure
        val nearCache = SuspendNearJCache.withoutListener(frontCache, backCache)

        assertFailsWith<IllegalStateException> { nearCache.remove("key") }

        coVerify(exactly = 0) { frontCache.remove("key") }
    }

    @Test
    fun `조건부 remove는 back 실패 시 front를 변경하지 않는다`() = runSuspendIO {
        val frontCache = mockk<SuspendJCache<String, String>>(relaxed = true)
        val backCache = mockk<SuspendJCache<String, String>>(relaxed = true)
        coEvery { backCache.remove("key", "old") } throws failure
        val nearCache = SuspendNearJCache.withoutListener(frontCache, backCache)

        assertFailsWith<IllegalStateException> { nearCache.remove("key", "old") }

        coVerify(exactly = 0) { frontCache.remove("key", "old") }
    }

    @Test
    fun `replace는 back 실패 시 front를 변경하지 않는다`() = runSuspendIO {
        val frontCache = mockk<SuspendJCache<String, String>>(relaxed = true)
        val backCache = mockk<SuspendJCache<String, String>>(relaxed = true)
        coEvery { backCache.replace("key", "value") } throws failure
        val nearCache = SuspendNearJCache.withoutListener(frontCache, backCache)

        assertFailsWith<IllegalStateException> { nearCache.replace("key", "value") }

        coVerify(exactly = 0) { frontCache.replace("key", "value") }
    }

    @Test
    fun `조건부 replace는 back 실패 시 front를 변경하지 않는다`() = runSuspendIO {
        val frontCache = mockk<SuspendJCache<String, String>>(relaxed = true)
        val backCache = mockk<SuspendJCache<String, String>>(relaxed = true)
        coEvery { backCache.replace("key", "old", "value") } throws failure
        val nearCache = SuspendNearJCache.withoutListener(frontCache, backCache)

        assertFailsWith<IllegalStateException> { nearCache.replace("key", "old", "value") }

        coVerify(exactly = 0) { frontCache.replace("key", "old", "value") }
    }

    @Test
    fun `back cancellation은 front 변경 없이 재전파된다`() = runSuspendIO {
        val frontCache = mockk<SuspendJCache<String, String>>(relaxed = true)
        val backCache = mockk<SuspendJCache<String, String>>(relaxed = true)
        val cancellation = CancellationException("back cache cancelled")
        coEvery { backCache.put("key", "value") } throws cancellation
        val nearCache = SuspendNearJCache.withoutListener(frontCache, backCache)

        val error = assertFailsWith<CancellationException> { nearCache.put("key", "value") }

        error.message shouldBeEqualTo cancellation.message
        coVerify(exactly = 0) { frontCache.put("key", "value") }
    }

    @Test
    fun `back commit 후 front 반영 실패는 front를 invalidate하고 원래 예외를 전달한다`() = runSuspendIO {
        val frontCache = mockk<SuspendJCache<String, String>>(relaxed = true)
        val backCache = mockk<SuspendJCache<String, String>>(relaxed = true)
        val frontFailure = IllegalStateException("front cache is unavailable")
        coEvery { backCache.put("key", "value") } just runs
        coEvery { frontCache.put("key", "value") } throws frontFailure
        val nearCache = SuspendNearJCache.withoutListener(frontCache, backCache)

        val error = assertFailsWith<IllegalStateException> { nearCache.put("key", "value") }

        error.message shouldBeEqualTo frontFailure.message
        coVerify(exactly = 1) { frontCache.remove("key") }
    }

    @Test
    fun `front 반영 중 cancellation도 front를 invalidate하고 재전파한다`() = runSuspendIO {
        val frontCache = mockk<SuspendJCache<String, String>>(relaxed = true)
        val backCache = mockk<SuspendJCache<String, String>>(relaxed = true)
        val cancellation = CancellationException("front cache cancelled")
        coEvery { backCache.put("key", "value") } just runs
        coEvery { frontCache.put("key", "value") } throws cancellation
        val nearCache = SuspendNearJCache.withoutListener(frontCache, backCache)

        val error = assertFailsWith<CancellationException> { nearCache.put("key", "value") }

        error.message shouldBeEqualTo cancellation.message
        coVerify(exactly = 1) { frontCache.remove("key") }
    }

    @Test
    fun `clearAll은 back CancellationException을 호출자에게 재전파한다`() = runTest {
        val cancellation = CancellationException("clear cancelled")
        val frontCache = mockk<SuspendJCache<String, String>>(relaxed = true)
        val backCache = mockk<SuspendJCache<String, String>>(relaxed = true)
        coEvery { frontCache.clear() } just runs
        coEvery { backCache.clear() } throws cancellation
        val nearCache = SuspendNearJCache.withoutListener(frontCache, backCache)

        val thrown = assertFailsWith<CancellationException> { nearCache.clearAll() }

        (thrown === cancellation).shouldBeTrue()
        coVerify { frontCache.clear() }
        coVerify { backCache.clear() }
    }

    @Test
    fun `close는 CancellationException을 호출자에게 재전파한다`() = runTest {
        val cancellation = CancellationException("close cancelled")
        val frontCache = mockk<SuspendJCache<String, String>>(relaxed = true)
        val backCache = mockk<SuspendJCache<String, String>>(relaxed = true)
        coEvery { frontCache.close() } throws cancellation
        val nearCache = SuspendNearJCache.withoutListener(frontCache, backCache)

        val thrown = assertFailsWith<CancellationException> { nearCache.close() }

        (thrown === cancellation).shouldBeTrue()
        coVerify { frontCache.close() }
    }
}
