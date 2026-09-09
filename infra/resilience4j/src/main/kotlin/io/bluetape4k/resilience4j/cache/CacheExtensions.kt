package io.bluetape4k.resilience4j.cache

import io.github.resilience4j.cache.Cache
import io.bluetape4k.support.requireNotNull
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

// resilience4j 1.1.+ 에서는 function1 에 대해 cache를 제공하지 않는다.
// 이를 확장하기 위해 기능을 추가했습니다.

/**
 * [func] 실행 결과를 Resilience4j [Cache]로 decorate 한 함수를 반환합니다.
 * 만약 캐시에 없을 시에는 [func]를 실행하여 결과를 캐시에 저장하고 반환합니다.
 *
 * ```
 * val cache = Cache.of(jcache)
 * val cachedLoader = cache.decorateFunction { key: K -> ... }
 * val result = cachedLoader(key)
 * ```
 *
 * @param func 실행할 함수
 */
inline fun <K, V> Cache<K, V>.decorateFunction(
    crossinline func: (K) -> V,
): (K) -> V = { key: K ->
    val validKey = key.requireNotNull("key")
    this.computeIfAbsent(validKey) { func(validKey) }
}

/**
 * 함수 실행 결과를 Resilience4j [Cache]로 decorate 한 함수를 반환합니다.
 *
 * ```
 * val cache = Cache.of(jcache)
 * val cachedLoader = { key: K -> ... }.cache(cache)
 * val result = cachedLoader(key)
 * ```
 *
 * @param cache 캐시 객체
 * @return 캐시된 값을 반환하는 함수
 */
fun <K, V> ((K) -> CompletionStage<V>).cache(cache: Cache<K, V>): (K) -> CompletionStage<V> {
    return cache.decorateCompletionStage(this)
}

/**
 * 비동기 실행 함수([func]) 실행 결과를 [Cache]에 저장하는 decorate 함수를 만듭니다.
 *
 * ```
 * val cache = Cache.of(jcache)
 * val cachedLoader = cache.decorateFunction1 { key: K -> ... }
 * val result = cachedLoader(key)
 * ```
 *
 * @param func 실행할 함수
 */
inline fun <K, V> Cache<K, V>.decorateFunction1(
    crossinline func: (K) -> V,
): (K) -> V = { key: K ->
    val validKey = key.requireNotNull("key")
    this.computeIfAbsent(validKey) { func(validKey) }
}

/**
 * 비동기 실행 함수([func]) 실행 결과를 [Cache]에 저장하는 decorate 함수를 만듭니다.
 *
 * ```
 * val cache = Cache.of(jcache)
 * val cachedLoader = { key: K -> ... }.cache(cache)
 * val result = cachedLoader(key)
 * ```
 *
 * @param cache 캐시 객체
 * @return 캐시된 값을 반환하는 함수
 */
fun <K, V> Cache<K, V>.decorateCompletionStage(
    func: (K) -> CompletionStage<V>,
): (K) -> CompletionStage<V> {
    val decorated = decorateCompletableFutureFunction { key: K ->
        func(key).toCompletableFuture()
    }
    return { key -> decorated(key) }
}

/**
 * 비동기 실행 함수([func]) 실행 결과를 [Cache]에 저장하는 decorate 함수를 만듭니다.
 *
 * ```
 * val cache = Cache.of(jcache)
 * val cachedLoader = cache.decorateCompletableFutureFunction { key: K -> ... }
 * val result = cachedLoader(key)
 * ```
 *
 * @param func 실행할 함수
 */
inline fun <K, V> Cache<K, V>.decorateCompletableFutureFunction(
    crossinline func: (K) -> CompletableFuture<V>,
): (K) -> CompletableFuture<V> = { key: K ->

    val validKey = key.requireNotNull("key")
    val promise = CompletableFuture<V>()
    val cachedValue = Optional.ofNullable(computeIfAbsent(validKey) { null })

    cachedValue.ifPresentOrElse({ promise.complete(it) }) {
        func(validKey)
            .whenComplete { result, error ->
                if (error != null) {
                    promise.completeExceptionally(error.cause ?: error)
                } else {
                    if (result != null) {
                        this.computeIfAbsent(validKey) { result }
                    }
                    promise.complete(result)
                }
            }
    }

    promise
}
