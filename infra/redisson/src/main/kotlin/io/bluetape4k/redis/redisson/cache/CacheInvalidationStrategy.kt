package io.bluetape4k.redis.redisson.cache

import io.bluetape4k.support.requireNotBlank
import org.redisson.api.RMap

/**
 * 캐시 무효화 전략 인터페이스입니다.
 *
 * ## 제공 기능
 * - [invalidate]: 지정한 키 목록을 캐시에서 제거합니다.
 * - [invalidateAll]: 캐시 전체를 비웁니다.
 * - [invalidateByPattern]: glob 패턴과 일치하는 키를 모두 제거합니다.
 *
 * ## 주의사항
 * - 무효화 대상 키 타입은 제네릭 [ID]로 지정합니다.
 * - 예외 처리 정책은 구현체에 따라 다릅니다.
 *
 * ## 사용 예
 * ```kotlin
 * strategy.invalidate("u:1", "u:2")
 * // 지정 키가 캐시에서 제거됨
 *
 * strategy.invalidateByPattern("user:*")
 * // user:로 시작하는 모든 키가 제거됨
 * ```
 *
 * @param ID 캐시 키 타입
 */
interface CacheInvalidationStrategy<ID : Any> {
    /**
     * 지정한 키들을 캐시에서 제거합니다.
     *
     * @param ids 제거할 캐시 키 목록
     */
    fun invalidate(vararg ids: ID)

    /**
     * 캐시의 모든 항목을 제거합니다.
     */
    fun invalidateAll()

    /**
     * 지정한 glob 패턴과 일치하는 모든 키를 캐시에서 제거합니다.
     *
     * @param pattern 키 매칭에 사용할 glob 패턴 (예: `"user:*"`)
     */
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
class RedisCacheInvalidationStrategy<ID : Any>(
    private val cache: RMap<ID, *>,
) : CacheInvalidationStrategy<ID> {
    override fun invalidate(vararg ids: ID) {
        cache.fastRemove(*ids)
    }

    override fun invalidateAll() {
        cache.clear()
    }

    override fun invalidateByPattern(pattern: String) {
        pattern.requireNotBlank("pattern")
        val keys = cache.keySet(pattern)
        cache.fastRemove(*keys.toTypedArray())
    }
}
