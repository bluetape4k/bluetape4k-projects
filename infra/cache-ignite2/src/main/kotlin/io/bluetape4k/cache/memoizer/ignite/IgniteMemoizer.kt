package io.bluetape4k.cache.memoizer.ignite

import io.bluetape4k.cache.memoizer.Memoizer
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.apache.ignite.client.ClientCache
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * [ClientCache]를 사용하는 동기 메모이저 확장 함수입니다.
 *
 * ```kotlin
 * val cache: ClientCache<Int, Int> = igniteClient.getOrCreateCache("squares")
 * val memoizer = cache.memoizer { key -> key * key }
 * val result1 = memoizer(5)   // 계산 후 Ignite에 저장
 * val result2 = memoizer(5)   // Ignite에서 반환
 * ```
 *
 * @param K  key type
 * @param V  value type
 * @receiver Ignite [ClientCache] 인스턴스
 * @param evaluator 실행할 함수
 * @return [IgniteMemoizer] instance
 */
fun <K: Any, V: Any> ClientCache<K, V>.memoizer(evaluator: (K) -> V): IgniteMemoizer<K, V> =
    IgniteMemoizer(this, evaluator)

/**
 * [ClientCache]를 사용하는 동기 메모이저 확장 함수입니다.
 *
 * ```kotlin
 * val memoizer = { key: Int -> key * key }.memoizer(cache)
 * val result = memoizer(5)
 * ```
 *
 * @param K key type
 * @param V value type
 * @receiver 실행할 함수
 * @param cache Ignite [ClientCache] 인스턴스
 * @return [IgniteMemoizer] instance
 */
fun <K: Any, V: Any> ((K) -> V).memoizer(cache: ClientCache<K, V>): IgniteMemoizer<K, V> =
    IgniteMemoizer(cache, this)

/**
 * [evaluator]가 실행한 결과를 [cache]에 저장하고, 재 실행 시에 빠르게 응답할 수 있도록 합니다.
 * 같은 JVM에서 동일 키가 동시에 요청되면 in-flight 연산을 공유하여 [evaluator]의 중복 실행을 줄입니다.
 *
 * ```kotlin
 * val cache: ClientCache<Int, Int> = igniteClient.getOrCreateCache("squares")
 * val memoizer = IgniteMemoizer(cache) { key -> key * key }
 * val result1 = memoizer(5)  // 25 — 계산 후 Ignite에 저장
 * val result2 = memoizer(5)  // 25 — Ignite에서 반환
 * ```
 *
 * @property cache     Ignite [ClientCache] 인스턴스
 * @property evaluator 실행할 함수
 */
class IgniteMemoizer<K: Any, V: Any>(
    val cache: ClientCache<K, V>,
    val evaluator: (K) -> V,
): Memoizer<K, V> {

    companion object: KLogging()

    private val inFlight = ConcurrentHashMap<K, CompletableFuture<V>>()

    override fun invoke(key: K): V {
        inFlight[key]?.let { return it.join() }

        val promise = CompletableFuture<V>()
        val existing = inFlight.putIfAbsent(key, promise)
        if (existing != null) return existing.join()

        try {
            val cached = cache.get(key)
            if (cached != null) {
                promise.complete(cached)
                return cached
            }

            val evaluated = evaluator(key)
            val isNew = cache.putIfAbsent(key, evaluated)
            val winner = if (isNew) evaluated else (cache.get(key) ?: evaluated)
            promise.complete(winner)
            return winner
        } catch (e: Throwable) {
            promise.completeExceptionally(e)
            throw e
        } finally {
            inFlight.remove(key, promise)
        }
    }

    override fun clear() {
        log.debug { "모든 메모이제이션 값 삭제: cache=${cache.name}" }
        cache.clear()
    }
}
