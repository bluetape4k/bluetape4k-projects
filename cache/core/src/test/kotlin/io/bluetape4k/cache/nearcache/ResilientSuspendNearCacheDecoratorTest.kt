package io.bluetape4k.cache.nearcache

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import io.bluetape4k.assertions.assertFailsWith
import java.time.Duration

/**
 * [ResilientSuspendNearCacheDecorator] 단위 테스트.
 *
 * MockK `coEvery`/`coVerify`로 [SuspendNearCacheOperations]를 mock하여
 * suspend 환경에서의 retry 및 failure strategy를 검증합니다.
 */
class ResilientSuspendNearCacheDecoratorTest {
    companion object: KLogging()

    private val delegate = mockk<SuspendNearCacheOperations<String>>(relaxed = true)

    @BeforeEach
    fun setup() {
        clearMocks(delegate)
        every { delegate.cacheName } returns "test-suspend-cache"
        every { delegate.isClosed } returns false
        every { delegate.stats() } returns DefaultNearCacheStatistics()
    }

    @Test
    fun `get - retry 후 성공`() = runSuspendIO {
        var callCount = 0
        coEvery { delegate.get("key1") } answers {
            callCount++
            if (callCount < 3) throw RuntimeException("transient failure")
            "value1"
        }

        val cache =
            ResilientSuspendNearCacheDecorator(
                delegate,
                NearCacheResilienceConfig(
                    retryMaxAttempts = 3,
                    retryWaitDuration = Duration.ofMillis(50),
                    getFailureStrategy = GetFailureStrategy.PROPAGATE_EXCEPTION
                )
            )

        cache.get("key1") shouldBeEqualTo "value1"
        coVerify(exactly = 3) { delegate.get("key1") }
    }

    @Test
    fun `get - RETURN_FRONT_OR_NULL 전략 시 null 반환`() = runSuspendIO {
        coEvery { delegate.get("key1") } throws RuntimeException("failure")

        val cache =
            ResilientSuspendNearCacheDecorator(
                delegate,
                NearCacheResilienceConfig(
                    retryMaxAttempts = 1,
                    retryWaitDuration = Duration.ofMillis(10),
                    getFailureStrategy = GetFailureStrategy.RETURN_FRONT_OR_NULL
                )
            )

        cache.get("key1").shouldBeNull()
    }

    @Test
    fun `get - PROPAGATE_EXCEPTION 전략 시 예외 전파`() {
        coEvery { delegate.get("key1") } throws RuntimeException("failure")

        val cache =
            ResilientSuspendNearCacheDecorator(
                delegate,
                NearCacheResilienceConfig(
                    retryMaxAttempts = 1,
                    retryWaitDuration = Duration.ofMillis(10),
                    getFailureStrategy = GetFailureStrategy.PROPAGATE_EXCEPTION
                )
            )

        assertFailsWith<RuntimeException> {
            runSuspendIO { cache.get("key1") }
        }
    }

    @Test
    fun `put - retry 후 성공`() = runSuspendIO {
        var callCount = 0
        coEvery { delegate.put("key1", "value1") } answers {
            callCount++
            if (callCount < 2) throw RuntimeException("transient failure")
        }

        val cache =
            ResilientSuspendNearCacheDecorator(
                delegate,
                NearCacheResilienceConfig(
                    retryMaxAttempts = 3,
                    retryWaitDuration = Duration.ofMillis(50)
                )
            )

        cache.put("key1", "value1")
        coVerify(exactly = 2) { delegate.put("key1", "value1") }
    }

    @Test
    fun `close - delegate에 위임`() = runSuspendIO {
        val cache = ResilientSuspendNearCacheDecorator(delegate)

        cache.close()
        coVerify(exactly = 1) { delegate.close() }
    }

    @Test
    fun `stats - delegate에 위임`() {
        val expectedStats = DefaultNearCacheStatistics(localHits = 10, backHits = 5)
        every { delegate.stats() } returns expectedStats

        val cache = ResilientSuspendNearCacheDecorator(delegate)

        cache.stats() shouldBeEqualTo expectedStats
    }

    /**
     * CancellationException은 코루틴 취소 신호이므로 catch(e: Exception)에서 삼키지 않고
     * 반드시 재전파해야 한다. 그렇지 않으면 코루틴이 취소되지 않고 좀비 상태가 된다.
     */
    @Test
    fun `get - CancellationException은 RETURN_FRONT_OR_NULL 전략에서도 반드시 재전파된다`() = runSuspendIO {
        coEvery { delegate.get("key1") } throws kotlinx.coroutines.CancellationException("test cancellation")

        val cache = ResilientSuspendNearCacheDecorator(
            delegate,
            NearCacheResilienceConfig(
                retryMaxAttempts = 1,
                retryWaitDuration = Duration.ofMillis(10),
                getFailureStrategy = GetFailureStrategy.RETURN_FRONT_OR_NULL
            )
        )

        var propagated = false
        try {
            cache.get("key1")
        } catch (e: kotlinx.coroutines.CancellationException) {
            propagated = true
        }
        propagated shouldBeEqualTo true
    }

    /**
     * getAll()에서도 CancellationException은 RETURN_FRONT_OR_NULL 전략과 무관하게 재전파된다.
     */
    @Test
    fun `getAll - CancellationException은 RETURN_FRONT_OR_NULL 전략에서도 반드시 재전파된다`() = runSuspendIO {
        coEvery { delegate.getAll(any()) } throws kotlinx.coroutines.CancellationException("test cancellation")

        val cache = ResilientSuspendNearCacheDecorator(
            delegate,
            NearCacheResilienceConfig(
                retryMaxAttempts = 1,
                retryWaitDuration = Duration.ofMillis(10),
                getFailureStrategy = GetFailureStrategy.RETURN_FRONT_OR_NULL
            )
        )

        var propagated = false
        try {
            cache.getAll(setOf("k1", "k2"))
        } catch (e: kotlinx.coroutines.CancellationException) {
            propagated = true
        }
        propagated shouldBeEqualTo true
    }
}
