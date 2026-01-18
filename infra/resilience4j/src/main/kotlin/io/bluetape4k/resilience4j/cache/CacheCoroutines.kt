package io.bluetape4k.resilience4j.cache

import io.github.resilience4j.cache.Cache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Resilience4j [Cache] 를 사용하여 캐시된 값을 반환합니다.
 *
 * ```
 * val cache = Cache.of(jcache)
 * val result = withCache(cache, key) { ... }
 * ```
 *
 * @param cache 캐시 객체
 * @param key 캐시 키
 * @param loader 캐시에 없을 시에 호출할 로더
 */
suspend inline fun <K, V> withCache(
    cache: Cache<K, V>,
    key: K,
    crossinline loader: suspend (K) -> V,
): V {
    return cache.executeSuspendFunction(key, loader)
}

/**
 * [Cache]를 Coroutine 환경에서 값을 [loader]로 로딩합니다.
 *
 * ```
 * val cache = Cache.of(jcache)
 * val loader: suspend (K) -> V = { key: K -> ... }
 * val cachedLoader = cache.decorateSuspendedFunction(loader)
 * val result = cachedLoader(key)
 * ```
 *
 * @param loader 캐시에 저장할 로더
 * @return 캐시된 값을 반환하는 함수
 * @see executeSuspendFunction
 */
inline fun <K, V> Cache<K, V>.decorateSuspendFunction(
    crossinline loader: suspend (K) -> V,
): suspend (K) -> V = { key: K ->
    executeSuspendFunction(key, loader)
}

/**
 * [Cache]를 Coroutine 환경에서 값을 [loader]를 실행하여 로딩합니다.
 *
 * ```
 * val cache = Cache.of(jcache)
 * val loader: suspend (K) -> V = { key: K -> ... }
 * val cachedLoader = cache.decorateSuspendedFunction(loader)
 * val result = cachedLoader(key)
 * ```
 *
 * @param key 캐시 키
 * @param loader 캐시에 저장할 로더
 * @return 캐시된 값
 */
suspend inline fun <K, V> Cache<K, V>.executeSuspendFunction(
    key: K,
    crossinline loader: suspend (K) -> V,
): V = suspendCancellableCoroutine { cont ->

    val cachedValue = runCatching { computeIfAbsent(key!!) { null } }.getOrNull()
    if (cachedValue != null) {
        cont.resume(cachedValue)
    } else {
        // Load cache value
        val result = runCatching { runBlocking { withContext(Dispatchers.IO) { loader(key) } } }
        if (result.isSuccess) {
            if (result.getOrNull() != null) {
                this.computeIfAbsent(key!!) { result.getOrNull() }
            }
            cont.resumeWith(result)
        } else {
            cont.resumeWithException(result.exceptionOrNull()!!)
        }
    }
}
