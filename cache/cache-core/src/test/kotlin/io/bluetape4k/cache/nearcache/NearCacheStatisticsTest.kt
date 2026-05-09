package io.bluetape4k.cache.nearcache

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Test

class NearCacheStatisticsTest {

    companion object: KLogging()

    @Test
    fun `hitRate - 모든 값이 0이면 0_0 반환`() {
        val stats = DefaultNearCacheStatistics()
        stats.hitRate shouldBeEqualTo 0.0
    }

    @Test
    fun `hitRate - localHits만 있으면 1_0`() {
        val stats = DefaultNearCacheStatistics(localHits = 10)
        stats.hitRate shouldBeEqualTo 1.0
    }

    @Test
    fun `hitRate - backHits만 있으면 1_0`() {
        val stats = DefaultNearCacheStatistics(backHits = 5)
        stats.hitRate shouldBeEqualTo 1.0
    }

    @Test
    fun `hitRate - backMisses만 있으면 0_0`() {
        val stats = DefaultNearCacheStatistics(backMisses = 3)
        stats.hitRate shouldBeEqualTo 0.0
    }

    @Test
    fun `hitRate - 혼합 케이스 계산`() {
        val stats = DefaultNearCacheStatistics(
            localHits = 10,
            backHits = 1,
            backMisses = 1
        )
        // (10+1) / (10+1+1) = 11/12
        val expected = 11.0 / 12.0
        assert(kotlin.math.abs(stats.hitRate - expected) < 1e-9) {
            "Expected $expected but was ${stats.hitRate}"
        }
    }

    @Test
    fun `data class 기본값`() {
        val stats = DefaultNearCacheStatistics()
        stats.localHits shouldBeEqualTo 0L
        stats.localMisses shouldBeEqualTo 0L
        stats.localSize shouldBeEqualTo 0L
        stats.localEvictions shouldBeEqualTo 0L
        stats.backHits shouldBeEqualTo 0L
        stats.backMisses shouldBeEqualTo 0L
    }
}
