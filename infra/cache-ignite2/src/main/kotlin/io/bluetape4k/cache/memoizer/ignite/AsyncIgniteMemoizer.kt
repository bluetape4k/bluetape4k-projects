package io.bluetape4k.cache.memoizer.ignite

import io.bluetape4k.cache.memoizer.AsyncMemoizer
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.apache.ignite.client.ClientCache
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * [ClientCache]를 사용하는 비동기 메모이저 확장 함수입니다.
 *
 * ```kotlin
 * val cache: ClientCache<Int, Int> = igniteClient.getOrCreateCache("squares")
 * val memoizer = cache.asyncMemoizer { key -> key * key }
 * val result = memoizer(5).get()  // 25
 * ```
 *
 * @param evaluator 수행할 함수
 * @return [AsyncIgniteMemoizer] 인스턴스
 */
fun <K: Any, V: Any> ClientCache<K, V>.asyncMemoizer(evaluator: (K) -> V): AsyncIgniteMemoizer<K, V> =
    AsyncIgniteMemoizer(this, evaluator)

/**
 * [ClientCache]를 사용하는 비동기 메모이저 확장 함수입니다.
 *
 * ```kotlin
 * val memoizer = { key: Int -> key * key }.asyncMemoizer(cache)
 * val result = memoizer(5).get()
 * ```
 *
 * @receiver 실행할 함수
 * @param cache Ignite [ClientCache] 인스턴스
 * @return [AsyncIgniteMemoizer] 인스턴스
 */
fun <K: Any, V: Any> ((K) -> V).asyncMemoizer(cache: ClientCache<K, V>): AsyncIgniteMemoizer<K, V> =
    AsyncIgniteMemoizer(cache, this)

/**
 * [evaluator] 결과를 Ignite [ClientCache]에 저장하는 비동기 메모이저입니다.
 * `ClientCache`는 네이티브 비동기 API를 제공하지 않으므로 [CompletableFuture.supplyAsync]로 래핑합니다.
 * 같은 JVM에서 동일 키가 동시에 요청되면 in-flight 연산을 공유하여 중복 실행을 줄입니다.
 *
 * ```kotlin
 * val cache: ClientCache<Int, Int> = igniteClient.getOrCreateCache("squares")
 * val memoizer = AsyncIgniteMemoizer(cache) { key -> key * key }
 * val result = memoizer(5).get()  // 25
 * ```
 *
 * @property cache     Ignite [ClientCache] 인스턴스
 * @property evaluator 실행할 함수
 */
class AsyncIgniteMemoizer<K: Any, V: Any>(
    val cache: ClientCache<K, V>,
    val evaluator: (K) -> V,
): AsyncMemoizer<K, V> {

    companion object: KLogging()

    private val inFlight = ConcurrentHashMap<K, CompletableFuture<V>>()

    override fun invoke(key: K): CompletableFuture<V> {
        return inFlight.computeIfAbsent(key) {
            val promise = CompletableFuture.supplyAsync {
                val cached = cache.get(key)
                if (cached != null) {
                    cached
                } else {
                    val value = evaluator(key)
                    val isNew = cache.putIfAbsent(key, value)
                    if (isNew) value else (cache.get(key) ?: value)
                }
            }

            promise.whenComplete { _, _ -> inFlight.remove(key, promise) }
            promise
        }
    }

    override fun clear() {
        log.debug { "모든 메모이제이션 값 삭제: cache=${cache.name}" }
        inFlight.clear()
        cache.clear()
    }
}
