package io.bluetape4k.cache.nearcache.jcache.management

/**
 * 통계를 수집하지 않는 빈(no-op) [NearJCacheStatisticsMXBean] 구현체입니다.
 *
 * 통계 수집이 불필요한 환경에서 오버헤드 없이 [NearJCacheStatisticsMXBean]을 만족시킬 때 사용합니다.
 */
class EmptyNearJCacheStatisticsMXBean: NearJCacheStatisticsMXBean() {

    override fun addHits(value: Long) {
        // Nothing to do.
    }

    override fun addMisses(value: Long) {
        // Nothing to do.
    }

    override fun addPuts(value: Long) {
        // Nothing to do.
    }

    override fun addEvictions(value: Long) {
        // Nothing to do.
    }

    override fun addGetTime(value: Long) {
        // Nothing to do.
    }

    override fun addPutTime(value: Long) {
        // Nothing to do.
    }

    override fun addRemoveTime(value: Long) {
        // Nothing to do.
    }
}
