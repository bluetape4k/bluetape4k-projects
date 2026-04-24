package io.bluetape4k.cache.nearcache.jcache.management

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInRange
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class NearJCacheStatisticsMXBeanTest {

    companion object: KLogging()

    private lateinit var stats: NearJCacheStatisticsMXBean

    @BeforeEach
    fun setup() {
        stats = NearJCacheStatisticsMXBean()
    }

    @Test
    fun `초기 상태 - 모든 카운터가 0`() {
        stats.cacheHits shouldBeEqualTo 0L
        stats.cacheMisses shouldBeEqualTo 0L
        stats.cacheGets shouldBeEqualTo 0L
        stats.cachePuts shouldBeEqualTo 0L
        stats.cacheRemovals shouldBeEqualTo 0L
        stats.cacheEvictions shouldBeEqualTo 0L
    }

    @Test
    fun `addHits - hit 횟수 누적`() {
        stats.addHits(10L)
        stats.addHits(5L)
        stats.cacheHits shouldBeEqualTo 15L
    }

    @Test
    fun `addMisses - miss 횟수 누적`() {
        stats.addMisses(3L)
        stats.cacheMisses shouldBeEqualTo 3L
    }

    @Test
    fun `cacheGets - hits + misses`() {
        stats.addHits(10L)
        stats.addMisses(2L)
        stats.cacheGets shouldBeEqualTo 12L
    }

    @Test
    fun `cacheHitPercentage - hits 비율 계산`() {
        stats.addHits(75L)
        stats.addMisses(25L)
        stats.cacheHitPercentage shouldBeInRange 74.9f..75.1f
    }

    @Test
    fun `cacheHitPercentage - gets가 0이면 0 반환`() {
        stats.cacheHitPercentage shouldBeEqualTo 0f
    }

    @Test
    fun `cacheMissPercentage - misses 비율 계산`() {
        stats.addHits(75L)
        stats.addMisses(25L)
        stats.cacheMissPercentage shouldBeInRange 24.9f..25.1f
    }

    @Test
    fun `cacheMissPercentage - gets가 0이면 0 반환`() {
        stats.cacheMissPercentage shouldBeEqualTo 0f
    }

    @Test
    fun `addPuts - put 횟수 누적`() {
        stats.addPuts(7L)
        stats.cachePuts shouldBeEqualTo 7L
    }

    @Test
    fun `addRemovals - removal 횟수 누적`() {
        stats.addRemovals(4L)
        stats.cacheRemovals shouldBeEqualTo 4L
    }

    @Test
    fun `addEvictions - eviction 횟수 누적`() {
        stats.addEvictions(2L)
        stats.cacheEvictions shouldBeEqualTo 2L
    }

    @Test
    fun `addGetTime과 averageGetTime 계산`() {
        stats.addHits(100L)
        stats.addGetTime(1_000_000L)  // 1ms in nanoseconds
        stats.averageGetTime shouldBeInRange 0.001f..100f
    }

    @Test
    fun `averageGetTime - gets가 0이면 0 반환`() {
        stats.averageGetTime shouldBeEqualTo 0f
    }

    @Test
    fun `addPutTime과 averagePutTime 계산`() {
        stats.addPuts(50L)
        stats.addPutTime(500_000L)
        stats.averagePutTime shouldBeInRange 0.001f..100f
    }

    @Test
    fun `averagePutTime - puts가 0이면 0 반환`() {
        stats.averagePutTime shouldBeEqualTo 0f
    }

    @Test
    fun `addRemoveTime과 averageRemoveTime 계산`() {
        stats.addRemovals(20L)
        stats.addRemoveTime(200_000L)
        stats.averageRemoveTime shouldBeInRange 0.001f..100f
    }

    @Test
    fun `averageRemoveTime - removals가 0이면 0 반환`() {
        stats.averageRemoveTime shouldBeEqualTo 0f
    }

    @Test
    fun `clear - 모든 카운터를 0으로 초기화`() {
        stats.addHits(10L)
        stats.addMisses(5L)
        stats.addPuts(15L)
        stats.addRemovals(3L)
        stats.addEvictions(1L)

        stats.clear()

        stats.cacheHits shouldBeEqualTo 0L
        stats.cacheMisses shouldBeEqualTo 0L
        stats.cachePuts shouldBeEqualTo 0L
        stats.cacheRemovals shouldBeEqualTo 0L
        stats.cacheEvictions shouldBeEqualTo 0L
    }
}

class EmptyNearJCacheStatisticsMXBeanTest {

    companion object: KLogging()

    private val stats = EmptyNearJCacheStatisticsMXBean()

    @Test
    fun `addHits - no-op으로 항상 0 유지`() {
        stats.addHits(100L)
        stats.cacheHits shouldBeEqualTo 0L
    }

    @Test
    fun `addMisses - no-op으로 항상 0 유지`() {
        stats.addMisses(100L)
        stats.cacheMisses shouldBeEqualTo 0L
    }

    @Test
    fun `addPuts - no-op으로 항상 0 유지`() {
        stats.addPuts(100L)
        stats.cachePuts shouldBeEqualTo 0L
    }

    @Test
    fun `addEvictions - no-op으로 항상 0 유지`() {
        stats.addEvictions(100L)
        stats.cacheEvictions shouldBeEqualTo 0L
    }

    @Test
    fun `addGetTime - no-op이므로 averageGetTime은 0`() {
        stats.addGetTime(1_000_000L)
        stats.averageGetTime shouldBeEqualTo 0f
    }

    @Test
    fun `addPutTime - no-op이므로 averagePutTime은 0`() {
        stats.addPutTime(1_000_000L)
        stats.averagePutTime shouldBeEqualTo 0f
    }

    @Test
    fun `addRemoveTime - no-op이므로 averageRemoveTime은 0`() {
        stats.addRemoveTime(1_000_000L)
        stats.averageRemoveTime shouldBeEqualTo 0f
    }
}
