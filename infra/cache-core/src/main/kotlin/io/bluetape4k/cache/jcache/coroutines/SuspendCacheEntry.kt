package io.bluetape4k.cache.jcache.coroutines

import java.io.Serializable
import javax.cache.Cache

/**
 * [SuspendCache] 엔트리를 표현하는 불변 값 객체입니다.
 *
 * ## 동작/계약
 * - 생성 시 전달된 key/value를 그대로 보관하며 이후 변경하지 않습니다.
 * - [Cache.Entry] 계약을 구현해 JCache API와 상호 운용됩니다.
 * - `unwrap`은 요청 타입이 현재 클래스와 호환될 때만 인스턴스를 반환합니다.
 *
 * ```kotlin
 * val entry = SuspendCacheEntry("u:1", 10)
 * // entry.key == "u:1"
 * // entry.value == 10
 * ```
 */
data class SuspendCacheEntry<K: Any, V: Any>(
    private val entryKey: K,
    private val entryValue: V,
): Cache.Entry<K, V>, Serializable {

    companion object {
        @Suppress("ConstPropertyName")
        private const val serialVersionUID: Long = 1L
    }

    override fun getKey(): K = entryKey
    override fun getValue(): V = entryValue

    override fun <T: Any> unwrap(clazz: Class<T>): T? = when {
        clazz.isAssignableFrom(javaClass) -> clazz.cast(this)
        else -> null
    }
}
