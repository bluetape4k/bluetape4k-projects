package io.bluetape4k.cache.jcache

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.support.requireNotBlank
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.apache.ignite.IgniteCache
import org.apache.ignite.lang.IgniteFuture
import java.util.concurrent.CompletableFuture
import javax.cache.configuration.CacheEntryListenerConfiguration
import javax.cache.configuration.Configuration
import javax.cache.configuration.MutableConfiguration
import org.apache.ignite.cache.CachingProvider as IgniteCachingProvider

/**
 * Ignite 2.x JCache를 코루틴 기반 [SuspendCache]로 감싼 구현체입니다.
 *
 * ## 동작/계약
 * - `cache.unwrap(IgniteCache::class.java)` 성공 시 Ignite 비동기 API를 우선 사용합니다.
 * - unwrap 실패 시 표준 JCache 동기 API를 `Dispatchers.IO`에서 실행합니다.
 * - `putAllFlow`는 비동기 put 작업을 수집해 `joinAll()`로 완료를 보장합니다.
 *
 * ```kotlin
 * val cache = Ignite2SuspendCache<String, String>("users")
 * cache.put("u:1", "debop")
 * val value = cache.get("u:1")
 * // value == "debop"
 * ```
 */
class IgniteSuspendCache<K: Any, V: Any>(private val cache: JCache<K, V>): SuspendCache<K, V> {

