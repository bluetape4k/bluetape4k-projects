package io.bluetape4k.hibernate.cache.lettuce

import io.bluetape4k.cache.nearcache.LettuceNearCache
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.warn
import org.hibernate.cache.internal.BasicCacheKeyImplementation
import org.hibernate.cache.internal.CacheKeyImplementation
import org.hibernate.cache.internal.NaturalIdCacheKey
import org.hibernate.cache.spi.support.DomainDataStorageAccess
import org.hibernate.engine.spi.SharedSessionContractImplementor
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.ObjectOutputStream
import java.io.Serializable
import java.security.MessageDigest
import java.util.Base64

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
 * - cache key canonicalization을 수행할 수 없는 식별자는 text/hashCode fallback 없이 fail-closed 처리한다.
 * - eviction 중 Redis 오류가 발생하면 예외를 호출자에게 전파하여 Hibernate가 성공으로 처리하지 않도록 한다.
 */
class LettuceNearCacheStorageAccess(
    private val regionName: String,
    private val nearCache: LettuceNearCache<Any>,
): DomainDataStorageAccess {

    companion object: KLogging() {
        private const val KEY_VERSION = "hck2"
    }

    // nearCache가 cacheName(=regionName) prefix를 Redis key에 자동으로 추가하므로
    // 여기서는 충돌 방지용 stable digest key로 정규화만 한다. (이중 prefix 방지)
    private fun cacheKey(key: Any): String = when (key) {
        is BasicCacheKeyImplementation -> digestKey(
            kind = "basic",
            entityOrRoleName = key.entityOrRoleName,
            tenantId = null,
            value = key.id,
        )

        is CacheKeyImplementation      -> digestKey(
            kind = "cache",
            entityOrRoleName = key.entityOrRoleName,
            tenantId = key.tenantId,
            value = key.id,
        )

        is NaturalIdCacheKey           -> digestKey(
            kind = "natural-id",
            entityOrRoleName = key.entityName,
            tenantId = key.tenantId,
            value = key.naturalIdValues,
        )

        else                           -> digestKey(
            kind = "raw",
            entityOrRoleName = key.javaClass.name,
            tenantId = null,
            value = key,
        )
    }

    private fun digestKey(
        kind: String,
        entityOrRoleName: String,
        tenantId: String?,
        value: Any?,
    ): String {
        val canonical = canonicalBytes(kind, entityOrRoleName, tenantId, value)
        val digest = MessageDigest.getInstance("SHA-256").digest(canonical)
        val encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
        return "$KEY_VERSION:$encoded"
    }

    private fun canonicalBytes(
        kind: String,
        entityOrRoleName: String,
        tenantId: String?,
        value: Any?,
    ): ByteArray =
        ByteArrayOutputStream().use { bytes ->
            ObjectOutputStream(bytes).use { out ->
                out.writeUTF(KEY_VERSION)
                out.writeUTF(kind)
                out.writeUTF(entityOrRoleName)
                out.writeBoolean(tenantId != null)
                tenantId?.let(out::writeUTF)
                out.writeCanonicalValue(value)
            }
            bytes.toByteArray()
        }

    private fun ObjectOutputStream.writeCanonicalValue(value: Any?) {
        when (value) {
            null            -> writeUTF("null")
            is Array<*>     -> writeObjectArray(value)
            is BooleanArray -> writePrimitiveArray("boolean[]", value.toList()) { writeBoolean(it) }
            is ByteArray    -> writeByteArray(value)
            is CharArray    -> writePrimitiveArray("char[]", value.toList()) { writeChar(it.code) }
            is DoubleArray  -> writePrimitiveArray("double[]", value.toList()) { writeDouble(it) }
            is FloatArray   -> writePrimitiveArray("float[]", value.toList()) { writeFloat(it) }
            is IntArray     -> writePrimitiveArray("int[]", value.toList()) { writeInt(it) }
            is LongArray    -> writePrimitiveArray("long[]", value.toList()) { writeLong(it) }
            is ShortArray   -> writePrimitiveArray("short[]", value.toList()) { writeShort(it.toInt()) }
            is Serializable -> writeSerializableValue(value)
            else            -> throw UnsupportedCacheKeyException(value)
        }
    }

    private fun ObjectOutputStream.writeObjectArray(values: Array<*>) {
        writeUTF("array")
        writeUTF(values.javaClass.componentType?.name ?: "java.lang.Object")
        writeInt(values.size)
        values.forEach { writeCanonicalValue(it) }
    }

    private inline fun <T> ObjectOutputStream.writePrimitiveArray(
        typeName: String,
        values: List<T>,
        writeElement: ObjectOutputStream.(T) -> Unit,
    ) {
        writeUTF("array")
        writeUTF(typeName)
        writeInt(values.size)
        values.forEach { writeElement(it) }
    }

    private fun ObjectOutputStream.writeByteArray(values: ByteArray) {
        writeUTF("array")
        writeUTF("byte[]")
        writeInt(values.size)
        write(values)
    }

    private fun ObjectOutputStream.writeSerializableValue(value: Serializable) {
        val serialized = try {
            ByteArrayOutputStream().use { bytes ->
                ObjectOutputStream(bytes).use { out -> out.writeObject(value) }
                bytes.toByteArray()
            }
        } catch (e: IOException) {
            throw UnsupportedCacheKeyException(value, e)
        }

        writeUTF("serializable")
        writeUTF(value.javaClass.name)
        writeInt(serialized.size)
        write(serialized)
    }

    /**
     * 식별자 전체 object graph를 canonical bytes로 만들 수 없을 때 사용한다.
     *
     * text/hashCode 기반 fallback은 서로 다른 식별자를 같은 cache key로 만들 수 있으므로
     * 허용하지 않는다. 호출 계층의 `runCatching`이 읽기 miss/쓰기 무시로 안전하게 폴백한다.
     */
    private class UnsupportedCacheKeyException(
        value: Any,
        cause: Throwable? = null,
    ): IllegalArgumentException(
        "Hibernate cache key value cannot be canonically serialized: ${value.javaClass.name}",
        cause,
    )

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
     * Redis 제거가 완료되지 않으면 예외를 로깅한 뒤 호출자에게 전파한다.
     * 로컬 캐시는 먼저 제거될 수 있으므로, 호출자는 eviction을 실패한 것으로 처리해야 한다.
     */
    override fun evictData(key: Any) {
        try {
            nearCache.remove(cacheKey(key))
        } catch (e: Exception) {
            log.warn(e) { "캐시 evict 실패 (region=$regionName, key=$key) → 호출자에게 전파" }
            throw e
        }
    }

    /**
     * region 전체 evict: local + Redis 모두 제거한다.
     *
     * Redis 전체 제거가 완료되지 않으면 예외를 로깅한 뒤 호출자에게 전파한다.
     * 로컬 캐시는 먼저 제거될 수 있으므로, 호출자는 eviction을 실패한 것으로 처리해야 한다.
     */
    override fun evictData() {
        try {
            nearCache.clearAll()
        } catch (e: Exception) {
            log.warn(e) { "캐시 전체 evict 실패 (region=$regionName) → 호출자에게 전파" }
            throw e
        }
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
