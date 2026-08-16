package io.bluetape4k.cache.nearcache.jcache.management

import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.LongAdder

internal fun interface NearJCacheTimeSource {
    fun nanoTime(): Long
}

internal object SystemNearJCacheTimeSource: NearJCacheTimeSource {
    override fun nanoTime(): Long = System.nanoTime()
}

internal interface NearJCacheRecordingContext {
    fun startTimeNanos(): Long

    fun recordGet(
        startedAt: Long,
        hits: Long,
        misses: Long,
        frontHits: Long,
        frontMisses: Long,
        backHits: Long,
        backMisses: Long,
    )

    fun recordPut(startedAt: Long, count: Long)
    fun recordRemove(startedAt: Long, count: Long)

    val cacheHits: Long
    val cacheMisses: Long
    val cacheGets: Long
    val cachePuts: Long
    val cacheRemovals: Long
    val cacheEvictions: Long
    val frontHits: Long
    val frontMisses: Long
    val backHits: Long
    val backMisses: Long
    val totalGetTimeNanos: Long
    val totalPutTimeNanos: Long
    val totalRemoveTimeNanos: Long
}

internal interface NearJCacheStatisticsRecorder {
    fun current(): NearJCacheRecordingContext
    fun clear()
    fun addHits(value: Long)
    fun addMisses(value: Long)
    fun addPuts(value: Long)
    fun addRemovals(value: Long)
    fun addEvictions(value: Long)
    fun addGetTime(value: Long)
    fun addPutTime(value: Long)
    fun addRemoveTime(value: Long)
}

internal class ActiveNearJCacheStatisticsRecorder(
    private val timeSource: NearJCacheTimeSource = SystemNearJCacheTimeSource,
): NearJCacheStatisticsRecorder {
    private val generation = AtomicReference(ActiveGeneration(timeSource))

    override fun current(): NearJCacheRecordingContext = generation.get()

    override fun clear() {
        generation.getAndSet(ActiveGeneration(timeSource))
    }

    override fun addHits(value: Long) = generation.get().addHits(value)
    override fun addMisses(value: Long) = generation.get().addMisses(value)
    override fun addPuts(value: Long) = generation.get().addPuts(value)
    override fun addRemovals(value: Long) = generation.get().addRemovals(value)
    override fun addEvictions(value: Long) = generation.get().addEvictions(value)
    override fun addGetTime(value: Long) = generation.get().addGetTime(value)
    override fun addPutTime(value: Long) = generation.get().addPutTime(value)
    override fun addRemoveTime(value: Long) = generation.get().addRemoveTime(value)
}

internal object NoOpNearJCacheStatisticsRecorder: NearJCacheStatisticsRecorder {
    override fun current(): NearJCacheRecordingContext = NoOpRecordingContext
    override fun clear() = Unit
    override fun addHits(value: Long) = Unit
    override fun addMisses(value: Long) = Unit
    override fun addPuts(value: Long) = Unit
    override fun addRemovals(value: Long) = Unit
    override fun addEvictions(value: Long) = Unit
    override fun addGetTime(value: Long) = Unit
    override fun addPutTime(value: Long) = Unit
    override fun addRemoveTime(value: Long) = Unit
}

@Suppress("TooManyFunctions")
private class ActiveGeneration(
    private val timeSource: NearJCacheTimeSource,
): NearJCacheRecordingContext {
    private val hits = LongAdder()
    private val misses = LongAdder()
    private val puts = LongAdder()
    private val removals = LongAdder()
    private val evictions = LongAdder()
    private val observedFrontHits = LongAdder()
    private val observedFrontMisses = LongAdder()
    private val observedBackHits = LongAdder()
    private val observedBackMisses = LongAdder()
    private val getTime = LongAdder()
    private val putTime = LongAdder()
    private val removeTime = LongAdder()

    override fun startTimeNanos(): Long = timeSource.nanoTime()

    override fun recordGet(
        startedAt: Long,
        hits: Long,
        misses: Long,
        frontHits: Long,
        frontMisses: Long,
        backHits: Long,
        backMisses: Long,
    ) {
        if (hits + misses <= 0L) return
        addHits(hits)
        addMisses(misses)
        observedFrontHits.add(frontHits)
        observedFrontMisses.add(frontMisses)
        observedBackHits.add(backHits)
        observedBackMisses.add(backMisses)
        getTime.add(elapsedSince(startedAt))
    }

    override fun recordPut(startedAt: Long, count: Long) {
        if (count <= 0L) return
        addPuts(count)
        putTime.add(elapsedSince(startedAt))
    }

    override fun recordRemove(startedAt: Long, count: Long) {
        if (count <= 0L) return
        addRemovals(count)
        removeTime.add(elapsedSince(startedAt))
    }

    fun addHits(value: Long) = hits.add(value)
    fun addMisses(value: Long) = misses.add(value)
    fun addPuts(value: Long) = puts.add(value)
    fun addRemovals(value: Long) = removals.add(value)
    fun addEvictions(value: Long) = evictions.add(value)
    fun addGetTime(value: Long) = getTime.add(value)
    fun addPutTime(value: Long) = putTime.add(value)
    fun addRemoveTime(value: Long) = removeTime.add(value)

    override val cacheHits: Long get() = hits.sum()
    override val cacheMisses: Long get() = misses.sum()
    override val cacheGets: Long get() = cacheHits + cacheMisses
    override val cachePuts: Long get() = puts.sum()
    override val cacheRemovals: Long get() = removals.sum()
    override val cacheEvictions: Long get() = evictions.sum()
    override val frontHits: Long get() = observedFrontHits.sum()
    override val frontMisses: Long get() = observedFrontMisses.sum()
    override val backHits: Long get() = observedBackHits.sum()
    override val backMisses: Long get() = observedBackMisses.sum()
    override val totalGetTimeNanos: Long get() = getTime.sum()
    override val totalPutTimeNanos: Long get() = putTime.sum()
    override val totalRemoveTimeNanos: Long get() = removeTime.sum()

    private fun elapsedSince(startedAt: Long): Long = (timeSource.nanoTime() - startedAt).coerceAtLeast(0L)
}

private object NoOpRecordingContext: NearJCacheRecordingContext {
    override fun startTimeNanos(): Long = 0L
    override fun recordGet(
        startedAt: Long,
        hits: Long,
        misses: Long,
        frontHits: Long,
        frontMisses: Long,
        backHits: Long,
        backMisses: Long,
    ) = Unit
    override fun recordPut(startedAt: Long, count: Long) = Unit
    override fun recordRemove(startedAt: Long, count: Long) = Unit
    override val cacheHits: Long get() = 0L
    override val cacheMisses: Long get() = 0L
    override val cacheGets: Long get() = 0L
    override val cachePuts: Long get() = 0L
    override val cacheRemovals: Long get() = 0L
    override val cacheEvictions: Long get() = 0L
    override val frontHits: Long get() = 0L
    override val frontMisses: Long get() = 0L
    override val backHits: Long get() = 0L
    override val backMisses: Long get() = 0L
    override val totalGetTimeNanos: Long get() = 0L
    override val totalPutTimeNanos: Long get() = 0L
    override val totalRemoveTimeNanos: Long get() = 0L
}
