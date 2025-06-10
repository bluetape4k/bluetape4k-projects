package io.bluetape4k.cache.jcache.coroutines

import java.io.Serializable
import javax.cache.Cache

/**
 * [CoCache]의 entry를 표현합니다.
 *
 * @param K key type
 * @param V value type
 * @property key cache key
 * @property value cache value
 */
@Deprecated("Use SuspendCacheEntry instead", ReplaceWith("SuspendCacheEntry"))
data class CoCacheEntry<K: Any, V: Any>(
    private val key: K,
    private val value: V,
): Cache.Entry<K, V>, Serializable {

    override fun getKey(): K = key
    override fun getValue(): V = value

    override fun <T: Any> unwrap(clazz: Class<T>): T? = when {
        clazz.isAssignableFrom(javaClass) -> clazz.cast(this)
        else                              -> null
    }
}
