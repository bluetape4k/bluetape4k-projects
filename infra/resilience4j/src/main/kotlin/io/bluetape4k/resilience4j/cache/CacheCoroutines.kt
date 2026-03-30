package io.bluetape4k.resilience4j.cache

import io.github.resilience4j.cache.Cache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Collections
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap

@PublishedApi
internal object CacheCoroutineLocks {
    private val locksByCache =
        Collections.synchronizedMap(WeakHashMap<Cache<*, *>, ConcurrentHashMap<Any, Mutex>>())

    fun mutexFor(cache: Cache<*, *>, key: Any): Mutex {
        val locks = synchronized(locksByCache) {
            locksByCache.getOrPut(cache) { ConcurrentHashMap() }
        }
        return locks.computeIfAbsent(key) { Mutex() }
    }

    fun release(cache: Cache<*, *>, key: Any, mutex: Mutex) {
        if (mutex.isLocked) return

        synchronized(locksByCache) {
            val locks = locksByCache[cache] ?: return
            locks.remove(key, mutex)
            if (locks.isEmpty()) {
                locksByCache.remove(cache)
            }
        }
    }
}

/**
 * Resilience4j [Cache] 를 사용하여 캐시된 값을 반환합니다.
 *
 * 캐시에 값이 없을 경우 [loader]를 실행하여 값을 로드하고 캐시에 저장합니다.
 *
 * ```kotlin
 * val cache = Cache.of(jcache)
 * val result = withCache(cache, "key") { key -> loadFromDatabase(key) }
 * ```
 *
 * @param K 캐시 키 타입
 * @param V 캐시 값 타입
 * @param cache Resilience4j 캐시 객체
 * @param key 캐시 키
 * @param loader 캐시 미스 시 호출할 로더
 * @return 캐시된 값 또는 로드된 값
 */
suspend inline fun <K, V> withCache(
    cache: Cache<K, V>,
    key: K,
    crossinline loader: suspend (K) -> V,
): V {
    return cache.executeSuspendFunction(key, loader)
}

/**
 * [Cache]를 Coroutine 환경에서 사용할 수 있도록 데코레이터 함수를 생성합니다.
 *
 * 반환된 함수는 캐시 미스 시에만 원본 [loader]를 실행합니다.
 *
 * ```kotlin
 * val cache = Cache.of(jcache)
 * val loader: suspend (String) -> User = { key -> loadFromDatabase(key) }
 * val cachedLoader = cache.decorateSuspendFunction(loader)
 * val user = cachedLoader("user:1")
 * ```
 *
 * @param K 캐시 키 타입
 * @param V 캐시 값 타입
 * @param loader 캐시 미스 시 실행할 key 기반 suspend 함수
 * @return 캐시가 적용된 suspend 함수
 * @see executeSuspendFunction
 */
inline fun <K, V> Cache<K, V>.decorateSuspendFunction(
    crossinline loader: suspend (K) -> V,
): suspend (K) -> V = { key: K ->
    executeSuspendFunction(key, loader)
}

/**
 * [Cache]를 사용하여 지정된 키로 값을 로드합니다.
 *
 * JCache 접근은 블로킹 I/O이므로 [Dispatchers.IO]에서 실행합니다.
 * 캐시 히트 시 [loader]를 호출하지 않고 캐시된 값을 반환합니다.
 * 캐시 미스 시 [loader]를 실행하고 결과를 캐시에 저장합니다.
 * 동일 key의 동시 cache miss는 내부 mutex로 직렬화하여 loader 중복 실행을 방지합니다.
 * JCache 접근 중 예외가 발생하면 miss로 숨기지 않고 호출자에게 그대로 전파합니다.
 *
 * ```kotlin
 * val cache = Cache.of(jcache)
 * val user = cache.executeSuspendFunction("user:1") { key ->
 *     loadFromDatabase(key)
 * }
 * ```
 *
 * @param K 캐시 키 타입
 * @param V 캐시 값 타입
 * @param key 캐시 키
 * @param loader 캐시 미스 시 실행할 key 기반 suspend 함수
 * @return 캐시된 값 또는 로드된 값
 */
suspend inline fun <K, V> Cache<K, V>.executeSuspendFunction(
    key: K,
    crossinline loader: suspend (K) -> V,
): V {
    val cacheKey = requireNotNull(key) { "cache key must not be null" }
    val mutex = CacheCoroutineLocks.mutexFor(this, cacheKey as Any)

    return try {
        mutex.withLock {
            // JCache 접근은 블로킹 I/O - Dispatchers.IO로 offload
            val cachedValue: V? = withContext(Dispatchers.IO) {
                computeIfAbsent(cacheKey) { null }
            }
            if (cachedValue != null) {
                return@withLock cachedValue
            }

            // 캐시 미스: loader 실행
            val value = loader(cacheKey)

            // 로드된 값을 캐시에 저장 (null이 아닌 경우만)
            if (value != null) {
                withContext(Dispatchers.IO) {
                    computeIfAbsent(cacheKey) { value }
                }
            }

            value
        }
    } finally {
        CacheCoroutineLocks.release(this, cacheKey as Any, mutex)
    }
}
