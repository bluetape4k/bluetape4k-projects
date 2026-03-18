package io.bluetape4k.cache.nearcache

import io.bluetape4k.logging.KLogging
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration

/**
 * [ResilientNearCacheDecorator] 단위 테스트.
 *
 * MockK로 [NearCacheOperations]를 mock하여 retry 및 failure strategy를 검증합니다.
 */
class ResilientNearCacheDecoratorTest {
    companion object : KLogging()

    private val delegate = mockk<NearCacheOperations<String>>(relaxed = true)

    @BeforeEach
    fun setup() {
        clearMocks(delegate)
        every { delegate.cacheName } returns "test-cache"
        every { delegate.isClosed } returns false
        every { delegate.stats() } returns DefaultNearCacheStatistics()
    }

    @Test
    fun `get - retry 후 성공`() {
        var callCount = 0
        every { delegate.get("key1") } answers {
            callCount++
            if (callCount < 3) throw RuntimeException("transient failure")
            "value1"
        }

        val cache =
            ResilientNearCacheDecorator(
                delegate,
                NearCacheResilienceConfig(
                    retryMaxAttempts = 3,
                    retryWaitDuration = Duration.ofMillis(50),
                    getFailureStrategy = GetFailureStrategy.PROPAGATE_EXCEPTION
                )
            )

        cache.get("key1") shouldBeEqualTo "value1"
        verify(exactly = 3) { delegate.get("key1") }
    }

    @Test
    fun `get - RETURN_FRONT_OR_NULL 전략 시 실패하면 null 반환`() {
        every { delegate.get("key1") } throws RuntimeException("failure")

        val cache =
            ResilientNearCacheDecorator(
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
        every { delegate.get("key1") } throws RuntimeException("failure")

        val cache =
            ResilientNearCacheDecorator(
                delegate,
                NearCacheResilienceConfig(
                    retryMaxAttempts = 1,
                    retryWaitDuration = Duration.ofMillis(10),
                    getFailureStrategy = GetFailureStrategy.PROPAGATE_EXCEPTION
                )
            )

        assertThrows<RuntimeException> { cache.get("key1") }
    }

    @Test
    fun `put - retry 후 성공`() {
        var callCount = 0
        every { delegate.put("key1", "value1") } answers {
            callCount++
            if (callCount < 2) throw RuntimeException("transient failure")
        }

        val cache =
            ResilientNearCacheDecorator(
                delegate,
                NearCacheResilienceConfig(
                    retryMaxAttempts = 3,
                    retryWaitDuration = Duration.ofMillis(50)
                )
            )

        cache.put("key1", "value1")
        verify(exactly = 2) { delegate.put("key1", "value1") }
    }

    @Test
    fun `put - retry 초과 시 예외 전파`() {
        every { delegate.put("key1", "value1") } throws RuntimeException("persistent failure")

        val cache =
            ResilientNearCacheDecorator(
                delegate,
                NearCacheResilienceConfig(
                    retryMaxAttempts = 2,
                    retryWaitDuration = Duration.ofMillis(10)
                )
            )

        assertThrows<RuntimeException> { cache.put("key1", "value1") }
    }

    @Test
    fun `close - delegate에 위임`() {
        val cache = ResilientNearCacheDecorator(delegate)

        cache.close()
        verify(exactly = 1) { delegate.close() }
    }

    @Test
    fun `stats - delegate에 위임`() {
        val expectedStats = DefaultNearCacheStatistics(localHits = 10, backHits = 5)
        every { delegate.stats() } returns expectedStats

        val cache = ResilientNearCacheDecorator(delegate)

        cache.stats() shouldBeEqualTo expectedStats
    }

    @Test
    fun `getAll - RETURN_FRONT_OR_NULL 전략 시 빈 Map 반환`() {
        every { delegate.getAll(any()) } throws RuntimeException("failure")

        val cache =
            ResilientNearCacheDecorator(
                delegate,
                NearCacheResilienceConfig(
                    retryMaxAttempts = 1,
                    retryWaitDuration = Duration.ofMillis(10),
                    getFailureStrategy = GetFailureStrategy.RETURN_FRONT_OR_NULL
                )
            )

        cache.getAll(setOf("k1", "k2")) shouldBeEqualTo emptyMap()
    }
}
