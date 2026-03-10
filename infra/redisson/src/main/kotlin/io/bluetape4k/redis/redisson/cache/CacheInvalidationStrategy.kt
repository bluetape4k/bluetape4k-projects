package io.bluetape4k.redis.redisson.cache

import org.redisson.api.RMap

/**
 * 캐시 무효화 전략 계약입니다.
 *
 * ## 동작/계약
 * - 선택 키 무효화([invalidate]), 전체 무효화([invalidateAll]), 패턴 기반 무효화([invalidateByPattern])를 제공합니다.
 * - 무효화 대상 키 형식은 제네릭 [ID] 타입에 따릅니다.
 * - 예외 처리 정책은 구현체에 위임됩니다.
 *
 * ```kotlin
 * strategy.invalidate("u:1", "u:2")
 * // 지정 키가 캐시에서 제거됨
 * ```
 */
interface CacheInvalidationStrategy<ID: Any> {
    fun invalidate(vararg ids: ID)
    fun invalidateAll()
    fun invalidateByPattern(pattern: String)
}

/**
 * Redisson [RMap] 기반 [CacheInvalidationStrategy] 구현체입니다.
 *
 * ## 동작/계약
 * - [invalidate]는 `fastRemove`를 사용해 지정 키를 빠르게 제거합니다.
 * - [invalidateAll]은 `clear()`로 전체 엔트리를 제거합니다.
 * - [invalidateByPattern]은 `keySet(pattern)` 조회 후 일치 키를 일괄 제거합니다.
 *
 * ```kotlin
 * val strategy = RedisCacheInvalidationStrategy(cacheMap)
 * strategy.invalidateByPattern("user:*")
 * // user:* 패턴 키가 제거됨
 * ```
 */
class RedisCacheInvalidationStrategy<ID: Any>(
    private val cache: RMap<ID, *>,
): CacheInvalidationStrategy<ID> {

    override fun invalidate(vararg ids: ID) {
        cache.fastRemove(*ids)
    }

    override fun invalidateAll() {
        cache.clear()
    }

    override fun invalidateByPattern(pattern: String) {
        val keys = cache.keySet(pattern)
        cache.fastRemove(*keys.toTypedArray())
    }
}
