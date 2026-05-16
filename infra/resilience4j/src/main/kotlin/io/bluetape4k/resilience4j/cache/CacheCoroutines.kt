package io.bluetape4k.resilience4j.cache

import io.github.resilience4j.cache.Cache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@PublishedApi
internal object CacheCoroutineLocks {
    private val lock = ReentrantLock()
    private val locksByCache = WeakHashMap<Cache<*, *>, ConcurrentHashMap<Any, Mutex>>()

    fun mutexFor(cache: Cache<*, *>, key: Any): Mutex {
        val locks = lock.withLock {
            locksByCache.getOrPut(cache) { ConcurrentHashMap() }
        }
        return locks.computeIfAbsent(key) { Mutex() }
    }

    fun release(cache: Cache<*, *>, key: Any, mutex: Mutex) {
        if (mutex.isLocked) return

        lock.withLock {
            val locks = locksByCache[cache] ?: return
            locks.remove(key, mutex)
            if (locks.isEmpty()) {
                locksByCache.remove(cache)
            }
        }
    }
}

/**
 * Returns a cached value through a Resilience4j [Cache], loading it on miss.
 *
 * Contract:
 * - The upstream [Cache] interface exposes `computeIfAbsent` but no public
 *   backing JCache accessor.
 * - The suspend loader runs outside the blocking cache probe so coroutine
 *   cancellation can propagate.
 * - Use [SuspendCache] when direct JCache read/write semantics are required.
 *
 * ```kotlin
 * val cache = Cache.of(jcache)
 * val result = withCache(cache, "key") { key -> loadFromDatabase(key) }
 * ```
 *
 * @param K cache key type.
 * @param V cache value type.
 * @param cache Resilience4j cache facade.
 * @param key cache key.
 * @param loader suspend loader invoked on cache miss.
 * @return cached or loaded value.
 */
suspend inline fun <K, V> withCache(
    cache: Cache<K, V>,
    key: K,
    crossinline loader: suspend (K) -> V,
): V {
    return cache.executeSuspendFunction(key, loader)
}

/**
 * Decorates a key-based suspend loader with a Resilience4j [Cache].
 *
 * The returned function invokes [loader] only when the upstream cache facade
 * reports a miss. Coroutine cancellation is rechecked around blocking cache
 * probes and is not converted into a cache value.
 *
 * ```kotlin
 * val cache = Cache.of(jcache)
 * val loader: suspend (String) -> User = { key -> loadFromDatabase(key) }
 * val cachedLoader = cache.decorateSuspendFunction(loader)
 * val user = cachedLoader("user:1")
 * ```
 *
 * @param K cache key type.
 * @param V cache value type.
 * @param loader key-based suspend loader invoked on cache miss.
 * @return suspend function decorated with cache lookup.
 * @see executeSuspendFunction
 */
inline fun <K, V> Cache<K, V>.decorateSuspendFunction(
    crossinline loader: suspend (K) -> V,
): suspend (K) -> V = { key: K ->
    executeSuspendFunction(key, loader)
}

/**
 * Loads a value for [key] through a Resilience4j [Cache].
 *
 * Contract:
 * - Cache probes run on [Dispatchers.IO] because JCache providers may block.
 * - Concurrent misses for the same key are serialized with an internal mutex.
 * - The suspend [loader] runs outside the blocking cache probe.
 * - `CancellationException` from the coroutine context or [loader] propagates
 *   unchanged.
 * - The upstream [Cache] interface does not expose direct `containsKey`/`get`;
 *   use [SuspendCache] for strict direct JCache access.
 *
 * ```kotlin
 * val cache = Cache.of(jcache)
 * val user = cache.executeSuspendFunction("user:1") { key ->
 *     loadFromDatabase(key)
 * }
 * ```
 *
 * @param K cache key type.
 * @param V cache value type.
 * @param key cache key; must not be null.
 * @param loader key-based suspend loader invoked on cache miss.
 * @return cached or loaded value.
 */
suspend inline fun <K, V> Cache<K, V>.executeSuspendFunction(
    key: K,
    crossinline loader: suspend (K) -> V,
): V {
    val cacheKey = requireNotNull(key) { "cache key must not be null" }
    val mutex = CacheCoroutineLocks.mutexFor(this, cacheKey as Any)

    return try {
        mutex.withLock {
            // Resilience4j Cache exposes no public backing JCache accessor.
            // Keep this compatibility probe non-blocking for the suspend loader,
            // and use SuspendCache.of(jcache) when direct JCache read/write
            // semantics are required.
            val cachedValue: V? = withContext(Dispatchers.IO) {
                computeIfAbsent(cacheKey) { null }
            }
            currentCoroutineContext().ensureActive()
            if (cachedValue != null) {
                return@withLock cachedValue
            }

            val value = loader(cacheKey)
            currentCoroutineContext().ensureActive()

            if (value != null) {
                withContext(Dispatchers.IO) {
                    computeIfAbsent(cacheKey) { value }
                }
                currentCoroutineContext().ensureActive()
            }

            value
        }
    } finally {
        CacheCoroutineLocks.release(this, cacheKey as Any, mutex)
    }
}
