package io.bluetape4k.cache.nearcache.management

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import javax.cache.management.CacheStatisticsMXBean

/**
 * [NearCache]의 통계 정보를 제공하는 [CacheStatisticsMXBean] 구현체입니다.
 */
open class NearCacheStatisticsMXBean: CacheStatisticsMXBean {

    private val removals = AtomicLong()
    private val hits = AtomicLong()
    private val puts = AtomicLong()
    private val misses = AtomicLong()
    private val evictions = AtomicLong()
    private val removeTime = AtomicLong()
    private val getTime = AtomicLong()
    private val putTime = AtomicLong()

    /**
     * Clears the statistics counters to 0 for the associated Cache.
     */
    override fun clear() {
        removals.set(0)
        hits.set(0)
        puts.set(0)
        misses.set(0)
        evictions.set(0)

        removeTime.set(0)
        getTime.set(0)
        putTime.set(0)
    }

    open fun addHits(value: Long) {
        hits.addAndGet(value)
    }

    /**
     * The number of get requests that were satisfied by the cache.
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
    override fun getCacheHits(): Long = hits.get()

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
     * A miss is a get request that is not satisfied.
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
    override fun getCacheMisses(): Long = misses.get()

    /**
     * Returns the percentage of cache accesses that did not find a requested entry
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
     * The total number of requests to the cache. This will be equal to the sum of
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
    override fun getCacheGets(): Long = hits.get() + misses.get()

    open fun addPuts(value: Long) {
        puts.addAndGet(value)
    }

    /**
     * The total number of puts to the cache.
     *
     *
     * A put is counted even if it is immediately evicted.
     *
     *
     * Replaces, where a put occurs which overrides an existing mapping is counted
     * as a put.
     *
     * @return the number of puts
     */
    override fun getCachePuts(): Long = puts.get()

    open fun addRemovals(value: Long) {
        removals.addAndGet(value)
    }

    /**
     * The total number of removals from the cache. This does not include evictions,
     * where the cache itself initiates the removal to make space.
     *
     * @return the number of removals
     */
    override fun getCacheRemovals(): Long = removals.get()

    open fun addEvitions(value: Long) {
        evictions.addAndGet(value)
    }

    /**
     * The total number of evictions from the cache. An eviction is a removal
     * initiated by the cache itself to free up space. An eviction is not treated as
     * a removal and does not appear in the removal counts.
     *
     * @return the number of evictions
     */
    override fun getCacheEvictions(): Long = evictions.get()

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
     * The mean time to execute gets.
     *
     *
     * In a read-through cache the time taken to load an entry on miss is not
     * included in get time.
     *
     * @return the time in µs
     */
    override fun getAverageGetTime(): Float = get(cacheGets, getTime.get())

    open fun addPutTime(value: Long) {
        putTime.addAndGet(value)
    }

    /**
     * The mean time to execute puts.
     *
     * @return the time in µs
     */
    override fun getAveragePutTime(): Float = get(cachePuts, putTime.get())

    open fun addRemoveTime(value: Long) {
        removeTime.addAndGet(value)
    }

    /**
     * The mean time to execute removes.
     *
     * @return the time in µs
     */
    override fun getAverageRemoveTime(): Float = get(cacheRemovals, removeTime.get())
}
