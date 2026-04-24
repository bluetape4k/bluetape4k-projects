package io.bluetape4k.hibernate.cache.lettuce

import io.bluetape4k.cache.nearcache.LettuceNearCache
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.warn
import org.hibernate.cache.internal.BasicCacheKeyImplementation
import org.hibernate.cache.internal.CacheKeyImplementation
import org.hibernate.cache.internal.NaturalIdCacheKey
import org.hibernate.cache.spi.support.DomainDataStorageAccess
import org.hibernate.engine.spi.SharedSessionContractImplementor

/**
 * [DomainDataStorageAccess] 구현체.
 *
 * [LettuceNearCache]를 래핑하여 Hibernate 2nd level cache 브릿지 역할을 한다.
 * Region 격리는 nearCache의 cacheName(=regionName) prefix가 담당한다.
 * Redis 실제 key: `{regionName}:{entityKey}`
 *
 * - [getFromCache]: Caffeine(L1) → Redis(L2) 순서로 조회.
 *   L2(Redis) 장애 시 예외를 로깅하고 null을 반환하여 Hibernate가 DB로 폴백할 수 있도록 한다.
 * - [putIntoCache]: write-through (L1 + L2 동시 저장).
 *   L2 장애 시 예외를 로깅하고 무시한다 (Hibernate 트랜잭션에 영향 없음).
 * - [evictData]: region 전체 evict 시 local + Redis 모두 제거
 * - [evictData] with key: 특정 key만 L1+L2 제거
 */
class LettuceNearCacheStorageAccess(
    private val regionName: String,
    private val nearCache: LettuceNearCache<Any>,
): DomainDataStorageAccess {

    companion object: KLogging()

    // nearCache가 cacheName(=regionName) prefix를 Redis key에 자동으로 추가하므로
    // 여기서는 key를 stable string으로 정규화만 한다. (이중 prefix 방지)
    private fun cacheKey(key: Any): String = when (key) {
        is BasicCacheKeyImplementation -> "${key.entityOrRoleName}#${canonicalString(key.id)}"
        is CacheKeyImplementation      -> "${key.entityOrRoleName}#${canonicalString(key.id)}"
        is NaturalIdCacheKey           -> buildString {
            append(key.entityName)
            append("##NaturalId[")
            append(canonicalNaturalIdValues(key.naturalIdValues))
            append("]")
        }
        else                           -> canonicalString(key)
    }

    private fun canonicalNaturalIdValues(values: Any?): String = when (values) {
        is Array<*>     -> values.joinToString(", ") { canonicalString(it) }
        is BooleanArray -> values.joinToString(", ")
        is ByteArray    -> values.joinToString(", ")
        is CharArray    -> values.joinToString(", ")
        is DoubleArray  -> values.joinToString(", ")
        is FloatArray   -> values.joinToString(", ")
        is IntArray     -> values.joinToString(", ")
        is LongArray    -> values.joinToString(", ")
        is ShortArray   -> values.joinToString(", ")
        else            -> canonicalString(values)
    }

    private fun canonicalString(value: Any?): String = when (value) {
        null            -> "null"
        is Array<*>     -> value.joinToString(prefix = "[", postfix = "]") { canonicalString(it) }
        is BooleanArray -> value.joinToString(prefix = "[", postfix = "]")
        is ByteArray    -> value.joinToString(prefix = "[", postfix = "]")
        is CharArray    -> value.joinToString(prefix = "[", postfix = "]")
        is DoubleArray  -> value.joinToString(prefix = "[", postfix = "]")
        is FloatArray   -> value.joinToString(prefix = "[", postfix = "]")
        is IntArray     -> value.joinToString(prefix = "[", postfix = "]")
        is LongArray    -> value.joinToString(prefix = "[", postfix = "]")
        is ShortArray   -> value.joinToString(prefix = "[", postfix = "]")
        else            -> value.toString()
    }

    /**
     * 캐시에서 값을 조회한다. Caffeine(L1) miss 시 Redis(L2)를 조회한다.
     *
     * Redis 장애 등 L2 예외 발생 시 예외를 로깅하고 null을 반환하여
     * Hibernate가 DB 폴백을 수행할 수 있도록 한다.
     */
    override fun getFromCache(key: Any, session: SharedSessionContractImplementor): Any? =
        runCatching { nearCache.get(cacheKey(key)) }
            .onFailure { e -> log.warn(e) { "캐시 조회 실패 (region=$regionName, key=$key) → null 반환" } }
            .getOrNull()

    /**
     * 캐시에 값을 저장한다 (write-through: L1 + L2 동시 저장).
     *
     * Redis 장애 등 L2 예외 발생 시 예외를 로깅하고 무시한다.
     * Hibernate 트랜잭션에 영향을 주지 않는다.
     */
    override fun putIntoCache(key: Any, value: Any, session: SharedSessionContractImplementor) {
        runCatching { nearCache.put(cacheKey(key), value) }
            .onFailure { e -> log.warn(e) { "캐시 저장 실패 (region=$regionName, key=$key) → 무시" } }
    }

    /**
     * 캐시에 해당 키가 존재하는지 확인한다.
     *
     * Redis 장애 등 예외 발생 시 false를 반환한다.
     * false를 반환하면 Hibernate는 DB를 통해 엔티티를 로드하므로, 예외를 전파하는 것보다
     * 안전하게 폴백 동작을 유도할 수 있다.
     */
    override fun contains(key: Any): Boolean =
        runCatching { nearCache.containsKey(cacheKey(key)) }
            .onFailure { e -> log.warn(e) { "캐시 containsKey 실패 (region=$regionName, key=$key) → false 반환" } }
            .getOrDefault(false)

    /**
     * 특정 키를 캐시(L1+L2)에서 제거한다.
     *
     * Redis 장애 등 예외 발생 시 예외를 로깅하고 무시한다.
     */
    override fun evictData(key: Any) {
        runCatching { nearCache.remove(cacheKey(key)) }
            .onFailure { e -> log.warn(e) { "캐시 evict 실패 (region=$regionName, key=$key) → 무시" } }
    }

    /**
     * region 전체 evict: local + Redis 모두 제거한다.
     *
     * Redis 장애 등 예외 발생 시 예외를 로깅하고 무시한다.
     */
    override fun evictData() {
        runCatching { nearCache.clearAll() }
            .onFailure { e -> log.warn(e) { "캐시 전체 evict 실패 (region=$regionName) → 무시" } }
    }

    /**
     * [LettuceNearCache] 인스턴스의 수명은 [LettuceNearCacheRegionFactory]가 관리한다.
     *
     * Hibernate가 region access 단위를 정리하더라도, 공유 cache를 여기서 닫으면
     * 같은 region을 재사용하는 다른 access 인스턴스가 즉시 깨질 수 있으므로 no-op으로 둔다.
     */
    override fun release() {
        // no-op: RegionFactory가 공유 near cache lifecycle을 관리한다.
    }
}
