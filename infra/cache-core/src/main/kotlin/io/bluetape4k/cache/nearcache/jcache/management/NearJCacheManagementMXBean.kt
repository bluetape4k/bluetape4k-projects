package io.bluetape4k.cache.nearcache.jcache.management

import io.bluetape4k.cache.nearcache.jcache.NearJCache
import javax.cache.management.CacheMXBean

/**
 * [NearJCache]에 대한 [CacheMXBean] 구현체입니다.
 *
 * NOTE: Configuration 정보를 얻기 위한 getConfiguration 에서 CapturedType 문제가 있어
 * 현재는 안전한 기본값을 반환합니다. 추후 Java 클래스로 재작성하여 완전히 구현해야 합니다.
 */
class NearJCacheManagementMXBean(
    private val cache: NearJCache<*, *>,
): CacheMXBean {
    /**
     * Determines the required type of keys for this [Cache], if any.
     *
     * @return the fully qualified class name of the key type,
     * or "java.lang.Object" if the type is undefined.
     */
    override fun getKeyType(): String = "java.lang.Object"

    /**
     * Determines the required type of values for this [Cache], if any.
     * @return the fully qualified class name of the value type,
     * or "java.lang.Object" if the type is undefined.
     */
    override fun getValueType(): String = "java.lang.Object"

    /**
     * Determines if a [Cache] should operate in read-through mode.
     *
     * The default value is `false`.
     *
     * @return `true` when a [Cache] is in "read-through" mode.
     */
    override fun isReadThrough(): Boolean = false

    /**
     * Determines if a [Cache] should operate in "write-through" mode.
     *
     * The default value is `false`.
     *
     * @return `true` when a [Cache] is in "write-through" mode.
     */
    override fun isWriteThrough(): Boolean = false

    /**
     * Whether storeByValue (true) or storeByReference (false).
     *
     * The default value is `true`.
     *
     * @return true if the cache is store by value
     */
    override fun isStoreByValue(): Boolean = true

    /**
     * Checks whether statistics collection is enabled in this cache.
     *
     * The default value is `false`.
     *
     * @return true if statistics collection is enabled
     */
    override fun isStatisticsEnabled(): Boolean = false

    /**
     * Checks whether management is enabled on this cache.
     *
     * The default value is `false`.
     *
     * @return true if management is enabled
     */
    override fun isManagementEnabled(): Boolean = false
}
