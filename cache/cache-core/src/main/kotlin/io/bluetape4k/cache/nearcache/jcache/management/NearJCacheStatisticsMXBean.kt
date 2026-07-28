package io.bluetape4k.cache.nearcache.jcache.management

import kotlinx.atomicfu.atomic
import java.util.concurrent.TimeUnit
import javax.cache.management.CacheStatisticsMXBean

/**
 * [NearCache]의 통계 정보를 제공하는 [CacheStatisticsMXBean] 구현체입니다.
 *
 * ```kotlin
 * val mxBean = NearJCacheStatisticsMXBean()
 * mxBean.addHits(10)
 * mxBean.addMisses(2)
 * mxBean.addPuts(12)
 * val hitPct = mxBean.cacheHitPercentage
 * // hitPct == (10 * 100f) / (10 + 2) ≈ 83.33f
 * ```
 */
open class NearJCacheStatisticsMXBean: CacheStatisticsMXBean {

    private val removals = atomic(0L)
    private val hits = atomic(0L)
    private val puts = atomic(0L)
    private val misses = atomic(0L)
    private val evictions = atomic(0L)
    private val removeTime = atomic(0L)
    private val getTime = atomic(0L)
    private val putTime = atomic(0L)

    /**
     * 연결된 Cache의 statistics counter를 0으로 초기화합니다.
     */
    override fun clear() {
        removals.value = 0
        hits.value = 0
        puts.value = 0
        misses.value = 0
        evictions.value = 0

        removeTime.value = 0
        getTime.value = 0
        putTime.value = 0
    }

    open fun addHits(value: Long) {
        hits.addAndGet(value)
    }

    /**
     * cache가 만족한 get request 수입니다.
     *
     *
     * [javax.cache.Cache.containsKey] is not a get request for
     * statistics purposes.
     *
     *
     * In a caches with multiple tiered storage, a hit may be implemented as a hit
     * to the cache or to the first tier.
     *
     *
     * For an [javax.cache.processor.EntryProcessor], a hit occurs when the
     * key exists and an entry processor can be invoked against it, even if no
     * methods of [javax.cache.Cache.Entry] or
     * [javax.cache.processor.MutableEntry] are called.
     *
     * @return the number of hits
     */
    override fun getCacheHits(): Long = hits.value

    /**
     * This is a measure of cache efficiency.
     *
     *
     * It is calculated as:
     * [.getCacheHits] divided by [()][.getCacheGets] * 100.
     *
     * @return the percentage of successful hits, as a decimal e.g 75.
     */
    override fun getCacheHitPercentage(): Float {
        return when (cacheGets) {
            0L   -> 0F
            else -> (cacheHits * 100F) / cacheGets
        }
    }

    open fun addMisses(value: Long) {
        misses.addAndGet(value)
    }

    /**
     * miss는 만족되지 않은 get request입니다.
     *
     *
     * In a simple cache a miss occurs when the cache does not satisfy the request.
     *
     *
     * [javax.cache.Cache.containsKey] is not a get request for
     * statistics purposes.
     *
     *
     * For an [javax.cache.processor.EntryProcessor], a miss occurs when the
     * key does not exist and therefore an entry processor cannot be invoked
     * against it.
     *
     *
     * In a caches with multiple tiered storage, a miss may be implemented as a miss
     * to the cache or to the first tier.
     *
     *
     * In a read-through cache a miss is an absence of the key in the cache that
     * will trigger a call to a CacheLoader. So it is still a miss even though the
     * cache will load and return the value.
     *
     *
     * Refer to the implementation for precise semantics.
     *
     * @return the number of misses
     */
    override fun getCacheMisses(): Long = misses.value

    /**
     * 요청한 entry를 찾지 못한 cache access 비율을 반환합니다
     * in the cache.
     *
     *
     * This is calculated as [.getCacheMisses] divided by
     * [.getCacheGets] * 100.
     *
     * @return the percentage of accesses that failed to find anything
     */
    override fun getCacheMissPercentage(): Float {
        return when (cacheGets) {
            0L   -> 0F
            else -> (cacheMisses * 100F) / cacheGets
        }
    }

    /**
     * cache에 대한 전체 request 수입니다. 이는 다음 합과 같습니다
     * the hits and misses.
     *
     *
     * A "get" is an operation that returns the current or previous value. It does
     * not include checking for the existence of a key.
     *
     *
     * In a caches with multiple tiered storage, a gets may be implemented as a get
     * to the cache or to the first tier.
     *
     * @return the number of gets
     */
    override fun getCacheGets(): Long = hits.value + misses.value

    open fun addPuts(value: Long) {
        puts.addAndGet(value)
    }

    /**
     * cache에 대한 전체 put 수입니다.
     *
     *
     * put 직후 evict되더라도 put으로 계산합니다.
     *
     *
     * Replaces, where a put occurs which overrides an existing mapping is counted
     * as a put.
     *
     * @return the number of puts
     */
    override fun getCachePuts(): Long = puts.value

    open fun addRemovals(value: Long) {
        removals.addAndGet(value)
    }

    /**
     * cache에서 제거된 전체 removal 수입니다. eviction은 포함하지 않습니다,
     * where the cache itself initiates the removal to make space.
     *
     * @return the number of removals
     */
    override fun getCacheRemovals(): Long = removals.value

    open fun addEvictions(value: Long) {
        evictions.addAndGet(value)
    }

    /**
     * cache에서 발생한 전체 eviction 수입니다. eviction은 removal입니다
     * initiated by the cache itself to free up space. An eviction is not treated as
     * a removal and does not appear in the removal counts.
     *
     * @return the number of evictions
     */
    override fun getCacheEvictions(): Long = evictions.value

    private fun get(value: Long, timeInNanos: Long): Float {
        if (value == 0L || timeInNanos == 0L) {
            return 0F
        }
        val timeInMicrosec = TimeUnit.NANOSECONDS.toMicros(timeInNanos).toFloat()
        return timeInMicrosec / value
    }

    open fun addGetTime(value: Long) {
        getTime.addAndGet(value)
    }

    /**
     * get 실행 평균 시간입니다.
     *
     *
     * In a read-through cache the time taken to load an entry on miss is not
     * included in get time.
     *
     * @return the time in µs
     */
    override fun getAverageGetTime(): Float = get(cacheGets, getTime.value)

    open fun addPutTime(value: Long) {
        putTime.addAndGet(value)
    }

    /**
     * put 실행 평균 시간입니다.
     *
     * @return the time in µs
     */
    override fun getAveragePutTime(): Float = get(cachePuts, putTime.value)

    open fun addRemoveTime(value: Long) {
        removeTime.addAndGet(value)
    }

    /**
     * remove 실행 평균 시간입니다.
     *
     * @return the time in µs
     */
    override fun getAverageRemoveTime(): Float = get(cacheRemovals, removeTime.value)
}
