package io.bluetape4k.resilience4j.cache

/**
 * [SuspendCache] 를 사용하여 캐시된 값을 반환합니다.
 *
 * @param cache    캐시 객체
 * @param cacheKey 캐시 키
 * @param loader   캐시에 없을 시에 호출할 로더
 */
suspend fun <K, V> withCaache(
    cache: SuspendCache<K, V>,
    cacheKey: K,
    @BuilderInference loader: suspend () -> V,
): V {
    return cache.computeIfAbsent(cacheKey, loader)
}

/**
 * [SuspendCache]를 Coroutine 환경에서 값을 [loader]로 로딩합니다.
 *
 * ```
 * val cache = CoCache.of(jcache)
 * val loader: suspend (K) -> V = { key: K -> ... }
 * val cachedLoader = cache.decorateSuspendedFunction(loader)
 * val result = cachedLoader(key)
 * ```
 *
 * @param loader 캐시에 저장할 로더
 */
fun <K, V> SuspendCache<K, V>.decorateSuspendSupplier(
    @BuilderInference loader: suspend () -> V,
): suspend (K) -> V = { cacheKey: K ->
    executeSuspendFunction(cacheKey, loader)
}

/**
 * [SuspendCache]를 Coroutine 환경에서 값을 [loader]를 실행하여 로딩합니다.
 *
 * ```
 * val cache = CoCache.of(jcache)
 * val loader: suspend (K) -> V = { key: K -> ... }
 * val cachedLoader = cache.decorateSuspendFunction(loader)
 * val result = cachedLoader(key)
 * ```
 *
 * @param loader 캐시에 저장할 로더
 */
inline fun <K, V> SuspendCache<K, V>.decorateSuspendFunction(
    @BuilderInference crossinline loader: suspend (K) -> V,
): suspend (K) -> V = { cacheKey: K ->
    executeSuspendFunction(cacheKey) { loader(cacheKey) }
}

/**
 * [SuspendCache]를 Coroutine 환경에서 값을 [loader]로 로딩합니다.
 *
 * ```
 * val cache = CoCache.of(jcache)
 * val loader: suspend (K) -> V = { key: K -> ... }
 * val result = cache.executeSuspendFunction(key, loader)
 * ```
 *
 * @param cacheKey 캐시 키
 * @param loader 캐시에 저장할 로더
 */
suspend inline fun <K, V> SuspendCache<K, V>.executeSuspendFunction(
    cacheKey: K,
    @BuilderInference crossinline loader: suspend () -> V,
): V {
    return computeIfAbsent(cacheKey) { loader() }
}
