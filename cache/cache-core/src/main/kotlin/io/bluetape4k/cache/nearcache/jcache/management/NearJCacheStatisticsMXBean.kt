package io.bluetape4k.cache.nearcache.jcache.management

/**
 * NearJCache의 표준 통계와 tier별 관찰값을 같은 generation에서 제공합니다.
 *
 * public no-arg constructor로 만든 standalone bean은 active recorder를 사용합니다.
 * `clear()`는 cache data가 아니라 logical/tier counter generation만 교체합니다.
 */
@Suppress("TooManyFunctions")
open class NearJCacheStatisticsMXBean private constructor(
    private val recorder: NearJCacheStatisticsRecorder,
): NearJCacheTierStatisticsMXBean {

    constructor() : this(ActiveNearJCacheStatisticsRecorder())

    protected constructor(noOp: Unit) : this(noOp.let { NoOpNearJCacheStatisticsRecorder })

    companion object {
        private const val STATISTICS_SCOPE = "NEAR_JCACHE_WRAPPER_V1"
        private const val PERCENT_MULTIPLIER = 100.0
        private const val NANOS_PER_MICROSECOND = 1_000.0
        private val SUPPORTED_OPERATIONS = arrayOf(
            "get",
            "getAll",
            "put",
            "putAll",
            "putIfAbsent",
            "replace",
            "remove",
            "getAndPut",
            "getAndReplace",
            "getAndRemove",
        )

        @JvmSynthetic
        internal fun fromRecorder(recorder: NearJCacheStatisticsRecorder): NearJCacheStatisticsMXBean =
            NearJCacheStatisticsMXBean(recorder)
    }

    override fun clear() = recorder.clear()

    open fun addHits(value: Long) = recorder.addHits(value)
    open fun addMisses(value: Long) = recorder.addMisses(value)
    open fun addPuts(value: Long) = recorder.addPuts(value)
    open fun addRemovals(value: Long) = recorder.addRemovals(value)
    open fun addEvictions(value: Long) = recorder.addEvictions(value)
    open fun addGetTime(value: Long) = recorder.addGetTime(value)
    open fun addPutTime(value: Long) = recorder.addPutTime(value)
    open fun addRemoveTime(value: Long) = recorder.addRemoveTime(value)

    override fun getCacheHits(): Long = recorder.current().cacheHits
    override fun getCacheMisses(): Long = recorder.current().cacheMisses
    override fun getCacheGets(): Long = recorder.current().cacheGets
    override fun getCachePuts(): Long = recorder.current().cachePuts
    override fun getCacheRemovals(): Long = recorder.current().cacheRemovals
    override fun getCacheEvictions(): Long = recorder.current().cacheEvictions

    override fun getCacheHitPercentage(): Float {
        val generation = recorder.current()
        return percentage(generation.cacheHits, generation.cacheGets)
    }

    override fun getCacheMissPercentage(): Float {
        val generation = recorder.current()
        return percentage(generation.cacheMisses, generation.cacheGets)
    }

    override fun getAverageGetTime(): Float {
        val generation = recorder.current()
        return averageMicroseconds(generation.totalGetTimeNanos, generation.cacheGets)
    }

    override fun getAveragePutTime(): Float {
        val generation = recorder.current()
        return averageMicroseconds(generation.totalPutTimeNanos, generation.cachePuts)
    }

    override fun getAverageRemoveTime(): Float {
        val generation = recorder.current()
        return averageMicroseconds(generation.totalRemoveTimeNanos, generation.cacheRemovals)
    }

    override fun getFrontHits(): Long = recorder.current().frontHits
    override fun getFrontMisses(): Long = recorder.current().frontMisses
    override fun getBackHits(): Long = recorder.current().backHits
    override fun getBackMisses(): Long = recorder.current().backMisses
    override fun getFrontEvictions(): Long = 0L
    override fun isFrontEvictionObservationSupported(): Boolean = false
    override fun isBulkRemovalCountSupported(): Boolean = false
    override fun getStatisticsScope(): String = STATISTICS_SCOPE
    override fun getSupportedOperations(): Array<String> = SUPPORTED_OPERATIONS.copyOf()
    override fun isBackWriteCompletionIncluded(): Boolean = false

    private fun percentage(value: Long, count: Long): Float =
        if (count == 0L) 0F else (value.toDouble() * PERCENT_MULTIPLIER / count).toFloat()

    private fun averageMicroseconds(totalNanos: Long, count: Long): Float =
        if (count == 0L) 0F else (totalNanos.toDouble() / count / NANOS_PER_MICROSECOND).toFloat()
}
