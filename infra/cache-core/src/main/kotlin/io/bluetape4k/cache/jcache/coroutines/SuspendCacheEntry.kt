package io.bluetape4k.cache.jcache.coroutines

import java.io.Serializable
import javax.cache.Cache

/**
 * [SuspendCache]의 entry를 표현합니다.
 *
 * @param K key type
 * @param V value type
 * @property key cache key
 * @property value cache value
 */
data class SuspendCacheEntry<K: Any, V: Any>(
    private val entryKey: K,
    private val entryValue: V,
): Cache.Entry<K, V>, Serializable {

    override fun getKey(): K = entryKey
    override fun getValue(): V = entryValue

    override fun <T: Any> unwrap(clazz: Class<T>): T? = when {
        clazz.isAssignableFrom(javaClass) -> clazz.cast(this)
        else -> null
    }
}
