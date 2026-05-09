package io.bluetape4k.cache.nearcache.jcache

/**
 * JCache/SuspendCache back cache에 대한 비동기 write-behind 명령.
 *
 * [ResilientNearJCache] 및 [ResilientSuspendNearJCache]의 write queue/channel에 큐잉되어
 * consumer가 순차적으로 back cache에 반영한다.
 *
 * ```kotlin
 * val cmd: BackJCacheCommand<String, Int> = BackJCacheCommand.Put("hello", 5)
 * val removeCmd: BackJCacheCommand<String, Int> = BackJCacheCommand.Remove("hello")
 * val clearCmd: BackJCacheCommand<String, Int> = BackJCacheCommand.ClearBack()
 * ```
 *
 * @param K 키 타입
 * @param V 값 타입
 */
sealed interface BackJCacheCommand<K: Any, V: Any> {

    /**
     * 단일 키-값 쌍을 back cache에 저장한다.
     */
    data class Put<K: Any, V: Any>(val key: K, val value: V): BackJCacheCommand<K, V>

    /**
     * 여러 키-값 쌍을 back cache에 저장한다.
     */
    data class PutAll<K: Any, V: Any>(val entries: Map<K, V>): BackJCacheCommand<K, V>

    /**
     * 단일 키를 back cache에서 삭제한다.
     */
    data class Remove<K: Any, V: Any>(val key: K): BackJCacheCommand<K, V>

    /**
     * 여러 키를 back cache에서 삭제한다.
     */
    data class RemoveAll<K: Any, V: Any>(val keys: Set<K>): BackJCacheCommand<K, V>

    /**
     * back cache의 모든 항목을 삭제한다.
     */
    class ClearBack<K: Any, V: Any>: BackJCacheCommand<K, V>
}
