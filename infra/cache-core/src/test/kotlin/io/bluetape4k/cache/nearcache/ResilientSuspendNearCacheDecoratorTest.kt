package io.bluetape4k.cache.nearcache

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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

        assertThrows<RuntimeException> {
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
}
