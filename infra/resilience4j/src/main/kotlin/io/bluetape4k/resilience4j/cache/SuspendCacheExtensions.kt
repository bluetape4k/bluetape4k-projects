package io.bluetape4k.resilience4j.cache

/**
 * [SuspendCache] 를 사용하여 캐시된 값을 반환합니다.
 *
 * 캐시에 값이 없을 경우 [loader]를 실행하여 값을 로드하고 캐시에 저장합니다.
 *
 * ```kotlin
 * val suspendCache = SuspendCache.of(jcache)
 * val result = withSuspendCache(suspendCache, "key") {
 *     // 캐시에 없을 때 실행
 *     loadFromDatabase()
 * }
 * ```
 *
 * @param K 캐시 키 타입
 * @param V 캐시 값 타입
 * @param cache 캐시 객체
 * @param cacheKey 캐시 키
 * @param loader 캐시에 없을 시에 호출할 로더
 * @return 캐시된 값 또는 로드된 값
 */
suspend fun <K, V> withSuspendCache(
    cache: SuspendCache<K, V>,
    cacheKey: K,
    @BuilderInference loader: suspend () -> V,
): V {
    return cache.computeIfAbsent(cacheKey, loader)
}

/**
 * [SuspendCache]를 사용하여 keyless suspend supplier를 키 기반 캐시 함수로 변환합니다.
 *
 * 반환된 함수는 캐시 미스 시에만 원본 [loader]를 실행합니다.
 *
 * ```kotlin
 * val cache = SuspendCache.of(jcache)
 * val loader: suspend () -> User = { loadFromDatabase() }
 * val cachedLoader: suspend (String) -> User = cache.decorateSuspendSupplier(loader)
 * val user = cachedLoader("user:1")
 * ```
 *
 * @param K 캐시 키 타입
 * @param V 캐시 값 타입
 * @param loader 캐시 미스 시 실행할 suspend supplier
 * @return 캐시가 적용된 suspend 함수
 */
inline fun <K, V> SuspendCache<K, V>.decorateSuspendSupplier(
    @BuilderInference crossinline loader: suspend () -> V,
): suspend (K) -> V = { cacheKey: K ->
    executeSuspendFunction(cacheKey, loader)
}

/**
 * [SuspendCache]를 사용하여 key 기반 suspend loader를 캐시 함수로 변환합니다.
 *
 * 반환된 함수는 캐시 미스 시에만 원본 [loader]를 실행합니다.
 *
 * ```kotlin
 * val cache = SuspendCache.of(jcache)
 * val loader: suspend (String) -> User = { key -> loadFromDatabase(key) }
 * val cachedLoader: suspend (String) -> User = cache.decorateSuspendFunction(loader)
 * val user = cachedLoader("user:1")
 * ```
 *
 * @param K 캐시 키 타입
 * @param V 캐시 값 타입
 * @param loader 캐시 미스 시 실행할 key 기반 suspend 함수
 * @return 캐시가 적용된 suspend 함수
 */
inline fun <K, V> SuspendCache<K, V>.decorateSuspendFunction(
    @BuilderInference crossinline loader: suspend (K) -> V,
): suspend (K) -> V = { cacheKey: K ->
    executeSuspendFunction(cacheKey) { loader(cacheKey) }
}

/**
 * [SuspendCache]를 사용하여 지정된 키로 값을 로드합니다.
 *
 * 캐시 히트 시 [loader]를 호출하지 않고 캐시된 값을 반환합니다.
 *
 * ```kotlin
 * val cache = SuspendCache.of(jcache)
 * val user = cache.executeSuspendFunction("user:1") {
 *     loadFromDatabase()
 * }
 * ```
 *
 * @param K 캐시 키 타입
 * @param V 캐시 값 타입
 * @param cacheKey 캐시 키
 * @param loader 캐시 미스 시 실행할 suspend supplier
 * @return 캐시된 값 또는 로드된 값
 */
suspend inline fun <K, V> SuspendCache<K, V>.executeSuspendFunction(
    cacheKey: K,
    @BuilderInference crossinline loader: suspend () -> V,
): V {
    return computeIfAbsent(cacheKey) { loader() }
}
