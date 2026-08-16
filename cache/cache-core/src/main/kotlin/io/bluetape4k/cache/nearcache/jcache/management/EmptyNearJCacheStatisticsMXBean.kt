package io.bluetape4k.cache.nearcache.jcache.management

/**
 * 통계를 수집하지 않는 기존 호환용 no-op bean입니다.
 *
 * `addRemovals`를 포함한 legacy mutator, `clear()`, 표준/tier getter는 singleton
 * no-op recorder를 사용하므로 clock과 counter를 만들거나 갱신하지 않습니다.
 */
class EmptyNearJCacheStatisticsMXBean:
    NearJCacheStatisticsMXBean(Unit)
