package io.bluetape4k.cache.nearcache.jcache.management

import javax.cache.management.CacheStatisticsMXBean

/**
 * NearJCache wrapper의 표준 통계와 front/back tier 관찰값을 함께 노출합니다.
 *
 * capability getter가 `false`인 값은 관찰되지 않았다는 뜻이며, 실제 사건이 없었다는
 * 증거로 해석하지 않습니다.
 */
interface NearJCacheTierStatisticsMXBean: CacheStatisticsMXBean {
    fun getFrontHits(): Long
    fun getFrontMisses(): Long
    fun getBackHits(): Long
    fun getBackMisses(): Long
    fun getFrontEvictions(): Long
    fun isFrontEvictionObservationSupported(): Boolean
    fun isBulkRemovalCountSupported(): Boolean
    fun getStatisticsScope(): String
    fun getSupportedOperations(): Array<String>
    fun isBackWriteCompletionIncluded(): Boolean
}