    companion object: KLoggingChannel() {
        /**
         * Ignite 연산 최대 대기 시간 (ms).
         *
         * ARM64 환경에서 [IgniteFuture.listen] 콜백이 발화하지 않아 무한 대기하는 현상 방지.
         */
        const val OPERATION_TIMEOUT_MS = 30_000L

        /**
         * 캐시 이름과 구성으로 [IgniteSuspendCache]를 생성합니다.
         *
         * ## 동작/계약
         * - [cacheName]이 blank면 `IllegalArgumentException`이 발생합니다.
         * - 동일 이름 캐시가 존재하면 재사용하고, 없으면 [configuration]으로 생성합니다.
         * - 반환 객체는 생성/조회된 JCache 인스턴스를 래핑합니다.
         *
         * ```kotlin
         * val cache = Ignite2SuspendCache("users", MutableConfiguration<String, String>())
         * // cache.isClosed() == false
         * ```
         */
        @JvmStatic
        operator fun <K: Any, V: Any> invoke(
            cacheName: String,
            configuration: Configuration<K, V> = MutableConfiguration(),
        ): IgniteSuspendCache<K, V> {
            cacheName.requireNotBlank("cacheName")
            val manager = IgniteCachingProvider().cacheManager
            val jcache = manager.getCache(cacheName, configuration.keyType, configuration.valueType)
                ?: manager.createCache(cacheName, configuration)
            return IgniteSuspendCache(jcache)
        }

        /**
         * reified 키/값 타입으로 [IgniteSuspendCache]를 생성합니다.
         *
         * ## 동작/계약
         * - [cacheName] blank 입력은 `IllegalArgumentException`을 발생시킵니다.
         * - `MutableConfiguration#setTypes`로 `K`, `V` 타입을 강제합니다.
         * - 기존 캐시가 있으면 재사용, 없으면 타입 설정으로 신규 생성합니다.
         *
         * ```kotlin
         * val cache = Ignite2SuspendCache<String, Int>("scores")
         * cache.put("u1", 10)
         * // cache.get("u1") == 10
         * ```
         */
        @JvmStatic
        inline operator fun <reified K: Any, reified V: Any> invoke(
            cacheName: String,
        ): IgniteSuspendCache<K, V> {
            cacheName.requireNotBlank("cacheName")
            val manager = IgniteCachingProvider().cacheManager
            val config = MutableConfiguration<K, V>().apply {
                setTypes(K::class.java, V::class.java)
            }
            val jcache = manager.getCache(cacheName, K::class.java, V::class.java)
                ?: manager.createCache(cacheName, config)
            return IgniteSuspendCache(jcache)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private val igniteCache: IgniteCache<K, V>? =
        runCatching { cache.unwrap(IgniteCache::class.java) as? IgniteCache<K, V> }.getOrNull()

    override fun entries(): Flow<SuspendCacheEntry<K, V>> = flow {
        cache.asSequence().forEach { emit(SuspendCacheEntry(it.key, it.value)) }
    }

    override suspend fun clear() {
        withContext(Dispatchers.IO) { cache.clear() }
    }

    override suspend fun close() {
        withContext(Dispatchers.IO) { cache.close() }
    }

    override fun isClosed(): Boolean = cache.isClosed

    override suspend fun containsKey(key: K): Boolean =
        withContext(Dispatchers.IO) { cache.containsKey(key) }

    override suspend fun get(key: K): V? =
        igniteCache?.getAsync(key)?.awaitIgnite()
            ?: withContext(Dispatchers.IO) { cache.get(key) }

    override fun getAll(): Flow<SuspendCacheEntry<K, V>> = entries()

    override fun getAll(keys: Set<K>): Flow<SuspendCacheEntry<K, V>> = flow {
        val entries =
            igniteCache?.getAllAsync(keys)?.awaitIgnite()
                ?: withContext(Dispatchers.IO) { cache.getAll(keys) }
        entries.forEach { (k, v) -> emit(SuspendCacheEntry(k, v)) }
    }

    override suspend fun getAndPut(key: K, value: V): V? =
        igniteCache?.getAndPutAsync(key, value)?.awaitIgnite()
            ?: withContext(Dispatchers.IO) { cache.getAndPut(key, value) }

    override suspend fun getAndRemove(key: K): V? =
        igniteCache?.getAndRemoveAsync(key)?.awaitIgnite()
            ?: withContext(Dispatchers.IO) { cache.getAndRemove(key) }

    override suspend fun getAndReplace(key: K, value: V): V? =
        igniteCache?.getAndReplaceAsync(key, value)?.awaitIgnite()
            ?: withContext(Dispatchers.IO) { cache.getAndReplace(key, value) }

    override suspend fun put(key: K, value: V) {
        igniteCache?.putAsync(key, value)?.awaitIgnite()
            ?: withContext(Dispatchers.IO) { cache.put(key, value) }
    }

    override suspend fun putAll(map: Map<K, V>) {
        igniteCache?.putAllAsync(map)?.awaitIgnite()
            ?: withContext(Dispatchers.IO) { cache.putAll(map) }
    }

    override suspend fun putAllFlow(entries: Flow<Pair<K, V>>) {
        if (igniteCache != null) {
            coroutineScope {
                entries.collect { (key, value) ->
                    launch { igniteCache.putAsync(key, value).awaitIgnite() }
                }
            }
            return
        }
        entries.collect { put(it.first, it.second) }
    }

    override suspend fun putIfAbsent(key: K, value: V): Boolean =
        igniteCache?.putIfAbsentAsync(key, value)?.awaitIgnite()
            ?: withContext(Dispatchers.IO) { cache.putIfAbsent(key, value) }

    override suspend fun remove(key: K): Boolean =
        igniteCache?.removeAsync(key)?.awaitIgnite()
            ?: withContext(Dispatchers.IO) { cache.remove(key) }

    override suspend fun remove(key: K, oldValue: V): Boolean =
        igniteCache?.removeAsync(key, oldValue)?.awaitIgnite()
            ?: withContext(Dispatchers.IO) { cache.remove(key, oldValue) }

    override suspend fun removeAll() {
        igniteCache?.removeAllAsync()?.awaitIgnite()
            ?: withContext(Dispatchers.IO) { cache.removeAll() }
    }

    override suspend fun removeAll(keys: Set<K>) {
        igniteCache?.removeAllAsync(keys)?.awaitIgnite()
            ?: withContext(Dispatchers.IO) { cache.removeAll(keys) }
    }

    override suspend fun replace(key: K, oldValue: V, newValue: V): Boolean =
        igniteCache?.replaceAsync(key, oldValue, newValue)?.awaitIgnite()
            ?: withContext(Dispatchers.IO) { cache.replace(key, oldValue, newValue) }

    override suspend fun replace(key: K, value: V): Boolean =
        igniteCache?.replaceAsync(key, value)?.awaitIgnite()
            ?: withContext(Dispatchers.IO) { cache.replace(key, value) }

    override fun registerCacheEntryListener(configuration: CacheEntryListenerConfiguration<K, V>) {
        cache.registerCacheEntryListener(configuration)
    }

    override fun deregisterCacheEntryListener(configuration: CacheEntryListenerConfiguration<K, V>) {
        cache.deregisterCacheEntryListener(configuration)
    }
}

private fun <T> IgniteFuture<T>.toCompletableFuture(): CompletableFuture<T> {
    val cf = CompletableFuture<T>()
    listen { f ->
        try {
            cf.complete(f.get())
        } catch (e: Throwable) {
            cf.completeExceptionally(e.cause ?: e)
        }
    }
    return cf
}

/**
 * ARM64에서 [IgniteFuture.listen] 콜백이 발화하지 않아 무한 대기하는 현상 방지를 위해
 * [IgniteSuspendCache.OPERATION_TIMEOUT_MS] 상한을 적용합니다.
 */
private suspend fun <T> IgniteFuture<T>.awaitIgnite(): T =
    withTimeout(IgniteSuspendCache.OPERATION_TIMEOUT_MS) { toCompletableFuture().await() }
