package io.bluetape4k.hibernate.cache.lettuce

import io.bluetape4k.cache.nearcache.LettuceNearCache
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
 * - [getFromCache]: Caffeine(L1) → Redis(L2) 순서로 조회
 * - [putIntoCache]: write-through (L1 + L2 동시 저장)
 * - [evictData]: region 전체 evict 시 local + Redis 모두 제거
 * - [evictData] with key: 특정 key만 L1+L2 제거
 */
class LettuceNearCacheStorageAccess(
    private val regionName: String,
    private val nearCache: LettuceNearCache<Any>,
): DomainDataStorageAccess {

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

    override fun getFromCache(key: Any, session: SharedSessionContractImplementor): Any? =
        nearCache.get(cacheKey(key))

    override fun putIntoCache(key: Any, value: Any, session: SharedSessionContractImplementor) {
        nearCache.put(cacheKey(key), value)
    }

    override fun contains(key: Any): Boolean =
        nearCache.containsKey(cacheKey(key))

    override fun evictData(key: Any) {
        nearCache.remove(cacheKey(key))
    }

    /**
     * region 전체 evict: local + Redis 모두 제거한다.
     */
    override fun evictData() {
        nearCache.clearAll()
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
