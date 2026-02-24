package io.bluetape4k.ignite.memorizer

import io.bluetape4k.cache.memorizer.AsyncMemorizer
import io.bluetape4k.concurrent.completableFutureOf
import io.bluetape4k.concurrent.flatMap
import io.bluetape4k.concurrent.map
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.apache.ignite.IgniteCache
import org.apache.ignite.lang.IgniteFuture
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.ConcurrentHashMap

/**
 * [IgniteFuture]를 [CompletableFuture]로 변환합니다.
 *
 * [IgniteFuture]는 [java.util.concurrent.Future]를 구현하지 않으므로
 * `listen()` 콜백을 사용하여 변환합니다.
 */
@Suppress("UNCHECKED_CAST")
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
 * 비동기 [evaluator]의 결과를 Apache Ignite 2.x [IgniteCache]에 저장하는 [AsyncMemorizer] 생성 확장 함수입니다.
 *
 * ```kotlin
 * val cache: IgniteCache<Int, Int> = ignite.getOrCreateCache("async-square")
 * val memorizer = cache.asyncMemorizer { key ->
 *     CompletableFuture.supplyAsync { key * key }
 * }
 * val result = memorizer(4).get()  // 16
 * ```
 *
 * @param K 입력 파라미터 타입
 * @param V 결과 타입
 * @param evaluator 비동기 결과를 반환하는 함수
 * @return [AsyncIgniteMemorizer] 인스턴스
 */
fun <K: Any, V: Any> IgniteCache<K, V>.asyncMemorizer(
    evaluator: (K) -> CompletionStage<V>,
): AsyncIgniteMemorizer<K, V> = AsyncIgniteMemorizer(this, evaluator)

/**
 * 비동기 함수를 [AsyncIgniteMemorizer]로 감싸는 확장 함수입니다.
 *
 * @receiver 비동기 결과를 반환하는 함수
 * @param cache 결과를 저장할 Ignite [IgniteCache]
 * @return [AsyncIgniteMemorizer] 인스턴스
 */
fun <K: Any, V: Any> ((K) -> CompletionStage<V>).asyncMemorizer(
    cache: IgniteCache<K, V>,
): AsyncIgniteMemorizer<K, V> = AsyncIgniteMemorizer(cache, this)

/**
 * 비동기 [evaluator] 결과를 Apache Ignite 2.x [IgniteCache]에 저장하는 [AsyncMemorizer] 구현체입니다.
 *
 * `inFlight` 맵으로 동일 키에 대한 중복 평가를 방지합니다.
 * `IgniteCache.getAsync()` / `putAsync()` 비동기 API를 활용하여 스레드를 블로킹하지 않습니다.
 *
 * ```kotlin
 * val cache: IgniteCache<Int, Int> = ignite.getOrCreateCache("async-square")
 * val memorizer = AsyncIgniteMemorizer(cache) { key ->
 *     CompletableFuture.supplyAsync { key * key }
 * }
 * val result1 = memorizer(4).get()  // 16 (새로 계산)
 * val result2 = memorizer(4).get()  // 16 (캐시에서 조회)
 * ```
 *
 * @property cache 결과를 저장할 Apache Ignite 2.x [IgniteCache]
 * @property evaluator 비동기 결과를 반환하는 함수
 */
class AsyncIgniteMemorizer<K: Any, V: Any>(
    val cache: IgniteCache<K, V>,
    val evaluator: (K) -> CompletionStage<V>,
): AsyncMemorizer<K, V> {

    companion object: KLogging()

    private val inFlight = ConcurrentHashMap<K, CompletableFuture<V>>()

    @Suppress("UNCHECKED_CAST")
    override fun invoke(key: K): CompletableFuture<V> {
        return inFlight.computeIfAbsent(key) {
            val promise = cache.getAsync(key)
                .toCompletableFuture()
                .flatMap { cached ->
                    if (cached != null) {
                        log.debug { "캐시 히트. key=$key" }
                        completableFutureOf(cached as V)
                    } else {
                        log.debug { "캐시 미스 - 새 값 계산. key=$key" }
                        evaluator(key)
                            .toCompletableFuture()
                            .flatMap { value ->
                                (cache.putAsync(key, value) as IgniteFuture<Any?>)
                                    .toCompletableFuture()
                                    .map { value }
                            }
                    }
                }

            promise.whenComplete { _, _ -> inFlight.remove(key, promise) }
            promise
        }
    }

    /**
     * 캐시를 모두 비웁니다.
     */
    override fun clear() {
        log.debug { "AsyncIgniteMemorizer 캐시를 초기화합니다. cache=${cache.name}" }
        cache.clear()
    }
}
